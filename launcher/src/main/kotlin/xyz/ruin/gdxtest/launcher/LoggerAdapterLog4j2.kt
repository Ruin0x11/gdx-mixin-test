package xyz.ruin.gdxtest.launcher

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.spongepowered.asm.logging.Level
import org.spongepowered.asm.logging.LoggerAdapterAbstract

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


class LoggerAdapterLog4j2(name: String?) : LoggerAdapterAbstract(name) {
    private val logger: Logger
    override fun getType(): String {
        return "Log4j2 (via LaunchWrapper)"
    }

    override fun catching(level: Level, t: Throwable) {
        logger.catching(LEVELS[level.ordinal], t)
    }

    override fun catching(t: Throwable) {
        logger.catching(t)
    }

    override fun debug(message: String, vararg params: Any) {
        logger.debug(message, *params)
    }

    override fun debug(message: String, t: Throwable) {
        logger.debug(message, t)
    }

    override fun error(message: String, vararg params: Any) {
        logger.error(message, *params)
    }

    override fun error(message: String, t: Throwable) {
        logger.error(message, t)
    }

    override fun fatal(message: String, vararg params: Any) {
        logger.fatal(message, *params)
    }

    override fun fatal(message: String, t: Throwable) {
        logger.fatal(message, t)
    }

    override fun info(message: String, vararg params: Any) {
        logger.info(message, *params)
    }

    override fun info(message: String, t: Throwable) {
        logger.info(message, t)
    }

    override fun log(level: Level, message: String, vararg params: Any) {
        logger.log(LEVELS[level.ordinal], message, *params)
    }

    override fun log(level: Level, message: String, t: Throwable) {
        logger.log(LEVELS[level.ordinal], message, t)
    }

    override fun <T : Throwable?> throwing(t: T): T {
        return logger.throwing(t)
    }

    override fun trace(message: String, vararg params: Any) {
        logger.trace(message, *params)
    }

    override fun trace(message: String, t: Throwable) {
        logger.trace(message, t)
    }

    override fun warn(message: String, vararg params: Any) {
        logger.warn(message, *params)
    }

    override fun warn(message: String, t: Throwable) {
        logger.warn(message, t)
    }

    companion object {
        private val LEVELS = arrayOf( /* FATAL = */
            org.apache.logging.log4j.Level.FATAL,  /* ERROR = */
            org.apache.logging.log4j.Level.ERROR,  /* WARN =  */
            org.apache.logging.log4j.Level.WARN,  /* INFO =  */
            org.apache.logging.log4j.Level.INFO,  /* DEBUG = */
            org.apache.logging.log4j.Level.DEBUG,  /* TRACE = */
            org.apache.logging.log4j.Level.TRACE
        )
    }

    init {
        logger = LogManager.getLogger(name)
    }
}
