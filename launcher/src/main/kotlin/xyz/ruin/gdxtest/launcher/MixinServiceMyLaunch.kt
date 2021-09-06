package xyz.ruin.gdxtest.launcher

import com.google.common.collect.ImmutableList
import com.google.common.collect.Sets
import com.google.common.io.ByteStreams
import com.google.common.io.Closeables
import net.minecraft.launchwrapper.IClassNameTransformer
import org.apache.logging.log4j.LogManager
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.spongepowered.asm.launch.GlobalProperties
import org.spongepowered.asm.launch.platform.MainAttributes
import org.spongepowered.asm.launch.platform.container.ContainerHandleURI
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual
import org.spongepowered.asm.launch.platform.container.IContainerHandle
import org.spongepowered.asm.logging.ILogger
import org.spongepowered.asm.mixin.MixinEnvironment
import org.spongepowered.asm.mixin.MixinEnvironment.CompatibilityLevel
import org.spongepowered.asm.mixin.throwables.MixinException
import org.spongepowered.asm.service.*
import org.spongepowered.asm.service.mojang.LoggerAdapterLog4j2
import org.spongepowered.asm.transformers.MixinClassReader
import org.spongepowered.asm.util.Constants
import org.spongepowered.asm.util.perf.Profiler
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.util.*

/*
* This file is part of Mixin, licensed under the MIT License (MIT).
*
* Copyright (c) SpongePowered <https://www.spongepowered.org>
* Copyright (c) contributors
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in
* all copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
* THE SOFTWARE.
*/

/**
 * Mixin service for launchwrapper
 */
