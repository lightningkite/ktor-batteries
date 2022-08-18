@file:SharedCode

package com.lightningkite.ktordb.live

import com.lightningkite.khrysalis.*
import com.lightningkite.ktordb.MultiplexMessage
import com.lightningkite.rx.mapNotNull
import com.lightningkite.rx.okhttp.*
import io.reactivex.rxjava3.core.Observable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import java.util.*
import java.util.concurrent.TimeUnit

var sharedSocketShouldBeActive: Observable<Boolean> = Observable.just(false)

var _overrideWebSocketProvider: ((url: String) -> Observable<WebSocketInterface>)? = null
private var retryTime = 1000L
private var lastRetry = 0L
private val sharedSocketCache = HashMap<String, Observable<WebSocketInterface>>()
fun sharedSocket(url: String): Observable<WebSocketInterface> {
    return sharedSocketCache.getOrPut(url) {
        sharedSocketShouldBeActive
            .distinctUntilChanged()
            .switchMap {
                val shortUrl = url.substringBefore('?')
                if (!it) Observable.never<WebSocketInterface>()
                else {
                    println("Creating socket to $url")
                    (_overrideWebSocketProvider?.invoke(url) ?: HttpClient.webSocket(url))
                        .switchMap {
                            lastRetry = System.currentTimeMillis()
//                        println("Connection to $shortUrl established, starting pings")
                            // Only have this observable until it fails

                            val pingMessages: Observable<WebSocketInterface> =
                                Observable.interval(5000L, TimeUnit.MILLISECONDS, HttpClient.responseScheduler!!)
                                    .map { _ ->
//                            println("Sending ping to $url")
                                        it.write.onNext(WebSocketFrame(text = ""))
                                    }.switchMap { Observable.never() }

                            val timeoutAfterSeconds: Observable<WebSocketInterface> = it.read
                                .doOnNext {
//                                println("Got message from $shortUrl: ${it}")
                                    if (System.currentTimeMillis() > lastRetry + 60_000L) {
                                        retryTime = 1000L
                                    }
                                }
                                .timeout(10_000L, TimeUnit.MILLISECONDS, HttpClient.responseScheduler!!)
                                .switchMap { Observable.never() }

                            Observable.merge(
                                Observable.just(it),
                                pingMessages,
                                timeoutAfterSeconds
                            )
                        }
                        .doOnError { println("Socket to $shortUrl FAILED with $it") }
                        .retryWhen @SwiftReturnType("Observable<Error>") {
                            val temp = retryTime
                            retryTime = temp * 2L
                            it.delay(temp, TimeUnit.MILLISECONDS, HttpClient.responseScheduler!!)
                        }
                        .doOnDispose {
                            println("Disconnecting socket to $shortUrl")
                        }
                }
            }
            .replay(1)
            .refCount()
    }
}

class MultiplexedWebsocketPart(val messages: Observable<String>, val send: (String) -> Unit)
class WebSocketIsh<IN : IsCodableAndHashable, OUT : IsCodableAndHashable>(
    val messages: Observable<IN>,
    val send: (OUT) -> Unit
)

@JsName("multiplexedSocketReified")
inline fun <reified IN : IsCodableAndHashableNotNull, reified OUT : IsCodableAndHashable> multiplexedSocket(
    url: String,
    path: String,
    noinline onSetup: (WebSocketIsh<IN, OUT>) -> Unit = {}
): Observable<WebSocketIsh<IN, OUT>> = multiplexedSocket(url, path, serializer<IN>(), serializer<OUT>(), onSetup)

@JsName("multiplexedSocket")
fun <IN : IsCodableAndHashableNotNull, OUT : IsCodableAndHashable> multiplexedSocket(
    url: String,
    path: String,
    inType: KSerializer<IN>,
    outType: KSerializer<OUT>,
    onSetup: (WebSocketIsh<IN, OUT>) -> Unit = {}
): Observable<WebSocketIsh<IN, OUT>> {
    val shortUrl = url.substringBefore('?')
    val channel = UUID.randomUUID().toString()
    var lastSocket: WebSocketInterface? = null
    return sharedSocket(url)
        .map {
//            println("Setting up channel on socket to $shortUrl with $path")
            lastSocket = it
            it.write.onNext(
                WebSocketFrame(
                    text = MultiplexMessage(
                        channel = channel,
                        path = path,
                        start = true
                    ).toJsonString()
                )
            )
            val part = MultiplexedWebsocketPart(
                messages = it.read.mapNotNull {
                    val text = it.text ?: return@mapNotNull null
                    if (text == "") return@mapNotNull null
                    val message: MultiplexMessage = text.fromJsonString() ?: return@mapNotNull null
                    if (message.channel == channel) message.data else null
                },
                send = { message ->
                    it.write.onNext(
                        WebSocketFrame(
                            text = MultiplexMessage(
                                channel = channel,
                                data = message
                            ).toJsonString()
                        )
                    )
                }
            )
            val typedPart = WebSocketIsh<IN, OUT>(
                messages = part.messages.mapNotNull { it.fromJsonString(inType) },
                send = { m -> part.send(m.toJsonString(outType)) }
            )
            onSetup(typedPart)
            typedPart
        }
        .doOnDispose {
//            println("Disconnecting channel on socket to $shortUrl with $path")
            lastSocket?.write?.onNext(
                WebSocketFrame(
                    text = MultiplexMessage(
                        channel = channel,
                        path = path,
                        end = true
                    ).toJsonString()
                )
            )
        }
}
