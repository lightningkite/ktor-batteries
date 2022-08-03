package com.lightningkite.ktordb

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.reflect.KProperty1

open class InMemoryFieldCollection<Model : Any>(val data: MutableList<Model> = ArrayList()) :
    AbstractSignalFieldCollection<Model>() {

    private val lock = ReentrantLock()
    val changeChannel = MutableSharedFlow<EntryChange<Model>>()

    override suspend fun find(
        condition: Condition<Model>,
        orderBy: List<SortPart<Model>>,
        skip: Int,
        limit: Int,
        maxQueryMs: Long
    ): Flow<Model> = flow {
        val result = lock.withLock {
            data.asSequence()
                .filter { condition(it) }
                .let {
                    orderBy.comparator?.let { c ->
                        it.sortedWith(c)
                    } ?: it
                }
                .drop(skip)
                .take(limit)
                .toList()
        }
        result
            .forEach {
                emit(it)
            }
    }

    override suspend fun count(condition: Condition<Model>): Int = data.count { condition(it) }

    override suspend fun <Key> groupCount(
        condition: Condition<Model>,
        groupBy: KProperty1<Model, Key>,
    ): Map<Key, Int> = data.groupingBy { groupBy.get(it) }.eachCount()

    override suspend fun <N : Number> aggregate(
        aggregate: Aggregate,
        condition: Condition<Model>,
        property: KProperty1<Model, N>
    ): Double? = data.asSequence().map { property.get(it).toDouble() }.aggregate(aggregate)

    override suspend fun <N : Number?, Key> groupAggregate(
        aggregate: Aggregate,
        condition: Condition<Model>,
        groupBy: KProperty1<Model, Key>,
        property: KProperty1<Model, N>,
    ): Map<Key, Double?> =
        data.asSequence().mapNotNull { groupBy.get(it) to (property.get(it)?.toDouble() ?: return@mapNotNull null) }
            .aggregate(aggregate)

    override suspend fun insertImpl(models: List<Model>): List<Model> = lock.withLock {
        data.addAll(models)
        models.forEach { model ->
            changeChannel.tryEmit(EntryChange(null, model))
        }
        return models
    }

    override suspend fun replaceOneImpl(condition: Condition<Model>, model: Model): EntryChange<Model> = lock.withLock {
        for (it in data.indices) {
            val old = data[it]
            if (condition(old)) {
                data[it] = model
                val changes = EntryChange(old, model)
                changeChannel.tryEmit(changes)
                return changes
            }
        }
        return EntryChange(null, null)
    }

    override suspend fun upsertOneImpl(
        condition: Condition<Model>,
        modification: Modification<Model>,
        model: Model
    ): EntryChange<Model> = lock.withLock {
        for (it in data.indices) {
            val old = data[it]
            if (condition(old)) {
                val changed = modification(old)
                data[it] = changed
                val changes = EntryChange(old, changed)
                changeChannel.tryEmit(changes)
                return changes
            }
        }
        data.add(model)
        val changes = EntryChange(null, model)
        changeChannel.tryEmit(changes)
        return changes
    }

    override suspend fun updateOneImpl(
        condition: Condition<Model>,
        modification: Modification<Model>
    ): EntryChange<Model> = lock.withLock {
        for (it in data.indices) {
            val old = data[it]
            if (condition(old)) {
                val new = modification(old)
                data[it] = new
                val changes = EntryChange(old, new)
                changeChannel.tryEmit(changes)
                return changes
            }
        }
        return EntryChange(null, null)
    }

    override suspend fun updateManyImpl(
        condition: Condition<Model>,
        modification: Modification<Model>
    ): CollectionChanges<Model> = lock.withLock {
        val changes = ArrayList<EntryChange<Model>>()
        var counter = 0
        data.indices.forEach {
            val old = data[it]
            if (condition(old)) {
                val new = modification(old)
                data[it] = new
                val change = EntryChange(old, new)
                changeChannel.tryEmit(change)
                changes.add(change)
                counter++
            }
        }
        return CollectionChanges(changes = changes)
    }

    override suspend fun deleteOneImpl(condition: Condition<Model>): Model? = lock.withLock {
        for (it in data.indices) {
            val old = data[it]
            if (condition(old)) {
                data.removeAt(it)
                changeChannel.tryEmit(EntryChange(old, null))
                return old
            }
        }
        return null
    }

    override suspend fun deleteManyImpl(condition: Condition<Model>): List<Model> = lock.withLock {
        val removed = ArrayList<Model>()
        data.removeAll {
            if (condition(it)) {
                changeChannel.tryEmit(EntryChange(it, null))
                removed.add(it)
                true
            } else {
                false
            }
        }
        return removed
    }

    override suspend fun watch(condition: Condition<Model>): Flow<EntryChange<Model>> =
        changeChannel.onEach { delay(1L) }

    override suspend fun replaceOneIgnoringResultImpl(condition: Condition<Model>, model: Model): Boolean =
        replaceOne(condition, model).new != null

    override suspend fun upsertOneIgnoringResultImpl(
        condition: Condition<Model>,
        modification: Modification<Model>,
        model: Model
    ): Boolean = upsertOne(condition, modification, model).old != null

    override suspend fun updateOneIgnoringResultImpl(
        condition: Condition<Model>,
        modification: Modification<Model>
    ): Boolean = updateOne(condition, modification).new != null

    override suspend fun updateManyIgnoringResultImpl(
        condition: Condition<Model>,
        modification: Modification<Model>
    ): Int = updateMany(condition, modification).changes.size

    override suspend fun deleteOneIgnoringOldImpl(condition: Condition<Model>): Boolean = deleteOne(condition) != null

    override suspend fun deleteManyIgnoringOldImpl(condition: Condition<Model>): Int = deleteMany(condition).size
}

