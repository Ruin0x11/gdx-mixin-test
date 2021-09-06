package xyz.ruin.gdxtest.launcher

import net.minecraft.launchwrapper.LogWrapper
import org.apache.logging.log4j.Level
import java.io.File
import java.lang.reflect.Method
import java.util.*
import kotlin.system.exitProcess


class Launch {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Launch().launch(args)
        }

        var blackboard: MutableMap<String, Any> = mutableMapOf()
        var home: File? = null
        var assetsDir: File? = null
        var classLoader: MyClassLoader = MyClassLoader(javaClass.classLoader)

        init {
            Thread.currentThread().contextClassLoader = classLoader
        }

        const val PRIMARY_TWEAKER: String = "xyz.ruin.gdxtest.launcher.MyTweaker"
    }

    private fun launch(args: Array<String>) {
        val argumentList: MutableList<String> = mutableListOf()

        val profileName = "profile"

        home = File.createTempFile("home", "xyz")
        assetsDir = File.createTempFile("assets", "xyz")

        val tweakClassNames: MutableList<String> = mutableListOf(PRIMARY_TWEAKER)
        val allTweakers: MutableList<ITweaker> = mutableListOf()
        val allTweakerNames: MutableSet<String> = HashSet()

        try {
            val tweakers: MutableList<ITweaker> = ArrayList(tweakClassNames.size + 1)
            // The list of tweak instances - may be useful for interoperability
            blackboard["Tweaks"] = tweakers
            // The primary tweaker (the first one specified on the command line) will actually
            // be responsible for providing the 'main' name and generally gets called first
            var primaryTweaker: ITweaker? = null
            // This loop will terminate, unless there is some sort of pathological tweaker
            // that reinserts itself with a new identity every pass
            // It is here to allow tweakers to "push" new tweak classes onto the 'stack' of
            // tweakers to evaluate allowing for cascaded discovery and injection of tweakers
            do {
                run {
                    val it: MutableIterator<String> = tweakClassNames.iterator()
                    while (it.hasNext()) {
                        val tweakName = it.next()
                        // Safety check - don't reprocess something we've already visited
                        if (allTweakerNames.contains(tweakName)) {
                            LogWrapper.log(
                                Level.WARN,
                                "Tweak class name %s has already been visited -- skipping",
                                tweakName
                            )
                            // remove the tweaker from the stack otherwise it will create an infinite loop
                            it.remove()
                            continue
                        } else {
                            allTweakerNames.add(tweakName)
                        }
                        LogWrapper.log(Level.INFO, "Loading tweak class name %s", tweakName)

                        // Ensure we allow the tweak class to load with the parent classloader
                        classLoader.addClassLoaderExclusion(
                            tweakName.substring(
                                0,
                                tweakName.lastIndexOf('.')
                            )
                        )
                        val tweaker =
                            Class.forName(tweakName, true, classLoader)
                                .newInstance() as ITweaker
                        tweakers.add(tweaker)

                        // Remove the tweaker from the list of tweaker names we've processed this pass
                        it.remove()
                        // If we haven't visited a tweaker yet, the first will become the 'primary' tweaker
                        if (primaryTweaker == null) {
                            LogWrapper.log(Level.INFO, "Using primary tweak class name %s", tweakName)
                            primaryTweaker = tweaker
                        }
                    }
                }

                // Now, iterate all the tweakers we just instantiated
                val it = tweakers.iterator()
                while (it.hasNext()) {
                    val tweaker = it.next()
                    LogWrapper.log(Level.INFO, "Calling tweak class %s", tweaker.javaClass.name)
                    tweaker.acceptOptions(listOf(), home!!, assetsDir!!, profileName)
                    tweaker.injectIntoClassLoader(classLoader)
                    allTweakers.add(tweaker)
                    // again, remove from the list once we've processed it, so we don't get duplicates
                    it.remove()
                }
                // continue around the loop until there's no tweak classes
            } while (tweakClassNames.isNotEmpty())

            // Once we're done, we then ask all the tweakers for their arguments and add them all to the
            // master argument list
            for (tweaker in allTweakers) {
                argumentList.addAll(listOf(*tweaker.launchArguments))
            }

            val launchTarget: String = primaryTweaker!!.launchTarget
            val clazz = Class.forName(launchTarget, false, classLoader)
            val mainMethod: Method = clazz.getMethod("main", Array<String>::class.java)

            LogWrapper.info("Launching wrapped game {%s}", launchTarget)
            mainMethod.invoke(null, argumentList.toTypedArray())
        } catch (e: Exception) {
            LogWrapper.log(Level.ERROR, e, "Unable to launch");
            exitProcess(1);
        }
    }
}