class MixinServiceMyLaunch : MixinServiceAbstract(), IClassProvider, IClassBytecodeProvider,
    ITransformerProvider {
    /**
     * Utility for reflecting into Launch ClassLoader
     */
    private val classLoaderUtil: MyLaunchClassTracker = MyLaunchClassTracker(Launch.classLoader)

    /**
     * Local transformer chain, this consists of all transformers present at the
     * init phase with the exclusion of the mixin transformer itself and known
     * re-entrant transformers. Detected re-entrant transformers will be
     * subsequently removed.
     */
    private var delegatedTransformers: MutableList<ILegacyClassTransformer>? = null

    /**
     * Class name transformer (if present)
     */
    private var nameTransformer: IClassNameTransformer? = null
    override fun getName(): String {
        return "LaunchWrapper"
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#isValid()
     */
    override fun isValid(): Boolean {
        try {
            // Detect launchwrapper
            Launch.classLoader.hashCode()
        } catch (ex: Throwable) {
            return false
        }
        return true
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#prepare()
     */
    override fun prepare() {
        // Only needed in dev, in production this would be handled by the tweaker
        Launch.classLoader.addClassLoaderExclusion(LAUNCH_PACKAGE)
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getInitialPhase()
     */
    override fun getInitialPhase(): MixinEnvironment.Phase {
        val command = System.getProperty("sun.java.command")
        if (command != null && command.contains("GradleStart")) {
            System.setProperty("mixin.env.remapRefMap", "true")
        }
        return if (findInStackTrace("xyz.ruin.gdxtest.launcher.Launch", "launch") > 81) {
            MixinEnvironment.Phase.DEFAULT
        } else MixinEnvironment.Phase.PREINIT
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService
     *      #getMaxCompatibilityLevel()
     */
    override fun getMaxCompatibilityLevel(): CompatibilityLevel {
        return CompatibilityLevel.JAVA_8
    }

    override fun createLogger(name: String): ILogger {
        return LoggerAdapterLog4j2(name)
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#init()
     */
    override fun init() {
        if (findInStackTrace("xyz.ruin.gdxtest.launcher.Launch", "launch") < 4) {
            logger.error("MixinBootstrap.doInit() called during a tweak constructor!")
        }
        val tweakClasses = GlobalProperties.get<MutableList<String>>(BLACKBOARD_KEY_TWEAKCLASSES)
        tweakClasses?.add(STATE_TWEAKER)
        super.init()
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getPlatformAgents()
     */
    override fun getPlatformAgents(): Collection<String> {
        return ImmutableList.of(
            "org.spongepowered.asm.launch.platform.MixinPlatformAgentFMLLegacy",
            "org.spongepowered.asm.launch.platform.MixinPlatformAgentLiteLoaderLegacy"
        )
    }

    override fun getPrimaryContainer(): IContainerHandle {
        var uri: URI? = null
        try {
            uri = this.javaClass.protectionDomain.codeSource.location.toURI()
            if (uri != null) {
                return ContainerHandleURI(uri)
            }
        } catch (ex: URISyntaxException) {
            ex.printStackTrace()
        }
        return ContainerHandleVirtual(this.name)
    }

    override fun getMixinContainers(): Collection<IContainerHandle> {
        val list = ImmutableList.builder<IContainerHandle>()
        getContainersFromClassPath(list)
        getContainersFromAgents(list)
        return list.build()
    }

    private fun getContainersFromClassPath(list: ImmutableList.Builder<IContainerHandle>) {
        // We know this is deprecated, it works for LW though, so access directly
        val sources = this.classPath
        if (sources != null) {
            for (url in sources) {
                try {
                    val uri = url.toURI()
                    logger.debug("Scanning {} for mixin tweaker", uri)
                    if ("file" != uri.scheme || !File(uri).exists()) {
                        continue
                    }
                    val attributes = MainAttributes.of(uri)
                    val tweaker = attributes[Constants.ManifestAttributes.TWEAKER]
                    if (MIXIN_TWEAKER_CLASS == tweaker) {
                        list.add(ContainerHandleURI(uri))
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getClassProvider()
     */
    override fun getClassProvider(): IClassProvider {
        return this
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getBytecodeProvider()
     */
    override fun getBytecodeProvider(): IClassBytecodeProvider {
        return this
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getTransformerProvider()
     */
    override fun getTransformerProvider(): ITransformerProvider {
        return this
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getClassTracker()
     */
    override fun getClassTracker(): IClassTracker {
        return classLoaderUtil
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getAuditTrail()
     */
    override fun getAuditTrail(): IMixinAuditTrail? {
        return null
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IClassProvider#findClass(
     *      java.lang.String)
     */
    @Throws(ClassNotFoundException::class)
    override fun findClass(name: String): Class<*> {
        val findClass = Launch.classLoader::class.java.getDeclaredMethod("findClass", String::class.java)
        return findClass.invoke(Launch.classLoader, name) as Class<*>
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IClassProvider#findClass(
     *      java.lang.String, boolean)
     */
    @Throws(ClassNotFoundException::class)
    override fun findClass(name: String, initialize: Boolean): Class<*> {
        return Class.forName(name, initialize, Launch.classLoader)
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IClassProvider#findAgentClass(
     *      java.lang.String, boolean)
     */
    @Throws(ClassNotFoundException::class)
    override fun findAgentClass(name: String, initialize: Boolean): Class<*> {
        return Class.forName(name, initialize, Launch::class.java.classLoader)
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#beginPhase()
     */
    override fun beginPhase() {
        Launch.classLoader.registerTransformer(TRANSFORMER_PROXY_CLASS)
        delegatedTransformers = null
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#checkEnv(
     *      java.lang.Object)
     */
    override fun checkEnv(bootSource: Any) {
        if (bootSource.javaClass.classLoader !== Launch::class.java.classLoader) {
            throw MixinException("Attempted to init the mixin environment in the wrong classloader")
        }
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getResourceAsStream(
     *      java.lang.String)
     */
    override fun getResourceAsStream(name: String): InputStream? {
        return Launch.classLoader.getResourceAsStream(name)
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IClassProvider#getClassPath()
     */
    @Deprecated("")
    override fun getClassPath(): Array<URL> {
        return arrayOf()
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getTransformers()
     */
    override fun getTransformers(): Collection<ITransformer> {
        val transformers = Launch.classLoader.transformers
        val wrapped: MutableList<ITransformer> = ArrayList(transformers.size)
        for (transformer in transformers) {
            if (transformer is ITransformer) {
                wrapped.add(transformer as ITransformer)
            } else {
                //wrapped.add(LegacyTransformerHandle(transformer))
                throw Exception("asdf")
            }
            if (transformer is IClassNameTransformer) {
                logger.debug("Found name transformer: {}", transformer.javaClass.name)
                nameTransformer = transformer
            }
        }
        return wrapped
    }

    /**
     * Returns (and generates if necessary) the transformer delegation list for
     * this environment.
     *
     * @return current transformer delegation list (read-only)
     */
    override fun getDelegatedTransformers(): List<ITransformer> {
        return Collections.unmodifiableList<ITransformer>(delegatedLegacyTransformers)
    }

    private val delegatedLegacyTransformers: List<ILegacyClassTransformer>?
        private get() {
            if (delegatedTransformers == null) {
                buildTransformerDelegationList()
            }
            return delegatedTransformers
        }

    /**
     * Builds the transformer list to apply to loaded mixin bytecode. Since
     * generating this list requires inspecting each transformer by name (to
     * cope with the new wrapper functionality added by FML) we generate the
     * list just once per environment and cache the result.
     */
    private fun buildTransformerDelegationList() {
        logger.debug("Rebuilding transformer delegation list:")
        delegatedTransformers = ArrayList()
        for (transformer in this.transformers) {
            if (transformer !is ILegacyClassTransformer) {
                continue
            }
            val legacyTransformer = transformer
            val transformerName = legacyTransformer.name
            var include = true
            for (excludeClass in excludeTransformers) {
                if (transformerName.contains(excludeClass)) {
                    include = false
                    break
                }
            }
            if (include && !legacyTransformer.isDelegationExcluded) {
                logger.debug("  Adding:    {}", transformerName)
                delegatedTransformers!!.add(legacyTransformer)
            } else {
                logger.debug("  Excluding: {}", transformerName)
            }
        }
        logger.debug("Transformer delegation list created with {} entries", delegatedTransformers!!.size)
    }

    /**
     * Adds a transformer to the transformer exclusions list
     *
     * @param name Class transformer exclusion to add
     */
    override fun addTransformerExclusion(name: String) {
        excludeTransformers.add(name)

        // Force rebuild of the list
        delegatedTransformers = null
    }

    /**
     * Retrieve class bytes using available classloaders, does not transform the
     * class
     *
     * @param name class name
     * @param transformedName transformed class name
     * @return class bytes or null if not found
     * @throws IOException propagated
     */
    @Deprecated("Use {@link #getClassNode} instead")
    @Throws(IOException::class)
    fun getClassBytes(name: String, transformedName: String): ByteArray? {
        val classBytes = Launch.classLoader.getClassBytes(name)
        if (classBytes != null) {
            return classBytes
        }
        val appClassLoader: MyClassLoader = Launch.classLoader
        var classStream: InputStream? = null
        return try {
            val resourcePath = transformedName.replace('.', '/') + ".class"
            classStream = appClassLoader.getResourceAsStream(resourcePath)
            ByteStreams.toByteArray(classStream)
        } catch (ex: Exception) {
            null
        } finally {
            Closeables.closeQuietly(classStream)
        }
    }

    /**
     * Loads class bytecode from the classpath
     *
     * @param className Name of the class to load
     * @param runTransformers True to run the loaded bytecode through the
     * delegate transformer chain
     * @return Transformed class bytecode for the specified class
     * @throws ClassNotFoundException if the specified class could not be loaded
     * @throws IOException if an error occurs whilst reading the specified class
     */
    @Deprecated("")
    @Throws(ClassNotFoundException::class, IOException::class)
    fun getClassBytes(className: String, runTransformers: Boolean): ByteArray {
        val transformedName = className.replace('/', '.')
        val name = unmapClassName(transformedName)
        val profiler = Profiler.getProfiler("mixin")
        val loadTime = profiler.begin(Profiler.ROOT, "class.load")
        var classBytes = this.getClassBytes(name, transformedName)
        loadTime.end()
        if (runTransformers) {
            val transformTime = profiler.begin(Profiler.ROOT, "class.transform")
            classBytes = applyTransformers(name, transformedName, classBytes, profiler)
            transformTime.end()
        }
        if (classBytes == null) {
            throw ClassNotFoundException(String.format("The specified class '%s' was not found", transformedName))
        }
        return classBytes
    }

    /**
     * Since we obtain the class bytes with getClassBytes(), we need to apply
     * the transformers ourself
     *
     * @param name class name
     * @param transformedName transformed class name
     * @param basicClass input class bytes
     * @return class bytecode after processing by all registered transformers
     * except the excluded transformers
     */
    private fun applyTransformers(
        name: String,
        transformedName: String,
        basicClass: ByteArray?,
        profiler: Profiler
    ): ByteArray? {
        var basicClass = basicClass
        if (classLoaderUtil.isClassExcluded(name, transformedName)) {
            return basicClass
        }
        for (transformer in delegatedLegacyTransformers!!) {
            // Clear the re-entrance semaphore
            lock.clear()
            val pos = transformer.name.lastIndexOf('.')
            val simpleName = transformer.name.substring(pos + 1)
            val transformTime = profiler.begin(Profiler.FINE, simpleName.lowercase())
            transformTime.info = transformer.name
            basicClass = transformer.transformClassBytes(name, transformedName, basicClass)
            transformTime.end()
            if (lock.isSet) {
                // Also add it to the exclusion list so we can exclude it if the environment triggers a rebuild
                addTransformerExclusion(transformer.name)
                lock.clear()
                logger.info(
                    "A re-entrant transformer '{}' was detected and will no longer process meta class data",
                    transformer.name
                )
            }
        }
        return basicClass
    }

    private fun unmapClassName(className: String): String {
        if (nameTransformer == null) {
            findNameTransformer()
        }
        return if (nameTransformer != null) {
            nameTransformer!!.unmapClassName(className)
        } else className
    }

    private fun findNameTransformer() {
        val transformers = Launch.classLoader.transformers
        for (transformer in transformers) {
            if (transformer is IClassNameTransformer) {
                logger.debug("Found name transformer: {}", transformer.javaClass.name)
                nameTransformer = transformer
            }
        }
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getClassNode(
     *      java.lang.String)
     */
    @Throws(ClassNotFoundException::class, IOException::class)
    override fun getClassNode(className: String): ClassNode {
        return this.getClassNode(className, this.getClassBytes(className, true), ClassReader.EXPAND_FRAMES)
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IClassBytecodeProvider#getClassNode(
     *      java.lang.String, boolean)
     */
    @Throws(ClassNotFoundException::class, IOException::class)
    override fun getClassNode(className: String, runTransformers: Boolean): ClassNode {
        return this.getClassNode(className, this.getClassBytes(className, true), ClassReader.EXPAND_FRAMES)
    }

    /**
     * Gets an ASM Tree for the supplied class bytecode
     *
     * @param classBytes Class bytecode
     * @param flags ClassReader flags
     * @return ASM Tree view of the specified class
     */
    private fun getClassNode(className: String, classBytes: ByteArray, flags: Int): ClassNode {
        val classNode = ClassNode()
        val classReader: ClassReader = MixinClassReader(classBytes, className)
        classReader.accept(classNode, flags)
        return classNode
    }

    companion object {
        // Blackboard keys
        val BLACKBOARD_KEY_TWEAKCLASSES = GlobalProperties.Keys.of("TweakClasses")
        val BLACKBOARD_KEY_TWEAKS = GlobalProperties.Keys.of("Tweaks")
        private const val MIXIN_TWEAKER_CLASS = LAUNCH_PACKAGE + "MixinTweaker"

        // Consts
        private const val STATE_TWEAKER = MIXIN_PACKAGE + "EnvironmentStateTweaker"
        private const val TRANSFORMER_PROXY_CLASS = MIXIN_PACKAGE + "transformer.Proxy"

        /**
         * Known re-entrant transformers, other re-entrant transformers will
         * detected automatically
         */
        private val excludeTransformers: MutableSet<String> = Sets.newHashSet(
            "net.minecraftforge.fml.common.asm.transformers.EventSubscriptionTransformer",
            "cpw.mods.fml.common.asm.transformers.EventSubscriptionTransformer",
            "net.minecraftforge.fml.common.asm.transformers.TerminalTransformer",
            "cpw.mods.fml.common.asm.transformers.TerminalTransformer"
        )

        /**
         * Log4j2 logger
         */
        private val logger = LogManager.getLogger()
        private fun findInStackTrace(className: String, methodName: String): Int {
            val currentThread = Thread.currentThread()
            if ("main" != currentThread.name) {
                return 0
            }
            val stackTrace = currentThread.stackTrace
            for (s in stackTrace) {
                if (className == s.className && methodName == s.methodName) {
                    return s.lineNumber
                }
            }
            return 0
        }
    }
}