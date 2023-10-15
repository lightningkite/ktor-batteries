// Package: com.lightningkite.ktordb.mock
// Generated by Khrysalis - this file will be overwritten.
import { ObserveModelApi } from '../ObserveModelApi'
import { HasId } from '../db/HasId'
import { Query } from '../db/Query'
import { MockTable } from './MockTable'
import { Observable, startWith } from 'rxjs'

//! Declares com.lightningkite.ktordb.mock.MockObserveModelApi
export class MockObserveModelApi<Model extends HasId<string>> extends ObserveModelApi<Model> {
    public constructor(public readonly table: MockTable<Model>) {
        super();
    }
    
    public observe(query: Query<Model>): Observable<Array<Model>> {
        return this.table.observe(query.condition).pipe(startWith(this.table.asList().filter((item: Model): boolean => (query.condition.invoke(item)))));
    }
}