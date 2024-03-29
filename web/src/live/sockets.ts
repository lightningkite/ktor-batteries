// Package: com.lightningkite.ktordb.live
// Generated by Khrysalis - this file will be overwritten.
import {MultiplexMessage} from '../db/MultiplexMessage'
import {
    ReifiedType,
    runOrNull,
    safeEq,
    xCharSequenceIsBlank,
    xMutableMapGetOrPut,
    xStringSubstringBefore
} from '@lightningkite/khrysalis-runtime'
import {doOnSubscribe, HttpClient, isNonNull, JSON2, WebSocketFrame, WebSocketInterface} from '@lightningkite/rxjs-plus'
import {filter as rFilter, interval, map as rMap, merge, NEVER, Observable, of, Subject, SubscriptionLike} from 'rxjs'
import {
    delay,
    distinctUntilChanged,
    filter,
    map,
    publishReplay,
    refCount,
    retryWhen,
    switchMap,
    tap,
    timeout
} from 'rxjs/operators'
import {v4 as randomUuidV4} from 'uuid'

//! Declares com.lightningkite.ktordb.live.sharedSocketShouldBeActive
export let _sharedSocketShouldBeActive: Observable<boolean> = of(true);

export function getSharedSocketShouldBeActive(): Observable<boolean> {
    return _sharedSocketShouldBeActive;
}

export function setSharedSocketShouldBeActive(value: Observable<boolean>) {
    _sharedSocketShouldBeActive = value;
}

let retryTime = 1000;
let lastRetry = 0;

//! Declares com.lightningkite.ktordb.live._overrideWebSocketProvider
export let __overrideWebSocketProvider: (((url: string) => Observable<WebSocketInterface>) | null) = null;

export function get_overrideWebSocketProvider(): (((url: string) => Observable<WebSocketInterface>) | null) {
    return __overrideWebSocketProvider;
}

export function set_overrideWebSocketProvider(value: (((url: string) => Observable<WebSocketInterface>) | null)) {
    __overrideWebSocketProvider = value;
}

const sharedSocketCache = new Map<string, Observable<WebSocketInterface>>();

//! Declares com.lightningkite.ktordb.live.sharedSocket
export function sharedSocket(url: string): Observable<WebSocketInterface> {
    return xMutableMapGetOrPut<string, Observable<WebSocketInterface>>(sharedSocketCache, url, (): Observable<WebSocketInterface> => (getSharedSocketShouldBeActive()
        .pipe(distinctUntilChanged(safeEq))
        .pipe(switchMap((it: boolean): Observable<WebSocketInterface> => {
            const shortUrl = xStringSubstringBefore(url, '?', undefined);
            return ((): Observable<WebSocketInterface> => {
                if ((!it)) {
                    return NEVER
                } else {
                    console.log(`Creating socket to ${url}`);
                    return (runOrNull(get_overrideWebSocketProvider(), _ => _(url)) ?? HttpClient.INSTANCE.webSocket(url))
                        .pipe(switchMap((it: WebSocketInterface): Observable<WebSocketInterface> => {
                            lastRetry = Date.now();
                            //                            println("Connection to $shortUrl established, starting pings")
                            // Only have this observable until it fails

                            const pingMessages: Observable<WebSocketInterface> = interval(30000)
                                .pipe(map((_0: number): void => {
                                    //                                        println("Sending ping to $url")
                                    return it.write.next({text: " ", binary: null});
                                })).pipe(switchMap((it: void): Observable<WebSocketInterface> => (NEVER)));

                            const timeoutAfterSeconds: Observable<WebSocketInterface> = it.read
                                .pipe(tap((it: WebSocketFrame): void => {
                                    //                                    println("Got message from $shortUrl: ${it}")
                                    if (Date.now() > lastRetry + 60000) {
                                        retryTime = 1000;
                                    }
                                }))
                                .pipe(timeout(40000))
                                .pipe(switchMap((it: WebSocketFrame): Observable<WebSocketInterface> => (NEVER)));

                            return merge(of(it), pingMessages, timeoutAfterSeconds);
                        }))
                        .pipe(tap(undefined, (it: any): void => {
                            console.log(`Socket to ${shortUrl} FAILED with ${it}`);
                        }))
                        .pipe(retryWhen((it: Observable<any>): Observable<any> => {
                            const temp = retryTime;
                            retryTime = temp * 2;
                            return it.pipe(delay(temp));
                        }))
                        .pipe(tap({
                            unsubscribe: (): void => {
                                console.log(`Disconnecting socket to ${shortUrl}`);
                            }
                        }));
                }
            })()
        }))
        .pipe(publishReplay(1))
        .pipe(refCount())));
}

