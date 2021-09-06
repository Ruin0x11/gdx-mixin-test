package xyz.ruin.gdxtest.launcher

import org.spongepowered.asm.service.IClassTracker

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
 * Utility class for reflecting into [LaunchClassLoader]. We **do not
 * write** anything of the classloader fields, but we need to be able to read
 * them to perform some validation tasks, and insert entries for mixin "classes"
 * into the invalid classes set.
 */
internal class MyLaunchClassTracker(
    /**
     * ClassLoader for this util
     */
    val classLoader: MyClassLoader
) : IClassTracker {
    /**
     * Get the classloader
     */

    /**
     * Get whether a class name exists in the cache (indicating it was loaded
     * via the inner loader
     *
     * @param name class name
     * @return true if the class name exists in the cache
     */
    override fun isClassLoaded(name: String): Boolean {
        return classLoader.cachedClasses.containsKey(name)
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getClassRestrictions(
     *      java.lang.String)
     */
    override fun getClassRestrictions(className: String): String {
        var restrictions = ""
        if (isClassClassLoaderExcluded(className, null)) {
            restrictions = "PACKAGE_CLASSLOADER_EXCLUSION"
        }
        if (isClassTransformerExcluded(className, null)) {
            restrictions = (if (restrictions.length > 0) "$restrictions," else "") + "PACKAGE_TRANSFORMER_EXCLUSION"
        }
        return restrictions
    }

    /**
     * Get whether the specified name or transformedName exist in either of the
     * exclusion lists
     *
     * @param name class name
     * @param transformedName transformed class name
     * @return true if either exclusion list contains either of the names
     */
    fun isClassExcluded(name: String, transformedName: String?): Boolean {
        return isClassClassLoaderExcluded(name, transformedName) || isClassTransformerExcluded(name, transformedName)
    }

    /**
     * Get whether the specified name or transformedName exist in the
     * classloader exclusion list
     *
     * @param name class name
     * @param transformedName transformed class name
     * @return true if the classloader exclusion list contains either of the
     * names
     */
    fun isClassClassLoaderExcluded(name: String, transformedName: String?): Boolean {
        for (exception in classLoader.classLoaderExclusions) {
            if (transformedName != null && transformedName.startsWith(exception) || name.startsWith(exception)) {
                return true
            }
        }
        return false
    }

    /**
     * Get whether the specified name or transformedName exist in the
     * transformer exclusion list
     *
     * @param name class name
     * @param transformedName transformed class name
     * @return true if the transformer exclusion list contains either of the
     * names
     */
    fun isClassTransformerExcluded(name: String, transformedName: String?): Boolean {
        for (exception in classLoader.transformerExclusions) {
            if (transformedName != null && transformedName.startsWith(exception) || name.startsWith(exception)) {
                return true
            }
        }
        return false
    }

    /**
     * Stuff a class name directly into the invalidClasses set, this prevents
     * the loader from classloading the named class. This is used by the mixin
     * processor to prevent classloading of mixin classes
     *
     * @param name class name
     */
    override fun registerInvalidClass(name: String) {
        classLoader.addInvalidClass(name)
    }
}