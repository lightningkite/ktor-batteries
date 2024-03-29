// Package: com.lightningkite.ktordb
// Generated by Khrysalis - this file will be overwritten.
import { HasId } from './db/HasId'
import { ReifiedType, setUpDataClass } from '@lightningkite/khrysalis-runtime'

//! Declares com.lightningkite.ktordb.SignalData
export class SignalData<Model extends HasId<string>> {
    public constructor(public readonly item: Model, public readonly created: boolean, public readonly deleted: boolean) {
    }
    public static properties = ["item", "created", "deleted"]
    public static propertyTypes(Model: ReifiedType) { return {item: Model, created: [Boolean], deleted: [Boolean]} }
    copy: (values: Partial<SignalData<Model>>) => this;
    equals: (other: any) => boolean;
    hashCode: () => number;
}
setUpDataClass(SignalData)