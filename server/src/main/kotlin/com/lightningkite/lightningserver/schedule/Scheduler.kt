package com.lightningkite.lightningserver.schedule

import com.lightningkite.lightningserver.ServerRunner
import java.time.Duration
import java.time.LocalTime
import java.time.OffsetTime
import java.time.ZoneId
import java.util.TimeZone

data class ScheduledTask(val name: String, val schedule: Schedule, val handler: suspend ServerRunner.() -> Unit)

sealed class Schedule {
    data class Frequency(val gap: Duration): Schedule()
    data class Daily(val time: LocalTime, val zone: ZoneId = ZoneId.systemDefault()): Schedule()
}