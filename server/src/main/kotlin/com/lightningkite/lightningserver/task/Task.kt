package com.lightningkite.lightningserver.task

import com.lightningkite.lightningserver.ServerRunner
import kotlinx.serialization.KSerializer


data class Task<INPUT>(
    val name: String,
    val serializer: KSerializer<INPUT>,
    val implementation: suspend ServerRunner.(INPUT) -> Unit
)
