@file:SharedCode

package com.lightningkite.ktordb.live

import com.lightningkite.khrysalis.SharedCode
import com.lightningkite.khrysalis.SwiftReturnType
import com.lightningkite.ktordb.*
import com.lightningkite.rx.okhttp.HttpClient
import io.reactivex.rxjava3.core.Observable
import java.util.*
import java.util.concurrent.TimeUnit

class LiveObserveModelApi<Model : HasId<UUID>>(
    val openSocket: (query: Query<Model>) -> Observable<List<Model>>
) : ObserveModelApi<Model>() {

    companion object {
        inline fun <reified Model : HasId<UUID>> create(
            multiplexUrl: String,
            token: String?,
            headers: Map<String, String>,
            path: String
        ): LiveObserveModelApi<Model> = LiveObserveModelApi(
            openSocket = { query ->
                multiplexedSocket<ListChange<Model>, Query<Model>>(
                    if (token != null) "$multiplexUrl?jwt=$token" else multiplexUrl,
                    path
                )
                    .filter(query)
            }
        )
    }

    val alreadyOpen = HashMap<Query<Model>, Observable<List<Model>>>()

    override fun observe(query: Query<Model>): Observable<List<Model>> {
        //multiplexedSocket<ListChange<Model>, Query<Model>>("$multiplexUrl?jwt=$token", path)
        return alreadyOpen.getOrPut(query) {
            openSocket(query)
                .doOnDispose { alreadyOpen.remove(query) }
                .replay(1)
                .refCount()
        }
    }
}

fun <T : HasId<UUID>> Observable<ListChange<T>>.toListObservable(ordering: Comparator<T>): Observable<List<T>> {
    val localList = ArrayList<T>()
    return map {
        it.wholeList?.let { localList.clear(); localList.addAll(it.sortedWith(ordering)) }
        it.new?.let {
            localList.removeAll { o -> it._id == o._id }
            var index = localList.indexOfFirst { inList -> ordering.compare(it, inList) < 0 }
            if (index == -1) index = localList.size
            localList.add(index, it)
        } ?: it.old?.let { localList.removeAll { o -> it._id == o._id } }
        localList.toList()
    }
}

fun <T : HasId<UUID>> Observable<WebSocketIsh<ListChange<T>, Query<T>>>.filter(query: Query<T>): Observable<List<T>> =
    this
        .doOnNext { it.send(query) }
        .switchMap { it.messages }
        .retryWhen @SwiftReturnType("Observable<Error>") {
            it.delay(
                5000L,
                TimeUnit.MILLISECONDS,
                HttpClient.responseScheduler!!
            )
        }
        .toListObservable(query.orderBy.comparator ?: compareBy { it._id })