package com.lightningkite.ktordb

import com.lightningkite.khrysalis.IsCodableAndHashable
import kotlinx.serialization.Serializable

@Serializable
data class GroupCountQuery<Model : IsCodableAndHashable>(
    val condition: Condition<Model> = Condition.Always(),
    val groupBy: KProperty1Partial<Model>
)

@Serializable
data class AggregateQuery<Model : IsCodableAndHashable>(
    val aggregate: Aggregate,
    val condition: Condition<Model> = Condition.Always(),
    val property: KProperty1Partial<Model>
)

@Serializable
data class GroupAggregateQuery<Model : IsCodableAndHashable>(
    val aggregate: Aggregate,
    val condition: Condition<Model> = Condition.Always(),
    val groupBy: KProperty1Partial<Model>,
    val property: KProperty1Partial<Model>
)