//! Declares com.lightningkite.ktordb.live.MultiplexedWebsocketPart
export class MultiplexedWebsocketPart {
    public constructor(public readonly messages: Observable<string>, public readonly send: ((a: string) => void)) {
    }
}

//! Declares com.lightningkite.ktordb.live.WebSocketIsh
export class WebSocketIsh<IN extends any, OUT extends any> {
    public constructor(public readonly messages: Observable<IN>, public readonly send: ((a: OUT) => void)) {
    }
}

//! Declares com.lightningkite.ktordb.live.multiplexedSocket
export function multiplexedSocketReified<IN extends any, OUT extends any>(IN: Array<any>, OUT: Array<any>, url: string, path: string): Observable<WebSocketIsh<IN, OUT>> {
    return multiplexedSocket<IN, OUT>(url, path, IN, OUT);
}

//! Declares com.lightningkite.ktordb.live.multiplexedSocket
export function multiplexedSocket<IN extends any, OUT extends any>(url: string, path: string, inType: ReifiedType, outType: ReifiedType): Observable<WebSocketIsh<IN, OUT>> {
    const shortUrl = xStringSubstringBefore(url, '?', undefined);
    const channel = randomUuidV4();
    return sharedSocket(url)
        .pipe(switchMap((sharedSocket: WebSocketInterface): Observable<WebSocketIsh<IN, OUT>> => {
            //            println("Setting up channel $channel to $shortUrl with $path")
            const multiplexedIn = sharedSocket.read.pipe(rMap((it: WebSocketFrame): (MultiplexMessage | null) => {
                const text = it.text
                if (text === null) {
                    return null
                }
                if (xCharSequenceIsBlank(text)) {
                    return null
                }
                return JSON2.parse<MultiplexMessage>(text, [MultiplexMessage]);
            }), rFilter(isNonNull))
                .pipe(filter((it: MultiplexMessage): boolean => (it.channel === channel)));
            let current = new Subject<IN>();
            return multiplexedIn
                .pipe(rMap((message: MultiplexMessage): (WebSocketIsh<IN, OUT> | null) => (((): (WebSocketIsh<IN, OUT> | null) => {
                    if (message.start) {
                        //                            println("Channel ${message.channel} established with $sharedSocket")
                        return new WebSocketIsh<IN, OUT>(current, (message: OUT): void => {
                            //                                    println("Sending $message to $channel")
                            sharedSocket.write.next({
                                text: JSON.stringify(new MultiplexMessage(channel, undefined, undefined, undefined, JSON.stringify(message))),
                                binary: null
                            });
                        });
                    } else if (message.data !== null) {
                        // console.log("Got ${message.data} to ${message.channel}")
                        const temp53 = message.data;
                        if (temp53 === null || temp53 === undefined) {
                            return null
                        }
                        current.next(JSON2.parse<IN>(temp53, inType))
                        return null;
                    } else if (message.end) {
                        //                            println("Channel ${message.channel} terminated")
                        current = new Subject();
                        sharedSocket.write.next({
                            text: JSON.stringify(new MultiplexMessage(channel, path, true, undefined, undefined)),
                            binary: null
                        });
                        return null;
                    } else {
                        return null
                    }
                })())), rFilter(isNonNull))
                .pipe(doOnSubscribe((_0: SubscriptionLike): void => {
                    //                    println("Sending onSubscribe Startup Message")
                    sharedSocket.write.next({
                        text: JSON.stringify(new MultiplexMessage(channel, path, true, undefined, undefined)),
                        binary: null
                    });
                }))
                .pipe(tap({
                    unsubscribe: (): void => {
                        //                    println("Disconnecting channel on socket to $shortUrl with $path")
                        sharedSocket.write.next({
                            text: JSON.stringify(new MultiplexMessage(channel, path, undefined, true, undefined)),
                            binary: null
                        });
                    }
                }))
                .pipe(retryWhen((it: Observable<any>): Observable<any> => {
                    const temp = retryTime;
                    retryTime = temp * 2;
                    return it.pipe(delay(temp));
                }));
        }));
}
