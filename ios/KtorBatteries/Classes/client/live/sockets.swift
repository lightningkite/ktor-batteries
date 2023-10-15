// Package: com.lightningkite.ktordb.live
// Generated by Khrysalis - this file will be overwritten.
import KhrysalisRuntime
import RxSwift
import RxSwiftPlus
import Foundation

public var sharedSocketShouldBeActive: Observable<Bool> = Observable.just(true)
private var retryTime = 1000
private var lastRetry = 0

public var _overrideWebSocketProvider: ((String) -> Observable<WebSocketInterface>)? = nil
private var sharedSocketCache = Dictionary<String, Observable<WebSocketInterface>>()
public func sharedSocket(url: String) -> Observable<WebSocketInterface> {
    return sharedSocketCache.getOrPut(key: url) { () -> Observable<WebSocketInterface> in sharedSocketShouldBeActive
            .distinctUntilChanged()
            .switchMap { (it) -> Observable<WebSocketInterface> in
            let shortUrl = url.substringBefore(delimiter: "?")
            return run { () -> Observable<WebSocketInterface> in
                if (!it) { return (Observable.never() as Observable<WebSocketInterface>) } else {
                    print("Creating socket to \(String(kotlin: url))")
                    return (_overrideWebSocketProvider?(url) ?? HttpClient.INSTANCE.webSocket(url: url))
                        .switchMap { (it) -> Observable<WebSocketInterface> in
                        lastRetry = Int(Date().timeIntervalSince1970 * 1000.0)
                        //                            println("Connection to $shortUrl established, starting pings")
                        // Only have this observable until it fails
                        
                        let pingMessages: Observable<WebSocketInterface> = Observable<Int>.interval(RxTimeInterval.milliseconds(Int(30000)), scheduler: HttpClient.INSTANCE.responseScheduler!)
                            .map { (_) -> Void in it.write.onNext(WebSocketFrame(text: " ")) }.switchMap { (it) -> Observable<WebSocketInterface> in Observable.never() }
                        
                        let timeoutAfterSeconds: Observable<WebSocketInterface> = it.read
                            .doOnNext { (it) -> Void in if Int(Date().timeIntervalSince1970 * 1000.0) > lastRetry + 60000 {
                            retryTime = 1000
                        } }
                            .timeout(.milliseconds(40000), scheduler: MainScheduler.instance)
                            .switchMap { (it) -> Observable<WebSocketInterface> in Observable.never() }
                        
                        return Observable.merge(Observable.just(it), pingMessages, timeoutAfterSeconds)
                    }
                        .doOnError { (it) -> Void in print("Socket to \(String(kotlin: shortUrl)) FAILED with \(it)") }
                        .retry(when:  { (it) -> Observable<Error> in
                        let temp = retryTime
                        retryTime = temp * 2
                        return it.delay(.milliseconds(temp), scheduler: MainScheduler.instance)
                    })
                        .doOnDispose { () -> Void in print("Disconnecting socket to \(String(kotlin: shortUrl))") }
                }
            }
        }
            .replay(1)
        .refCount() }
}

public final class MultiplexedWebsocketPart {
    public var messages: Observable<String>
    public var send: (String) -> Void
    public init(messages: Observable<String>, send: @escaping (String) -> Void) {
        self.messages = messages
        self.send = send
        //Necessary properties should be initialized now
    }
}
public final class WebSocketIsh<IN : Codable & Hashable, OUT : Codable & Hashable> {
    public var messages: Observable<IN>
    public var send: (OUT) -> Void
    public init(messages: Observable<IN>, send: @escaping (OUT) -> Void) {
        self.messages = messages
        self.send = send
        //Necessary properties should be initialized now
    }
}

public func multiplexedSocket<IN : Codable & Hashable, OUT : Codable & Hashable>(url: String, path: String) -> Observable<WebSocketIsh<IN, OUT>> {
    return multiplexedSocket(url: url, path: path, inType: IN.self, outType: OUT.self);
}

public func multiplexedSocket<IN : Codable & Hashable, OUT : Codable & Hashable>(url: String, path: String, inType: IN.Type, outType: OUT.Type) -> Observable<WebSocketIsh<IN, OUT>> {
    let shortUrl = url.substringBefore(delimiter: "?")
    let channel = String(kotlin: UUID.randomUUID())

    return sharedSocket(url: url)
        .switchMap { sharedSocket in
            print("Setting up channel $channel to $shortUrl with $path")
            let multiplexedIn: Observable<MultiplexMessage> = sharedSocket.read.compactMap({ (it) -> MultiplexMessage? in
                guard let text = it.text else { return nil }
                if text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { return nil }
                guard let message: MultiplexMessage = text.fromJsonString() else {
                    return nil
                }
                return message
            })
                .filter { (it) -> Bool in it.channel == channel }

            var current = PublishSubject<IN>()
            return multiplexedIn
                .compactMap { message in
                    if message.start {
                        print("Channel ${message.channel} established with $sharedSocket")
                        return (WebSocketIsh(
                            messages: current as Observable<IN>,
                            send: { (message) -> Void in
                                sharedSocket.write.onNext(
                                    WebSocketFrame(
                                        text: MultiplexMessage(
                                            channel: channel,
                                            data: message.toJsonString()).toJsonString()
                                    )
                                )
                            } as (OUT) -> Void) as WebSocketIsh<IN, OUT>)
                    } else if message.data != nil {
                        print("Got ${message.data} to ${message.channel}")
                        guard let next:IN = message.data?.fromJsonString() else {
                            return nil
                        }
                        current.onNext(next)

                        return nil
                    } else if message.end {
                        print("Channel ${message.channel} terminated")
                        current = PublishSubject()
                        sharedSocket.write.onNext(WebSocketFrame(text: MultiplexMessage(channel: channel, path: path, start: true).toJsonString()))
                        return nil
                    } else  {
                        return nil
                    }

//                    return (WebSocketIsh(messages: current as Observable<IN>, send: { (message) -> Void in sharedSocket.write.onNext(WebSocketFrame(text: MultiplexMessage(channel: channel, data: message.toJsonString()).toJsonString())) } as (OUT) -> Void) as WebSocketIsh<IN, OUT>)
                }
                .doOnSubscribe { (_) -> Void in sharedSocket.write.onNext(WebSocketFrame(text: MultiplexMessage(channel: channel, path: path, start: true).toJsonString())) }
                .doOnDispose { () -> Void in sharedSocket.write.onNext(WebSocketFrame(text: MultiplexMessage(channel: channel, path: path, end: true).toJsonString())) }
                .retry(when:  { (it) -> Observable<Error> in
                    let temp = retryTime
                    retryTime = temp * 2
                    return it.delay(.milliseconds(temp), scheduler: MainScheduler.instance)
                })

        }
}

