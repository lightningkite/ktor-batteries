// Package: com.lightningkite.ktordb.mock
// Generated by Khrysalis - this file will be overwritten.
import { Condition } from '../Condition'
import { HasId } from '../HasId'
import { SignalData } from '../SignalData'
import { UUIDFor } from '../UUIDFor'
import { EqualOverrideMap, runOrNull } from '@lightningkite/khrysalis-runtime'
import { execPipe, filter, toArray } from 'iter-tools-es'
import { Observable, Subject } from 'rxjs'
import { map } from 'rxjs/operators'

//! Declares com.lightningkite.ktordb.mock.MockTable
export class MockTable<Model extends HasId> {
    public constructor() {
        this.data = new EqualOverrideMap([]);
        this.signals = new Subject();
    }
    
    
    public readonly data: Map<UUIDFor<Model>, Model>;
    public readonly signals: Subject<SignalData<Model>>;
    
    public observe(condition: Condition<Model>): Observable<Array<Model>> {
        return this.signals.pipe(map((it: SignalData<Model>): Array<Model> => execPipe(this.data.values(), filter((it: Model): boolean => condition.invoke(it)), toArray)));
    }
    
    public getItem(id: UUIDFor<Model>): (Model | null) {
        return (this.data.get(id) ?? null);
    }
    
    public asList(): Array<Model> {
        return toArray(this.data.values());
    }
    
    public addItem(item: Model): Model {
        this.data.set(item._id, item);
        this.signals.next(new SignalData<Model>(item, true, false));
        return item;
    }
    
    public replaceItem(item: Model): Model {
        this.data.set(item._id, item);
        this.signals.next(new SignalData<Model>(item, false, false));
        return item;
    }
    
    public deleteItem(item: Model): void {
        this.deleteItemById(item._id);
    }
    
    public deleteItemById(id: UUIDFor<Model>): void {
        const item_16 = (this.data.get(id) ?? null);
        if (item_16 !== null) {
            this.data.delete(id);
            this.signals.next(new SignalData<Model>(item_16, false, true));
        }
    }
}