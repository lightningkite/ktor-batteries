package com.lightningkite.lightningserver.task

import com.lightningkite.lightningserver.ServerRunner

data class StartupTask(
    val name: String,
    val implementation: suspend ServerRunner.() -> Unit
)