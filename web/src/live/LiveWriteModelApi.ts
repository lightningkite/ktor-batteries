// Package: com.lightningkite.ktordb.live
// Generated by Khrysalis - this file will be overwritten.
import { Condition } from '../Condition'
import { HasId } from '../HasId'
import { MassModification } from '../MassModification'
import { Modification } from '../Modification'
import { UUIDFor } from '../UUIDFor'
import { WriteModelApi } from '../WriteModelApi'
import { ReifiedType } from '@lightningkite/khrysalis-runtime'
import { HttpBody, HttpClient, fromJSON, unsuccessfulAsError } from '@lightningkite/rxjs-plus'
import { Observable, switchMap } from 'rxjs'

//! Declares com.lightningkite.ktordb.live.LiveWriteModelApi
export class LiveWriteModelApi<Model extends HasId> extends WriteModelApi<Model> {
    public constructor(public readonly url: string, public readonly token: string, public readonly serializer: ReifiedType) {
        super();
    }
    
    
    public post(value: Model): Observable<Model> {
        return HttpClient.INSTANCE.call(this.url, HttpClient.INSTANCE.POST, new Map([["Authorization", `Bearer ${this.token}`]]), HttpBody.json(value), undefined).pipe(unsuccessfulAsError, fromJSON<Model>(this.serializer));
    }
    
    public postBulk(values: Array<Model>): Observable<Array<Model>> {
        return HttpClient.INSTANCE.call(`${this.url}/bulk`, HttpClient.INSTANCE.POST, new Map([["Authorization", `Bearer ${this.token}`]]), HttpBody.json(values), undefined).pipe(unsuccessfulAsError, fromJSON<Array<Model>>([Array, this.serializer]));
    }
    
    public put(value: Model): Observable<Model> {
        return HttpClient.INSTANCE.call(`${this.url}/${value._id}`, HttpClient.INSTANCE.PUT, new Map([["Authorization", `Bearer ${this.token}`]]), HttpBody.json(value), undefined).pipe(unsuccessfulAsError, fromJSON<Model>(this.serializer));
    }
    
    public putBulk(values: Array<Model>): Observable<Array<Model>> {
        return HttpClient.INSTANCE.call(`${this.url}/bulk`, HttpClient.INSTANCE.PUT, new Map([["Authorization", `Bearer ${this.token}`]]), HttpBody.json(values), undefined).pipe(unsuccessfulAsError, fromJSON<Array<Model>>([Array, this.serializer]));
    }
    
    public patch(id: UUIDFor<Model>, modification: Modification<Model>): Observable<Model> {
        return HttpClient.INSTANCE.call(`${this.url}/${id}`, HttpClient.INSTANCE.PATCH, new Map([["Authorization", `Bearer ${this.token}`]]), HttpBody.json(modification), undefined).pipe(unsuccessfulAsError, fromJSON<Model>(this.serializer));
    }
    
    public patchBulk(modification: MassModification<Model>): Observable<Array<Model>> {
        return HttpClient.INSTANCE.call(`${this.url}/bulk`, HttpClient.INSTANCE.PATCH, new Map([["Authorization", `Bearer ${this.token}`]]), HttpBody.json(modification), undefined).pipe(unsuccessfulAsError, fromJSON<Array<Model>>([Array, this.serializer]));
    }
    
    public _delete(id: UUIDFor<Model>): Observable<void> {
        return HttpClient.INSTANCE.call(`${this.url}/${id}`, HttpClient.INSTANCE.DELETE, new Map([["Authorization", `Bearer ${this.token}`]]), undefined, undefined).pipe(unsuccessfulAsError, switchMap(x => x.text().then(x => undefined)));
    }
    
    public deleteBulk(condition: Condition<Model>): Observable<void> {
        return HttpClient.INSTANCE.call(`${this.url}/bulk`, HttpClient.INSTANCE.DELETE, new Map([["Authorization", `Bearer ${this.token}`]]), HttpBody.json(condition), undefined).pipe(unsuccessfulAsError, switchMap(x => x.text().then(x => undefined)));
    }
}