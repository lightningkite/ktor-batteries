package com.lightningkite.lightningserver.logging

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy
import com.lightningkite.lightningserver.Server
import com.lightningkite.lightningserver.ServerRunner
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object LoggingAppender: Server.ResourceRequirement<Appender<ILoggingEvent>, String> {
    override val type: String
        get() = "logging"
    override val serializer: KSerializer<String>
        get() = String.serializer()
    override val name: String
        get() = "logging"

    override fun ServerRunner.fromExplicit(setting: String): Appender<ILoggingEvent> = when {
        setting == "console" -> ConsoleAppender()
        setting.startsWith("file://") -> RollingFileAppender<ILoggingEvent>().apply rolling@{
                    context = LoggerFactory.getILoggerFactory() as LoggerContext
                    name = Logger.ROOT_LOGGER_NAME
                    encoder = PatternLayoutEncoder().apply {
                        context = LoggerFactory.getILoggerFactory() as LoggerContext
                        pattern = "%-12date{YYYY-MM-dd HH:mm:ss.SSS} %-5level %logger - %msg%n"
                        start()
                    }
                    isAppend = true
                    rollingPolicy = TimeBasedRollingPolicy<ILoggingEvent>().apply {
                        context = LoggerFactory.getILoggerFactory() as LoggerContext
                        setParent(this@rolling);
                        fileNamePattern = setting.substringAfter("file://");
                        maxHistory = 7;
                        start();
                    }
                    start()
                }
        else -> throw IllegalStateException("Unknown scheme $setting - supported are 'console' and 'file://somefile'")
    }

}

fun ServerRunner.loggingSetup() {
    val settings = LoggingSettings.setting()
    val logCtx: LoggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
    logCtx.getLogger(Logger.ROOT_LOGGER_NAME).detachAppender("console")
    logCtx.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(LoggingAppender())
    logCtx.getLogger(Logger.ROOT_LOGGER_NAME).level = settings.default.level
    for (sub in (settings.logger ?: mapOf())) {
        logCtx.getLogger(sub.key).level = sub.value.level
    }
}