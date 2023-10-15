// Package: com.lightningkite.ktordb.live
// Generated by Khrysalis - this file will be overwritten.
import { ObserveModelApi } from '../ObserveModelApi'
import { HasId } from '../db/HasId'
import { ListChange } from '../db/ListChange'
import { Query } from '../db/Query'
import { xListComparatorGet } from '../db/SortPart'
import { WebSocketIsh, multiplexedSocketReified } from './sockets'
import { Comparable, Comparator, EqualOverrideMap, compareBy, listRemoveAll, runOrNull, safeEq, xMutableMapGetOrPut } from '@lightningkite/khrysalis-runtime'
import { Observable } from 'rxjs'
import { delay, map, publishReplay, refCount, retryWhen, switchMap, tap } from 'rxjs/operators'

//! Declares com.lightningkite.ktordb.live.LiveObserveModelApi
export class LiveObserveModelApi<Model extends HasId<string>> extends ObserveModelApi<Model> {
    public constructor(public readonly openSocket: ((query: Query<Model>) => Observable<Array<Model>>)) {
        super();
        this.alreadyOpen = new EqualOverrideMap<Query<Model>, Observable<Array<Model>>>();
    }
    
    
    
    
    public readonly alreadyOpen: Map<Query<Model>, Observable<Array<Model>>>;
    
    public observe(query: Query<Model>): Observable<Array<Model>> {
        //multiplexedSocket<ListChange<Model>, Query<Model>>("$multiplexUrl?jwt=$token", path)
        return xMutableMapGetOrPut<Query<Model>, Observable<Array<Model>>>(this.alreadyOpen, query, (): Observable<Array<Model>> => (this.openSocket(query)
                .pipe(tap({ unsubscribe: (): void => {
                this.alreadyOpen.delete(query);
            } }))
                .pipe(publishReplay(1))
            .pipe(refCount())));
    }
}
export namespace LiveObserveModelApi {
    //! Declares com.lightningkite.ktordb.live.LiveObserveModelApi.Companion
    export class Companion {
        private constructor() {
        }
        public static INSTANCE = new Companion();
        
        public create<Model extends HasId<string>>(Model: Array<any>, multiplexUrl: string, token: (string | null), headers: Map<string, string>, path: string): LiveObserveModelApi<Model> {
            return new LiveObserveModelApi<Model>((query: Query<Model>): Observable<Array<Model>> => (xObservableFilter<Model>(multiplexedSocketReified<ListChange<Model>, Query<Model>>([ListChange, Model], [Query, Model], ((): string => {
                if (token !== null) { return `${multiplexUrl}?jwt=${token}` } else { return multiplexUrl }
            })(), path), query)));
        }
    }
}

//! Declares com.lightningkite.ktordb.live.toListObservable>io.reactivex.rxjava3.core.Observablecom.lightningkite.ktordb.ListChangecom.lightningkite.ktordb.live.toListObservable.T
export function xObservableToListObservable<T extends HasId<string>>(this_: Observable<ListChange<T>>, ordering: Comparator<T>): Observable<Array<T>> {
    const localList = ([] as Array<T>);
    return this_.pipe(map((it: ListChange<T>): Array<T> => {
        const it_7 = it.wholeList;
        if (it_7 !== null) {
            localList.length = 0; localList.push(...it_7.slice().sort(ordering));
        }
        const it_9 = it._new;
        if (it_9 !== null) {
            listRemoveAll(localList, (o: T): boolean => (safeEq(it_9._id, o._id)));
            let index = localList.findIndex((inList: T): boolean => (ordering(it_9, inList) < 0));
            if (index === (-1)) { index = localList.length }
            localList.splice(index, 0, it_9);
        } else {
            const it_16 = it.old;
            if (it_16 !== null) {
                listRemoveAll(localList, (o: T): boolean => (safeEq(it_16._id, o._id)));
            }
        }
        return Array.from(localList);
    }));
}

//! Declares com.lightningkite.ktordb.live.filter>io.reactivex.rxjava3.core.Observablecom.lightningkite.ktordb.live.WebSocketIshcom.lightningkite.ktordb.ListChangecom.lightningkite.ktordb.live.filter.T, com.lightningkite.ktordb.Querycom.lightningkite.ktordb.live.filter.T
export function xObservableFilter<T extends HasId<string>>(this_: Observable<WebSocketIsh<ListChange<T>, Query<T>>>, query: Query<T>): Observable<Array<T>> {
    return xObservableToListObservable<T>(this_
            .pipe(tap((it: WebSocketIsh<ListChange<T>, Query<T>>): void => {
            it.send(query);
        }))
            .pipe(switchMap((it: WebSocketIsh<ListChange<T>, Query<T>>): Observable<ListChange<T>> => (it.messages)))
        .pipe(retryWhen( (it: Observable<any>): Observable<any> => (it.pipe(delay(5000))))), xListComparatorGet(query.orderBy) ?? compareBy<T>((it: T): (Comparable<(any | null)> | null) => (it._id)));
}