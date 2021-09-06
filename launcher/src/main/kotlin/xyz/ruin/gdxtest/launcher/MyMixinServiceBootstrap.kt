package xyz.ruin.gdxtest.launcher

import org.spongepowered.asm.service.IMixinServiceBootstrap
import org.spongepowered.asm.service.ServiceInitialisationException


/**
 * Bootstrap for LaunchWrapper service
 */
class MyMixinServiceBootstrap : IMixinServiceBootstrap {
    override fun getName(): String {
        return "MyLaunch"
    }

    override fun getServiceClassName(): String {
        return "xyz.ruin.gdxtest.launcher.MixinServiceMyLaunch"
    }

    override fun bootstrap() {
        try {
            Launch.classLoader.hashCode()
        } catch (th: Throwable) {
            throw ServiceInitialisationException(this.name + " is not available")
        }

        // Essential ones
        Launch.classLoader.addClassLoaderExclusion(SERVICE_PACKAGE)
        Launch.classLoader.addClassLoaderExclusion(LAUNCH_PACKAGE)
        Launch.classLoader.addClassLoaderExclusion(LOGGING_PACKAGE)

        // Important ones
        Launch.classLoader.addClassLoaderExclusion(ASM_PACKAGE)
        Launch.classLoader.addClassLoaderExclusion(LEGACY_ASM_PACKAGE)
        Launch.classLoader.addClassLoaderExclusion(MIXIN_PACKAGE)
        Launch.classLoader.addClassLoaderExclusion(MIXIN_UTIL_PACKAGE)

        Launch.classLoader.addClassLoaderExclusion("xyz.ruin.gdxtest.launcher.")
    }

    companion object {
        private const val SERVICE_PACKAGE = "org.spongepowered.asm.service."
        private const val LAUNCH_PACKAGE = "org.spongepowered.asm.launch."
        private const val LOGGING_PACKAGE = "org.spongepowered.asm.logging."
        private const val MIXIN_UTIL_PACKAGE = "org.spongepowered.asm.util."
        private const val LEGACY_ASM_PACKAGE = "org.spongepowered.asm.lib."
        private const val ASM_PACKAGE = "org.objectweb.asm."
        private const val MIXIN_PACKAGE = "org.spongepowered.asm.mixin."
    }
}