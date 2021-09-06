package xyz.ruin.gdxtest.launcher

import net.minecraft.launchwrapper.IClassNameTransformer
import net.minecraft.launchwrapper.IClassTransformer
import net.minecraft.launchwrapper.LogWrapper
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.*


class MyClassLoader(private val inner: ClassLoader) : ClassLoader() {
    val transformers: MutableList<IClassTransformer> = mutableListOf()
    var renameTransformer: IClassNameTransformer? = null
    val invalidClasses: MutableSet<String> = mutableSetOf()
    val classLoaderExclusions: MutableList<String> = mutableListOf()
    val transformerExclusions: MutableList<String> = mutableListOf()
    val cachedClasses: MutableMap<String, Class<*>> = mutableMapOf()

    init {
        // classloader exclusions
        addClassLoaderExclusion("java.");
        addClassLoaderExclusion("sun.");
        addClassLoaderExclusion("org.lwjgl.");
        addClassLoaderExclusion("org.apache.logging.");
        addClassLoaderExclusion("xyz.ruin.gdxtest.launcher.");

        // transformer exclusions
        addTransformerExclusion("javax.");
        addTransformerExclusion("argo.");
        addTransformerExclusion("org.objectweb.asm.");
        addTransformerExclusion("com.google.common.");
        addTransformerExclusion("org.bouncycastle.");
        addTransformerExclusion("xyz.ruin.gdxtest.launcher.");
    }

    override fun findClass(className: String): Class<*> {
        return loadClass(className)
    }

    override fun loadClass(className: String): Class<*> {
        if (invalidClasses.contains(className)) {
            throw ClassNotFoundException(className)
        }

        if (classLoaderExclusions.any { className.startsWith(it) }) {
            return inner.loadClass(className)
        }

        if (cachedClasses.containsKey(className)) {
            return cachedClasses[className]!!
        }

        if (transformerExclusions.any { className.startsWith(it) }) {
            return try {
                val findClass = inner::class.java.getMethod("findClass", String::class.java)
                findClass.isAccessible = true
                val clazz = findClass.invoke(inner, className) as Class<*>
                cachedClasses[className] = clazz
                clazz
            } catch (e: ClassNotFoundException) {
                invalidClasses.add(className)
                throw e
            }
        }

        return doLoadClass(className)
    }

    fun getClassBytes(className: String): ByteArray? {
        val internalName: String = className.replace(".", "/") + ".class"
        val inputStream: InputStream = getResourceAsStream(internalName) ?: throw ClassNotFoundException(className)

        try {
            return inputStream.readAllBytes()
        } catch (ex: IOException) {
            return null
        }
    }

    @Throws(ClassNotFoundException::class)
    private fun doLoadClass(className: String): Class<*> {
        val bytes: ByteArray = getClassBytes(className) ?: throw ClassNotFoundException(className)

        return try {
            val untransformedName = untransformName(className)
            val transformedName = transformName(className)
            if (cachedClasses.containsKey(transformedName)) {
                return cachedClasses[transformedName]!!
            }

            val transformedClassBytes: ByteArray = runTransformers(untransformedName, transformedName, bytes)

            val cls = defineClass(transformedName, transformedClassBytes, 0, transformedClassBytes.size)

            // Additional check for defining the package, if not defined yet.
            if (cls.getPackage() == null) {
                val packageSeparator = transformedName.lastIndexOf('.')
                if (packageSeparator != -1) {
                    val packageName = transformedName.substring(0, packageSeparator)
                    definePackage(packageName, null, null, null, null, null, null, null)
                }
            }

            cachedClasses[transformedName] = cls
            cls
        } catch (e: Throwable) {
            invalidClasses.add(className)
            if (false /* DEBUG */) {
                LogWrapper.log(Level.TRACE, e, "Exception encountered attempting classloading of %s", className)
                LogManager.getLogger("LaunchWrapper")
                    .log(Level.ERROR, "Exception encountered attempting classloading of %s", e)
            }
            throw ClassNotFoundException(className, e)
        }
    }

    private fun runTransformers(name: String, transformedName: String, basicClass: ByteArray): ByteArray {
        var basicClass: ByteArray = basicClass
        if (false /* DEBUG_FINER */) {
            LogWrapper.finest(
                "Beginning transform of {%s (%s)} Start Length: %d", name, transformedName, basicClass.size
                    ?: 0
            )
            for (transformer in transformers) {
                val transName = transformer.javaClass.name
                LogWrapper.finest(
                    "Before Transformer {%s (%s)} %s: %d", name, transformedName, transName, basicClass.size
                        ?: 0
                )
                basicClass = transformer.transform(name, transformedName, basicClass)
                LogWrapper.finest(
                    "After  Transformer {%s (%s)} %s: %d", name, transformedName, transName, basicClass.size
                        ?: 0
                )
            }
            LogWrapper.finest(
                "Ending transform of {%s (%s)} Start Length: %d", name, transformedName, basicClass.size
                    ?: 0
            )
        } else {
            for (transformer in transformers) {
                basicClass = transformer.transform(name, transformedName, basicClass)
            }
        }
        return basicClass
    }
    fun addClassLoaderExclusion(className: String) {
        classLoaderExclusions.add(className)
    }

    private fun addTransformerExclusion(s: String) {
        transformerExclusions.add(s)
    }


    fun addInvalidClass(name: String) {
        invalidClasses.add(name)
    }

    private fun untransformName(name: String): String {
        return if (renameTransformer != null) {
            renameTransformer!!.unmapClassName(name)
        } else name
    }

    private fun transformName(name: String): String {
        return if (renameTransformer != null) {
            renameTransformer!!.remapClassName(name)
        } else name
    }

    fun registerTransformer(transformerProxyClass: String) {
        try {
            val transformer = loadClass(transformerProxyClass).newInstance() as IClassTransformer
            transformers.add(transformer)
            if (transformer is IClassNameTransformer && renameTransformer == null) {
                renameTransformer = transformer as IClassNameTransformer
            }
        } catch (e: Exception) {
            LogWrapper.log(
                Level.ERROR,
                e,
                "A critical problem occurred registering the ASM transformer class %s",
                transformerProxyClass
            )
        }
    }

    override fun getResource(name: String?): URL? = inner.getResource(name)

    override fun getResourceAsStream(name: String?): InputStream? = inner.getResourceAsStream(name)

    @Throws(IOException::class)
    override fun getResources(name: String?): Enumeration<URL?> = inner.getResources(name)
}