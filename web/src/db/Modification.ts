// Package: com.lightningkite.ktordb
// Generated by Khrysalis - this file will be overwritten.
import { Condition } from './Condition'
import { keySet } from './TProperty1Extensions'
import { EqualOverrideSet, ReifiedType, TProperty1, cMax, cMin, reflectiveGet, setUpDataClass, tryCastClass, xIterableMinusMultiple } from '@lightningkite/khrysalis-runtime'
import { filter, map, reduce, toArray } from 'iter-tools-es'

//! Declares com.lightningkite.ktordb.Modification
export class Modification<T extends any> {
    protected constructor() {
    }
    
    public hashCode(): number { throw undefined; }
    public equals(other: (any | null)): boolean { throw undefined; }
    public invoke(on: T): T { throw undefined; }
    public invokeDefault(): T { throw undefined; }
    
    public then(other: Modification<T>): Modification.Chain<T> {
        return new Modification.Chain<T>([this, other]);
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
}
export namespace Modification {
    //! Declares com.lightningkite.ktordb.Modification.Chain
    export class Chain<T extends any> extends Modification<T> {
        public constructor(public readonly modifications: Array<Modification<T>>) {
            super();
        }
        public static properties = ["modifications"]
        public static propertyTypes(T: ReifiedType) { return {modifications: [Array, [Modification, T]]} }
        copy: (values: Partial<Chain<T>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        
        public invoke(on: T): T {
            return reduce(on, (item: T, mod: Modification<T>): T => (mod.invoke(item)), this.modifications);
        }
        public invokeDefault(): T {
            const on = this.modifications[0].invokeDefault();
            return reduce(on, (item: T, mod: Modification<T>): T => (mod.invoke(item)), this.modifications.slice(1));
        }
    }
    setUpDataClass(Chain)
}
export namespace Modification {
    //! Declares com.lightningkite.ktordb.Modification.IfNotNull
    export class IfNotNull<T extends any> extends Modification<(T | null)> {
        public constructor(public readonly modification: Modification<T>) {
            super();
        }
        public static properties = ["modification"]
        public static propertyTypes(T: ReifiedType) { return {modification: [Modification, T]} }
        copy: (values: Partial<IfNotNull<T>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        
        public invoke(on: (T | null)): (T | null) {
            return ((): (T | null) => {
                const temp6 = on;
                if(temp6 === null) { return null }
                return ((it: T): T => (this.modification.invoke(it)))(temp6)
            })();
        }
        public invokeDefault(): (T | null) {
            return null;
        }
    }
    setUpDataClass(IfNotNull)
}
export namespace Modification {
    //! Declares com.lightningkite.ktordb.Modification.Assign
    export class Assign<T extends any> extends Modification<T> {
        public constructor(public readonly value: T) {
            super();
        }
        public static properties = ["value"]
        public static propertyTypes(T: ReifiedType) { return {value: T} }
        copy: (values: Partial<Assign<T>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        
        public invoke(on: T): T {
            return this.value;
        }
        public invokeDefault(): T {
            return this.value;
        }
    }
    setUpDataClass(Assign)
}
export namespace Modification {
    //! Declares com.lightningkite.ktordb.Modification.CoerceAtMost
    export class CoerceAtMost<T extends any> extends Modification<T> {
        public constructor(public readonly value: T) {
            super();
        }
        public static properties = ["value"]
        public static propertyTypes(T: ReifiedType) { return {value: T} }
        copy: (values: Partial<CoerceAtMost<T>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        
        public invoke(on: T): T {
            return cMin(on, this.value);
        }
        public invokeDefault(): T {
            return this.value;
        }
    }
    setUpDataClass(CoerceAtMost)
}
export namespace Modification {
    //! Declares com.lightningkite.ktordb.Modification.CoerceAtLeast
    export class CoerceAtLeast<T extends any> extends Modification<T> {
        public constructor(public readonly value: T) {
            super();
        }
        public static properties = ["value"]
        public static propertyTypes(T: ReifiedType) { return {value: T} }
        copy: (values: Partial<CoerceAtLeast<T>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        
        public invoke(on: T): T {
            return cMax(on, this.value);
        }
        public invokeDefault(): T {
            return this.value;
        }
    }
    setUpDataClass(CoerceAtLeast)
}
export namespace Modification {
    //! Declares com.lightningkite.ktordb.Modification.Increment
    export class Increment<T extends number> extends Modification<T> {
        public constructor(public readonly by: T) {
            super();
        }
        public static properties = ["by"]
        public static propertyTypes(T: ReifiedType) { return {by: T} }
        copy: (values: Partial<Increment<T>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        
        public invoke(on: T): T {
            return (on + this.by) as T;
        }
        public invokeDefault(): T {
            return this.by;
        }
    }
    setUpDataClass(Increment)
}
export namespace Modification {
    //! Declares com.lightningkite.ktordb.Modification.Multiply
    export class Multiply<T extends number> extends Modification<T> {
        public constructor(public readonly by: T) {
            super();
        }
        public static properties = ["by"]
        public static propertyTypes(T: ReifiedType) { return {by: T} }
        copy: (values: Partial<Multiply<T>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        
        public invoke(on: T): T {
            return (on * this.by) as T;
        }
        public invokeDefault(): T {
            return ((): T => {
                if (typeof (this.by) === "number") {
                    return 0 as T
                } else if (typeof (this.by) === "number") {
                    return 0 as T
                } else if (typeof (this.by) === "number") {
                    return 0 as T
                } else if (typeof (this.by) === "number") {
                    return 0 as T
                } else if (typeof (this.by) === "number") {
                    return 0 as T
                } else if (typeof (this.by) === "number") {
                    return 0.0 as T
                } else  {
                    throw undefined
                }
            })();
        }
    }
    setUpDataClass(Multiply)
}
export namespace Modification {
    //! Declares com.lightningkite.ktordb.Modification.AppendString
    export class AppendString extends Modification<string> {
        public constructor(public readonly value: string) {
            super();
        }
        public static properties = ["value"]
        public static propertyTypes() { return {value: [String]} }
        copy: (values: Partial<AppendString>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        
        public invoke(on: string): string {
            return on + this.value;
        }
        public invokeDefault(): string {
            return this.value;
        }
    }
    setUpDataClass(AppendString)
}
export namespace Modification {
    //! Declares com.lightningkite.ktordb.Modification.AppendList
    export class AppendList<T extends any> extends Modification<Array<T>> {
        public constructor(public readonly items: Array<T>) {
            super();
        }
        public static properties = ["items"]
        public static propertyTypes(T: ReifiedType) { return {items: [Array, T]} }
        copy: (values: Partial<AppendList<T>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        
        public invoke(on: Array<T>): Array<T> {
            return on.concat(this.items);
        }
        public invokeDefault(): Array<T> {
            return this.items;
        }
    }
    setUpDataClass(AppendList)
}
export namespace Modification {
    //! Declares com.lightningkite.ktordb.Modification.AppendSet
    export class AppendSet<T extends any> extends Modification<Array<T>> {
        public constructor(public readonly items: Array<T>) {
            super();
        }
        public static properties = ["items"]
        public static propertyTypes(T: ReifiedType) { return {items: [Array, T]} }
        copy: (values: Partial<AppendSet<T>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        
        public invoke(on: Array<T>): Array<T> {
            return toArray(new EqualOverrideSet((on.concat(this.items))));
        }
        public invokeDefault(): Array<T> {
            return this.items;
        }
    }
    setUpDataClass(AppendSet)
}
export namespace Modification {
    //! Declares com.lightningkite.ktordb.Modification.Remove
    export class Remove<T extends any> extends Modification<Array<T>> {
        public constructor(public readonly condition: Condition<T>) {
            super();
        }
        public static properties = ["condition"]
        public static propertyTypes(T: ReifiedType) { return {condition: [Condition, T]} }
        copy: (values: Partial<Remove<T>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        
        public invoke(on: Array<T>): Array<T> {
            return on.filter((it: T): boolean => ((!this.condition.invoke(it))));
        }
        public invokeDefault(): Array<T> {
            return [];
        }
    }
    setUpDataClass(Remove)
}
export namespace Modification {
    //! Declares com.lightningkite.ktordb.Modification.RemoveInstances
    export class RemoveInstances<T extends any> extends Modification<Array<T>> {
        public constructor(public readonly items: Array<T>) {
            super();
        }
        public static properties = ["items"]
        public static propertyTypes(T: ReifiedType) { return {items: [Array, T]} }
        copy: (values: Partial<RemoveInstances<T>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        
        public invoke(on: Array<T>): Array<T> {
            return xIterableMinusMultiple(on, this.items);
        }
        public invokeDefault(): Array<T> {
            return [];
        }
    }
    setUpDataClass(RemoveInstances)
}
export namespace Modification {
    //! Declares com.lightningkite.ktordb.Modification.DropFirst
    export class DropFirst<T extends any> extends Modification<Array<T>> {
        public constructor() {
            super();
        }
        
        public invoke(on: Array<T>): Array<T> {
            return on.slice(1);
        }
        public invokeDefault(): Array<T> {
            return [];
        }
        public hashCode(): number {
            return 1;
        }
        public equals(other: (any | null)): boolean {
            return (tryCastClass<Modification.DropFirst<T>>(other, Modification.DropFirst)) !== null;
        }
    }
}
export namespace Modification {
    //! Declares com.lightningkite.ktordb.Modification.DropLast
    export class DropLast<T extends any> extends Modification<Array<T>> {
        public constructor() {
            super();
        }
        
        public invoke(on: Array<T>): Array<T> {
            return on.slice(0, -1);
        }
        public invokeDefault(): Array<T> {
            return [];
        }
        public hashCode(): number {
            return 1;
        }
        public equals(other: (any | null)): boolean {
            return (tryCastClass<Modification.DropLast<T>>(other, Modification.DropLast)) !== null;
        }
    }
}
export namespace Modification {
    //! Declares com.lightningkite.ktordb.Modification.PerElement
    export class PerElement<T extends any> extends Modification<Array<T>> {
        public constructor(public readonly condition: Condition<T>, public readonly modification: Modification<T>) {
            super();
        }
        public static properties = ["condition", "modification"]
        public static propertyTypes(T: ReifiedType) { return {condition: [Condition, T], modification: [Modification, T]} }
        copy: (values: Partial<PerElement<T>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        
        public invoke(on: Array<T>): Array<T> {
            return on.map((it: T): T => (this.condition.invoke(it) ? this.modification.invoke(it) : it));
        }
        public invokeDefault(): Array<T> {
            return [];
        }
    }
    setUpDataClass(PerElement)
}
export namespace Modification {
    //! Declares com.lightningkite.ktordb.Modification.Combine
    export class Combine<T extends any> extends Modification<Map<string, T>> {
        public constructor(public readonly map: Map<string, T>) {
            super();
        }
        public static properties = ["map"]
        public static propertyTypes(T: ReifiedType) { return {map: [Map, [String], T]} }
        copy: (values: Partial<Combine<T>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        
        public invoke(on: Map<string, T>): Map<string, T> {
            return new Map([...on, ...this.map]);
        }
        public invokeDefault(): Map<string, T> {
            return this.map;
        }
    }
    setUpDataClass(Combine)
}
export namespace Modification {
    //! Declares com.lightningkite.ktordb.Modification.ModifyByKey
    export class ModifyByKey<T extends any> extends Modification<Map<string, T>> {
        public constructor(public readonly map: Map<string, Modification<T>>) {
            super();
        }
        public static properties = ["map"]
        public static propertyTypes(T: ReifiedType) { return {map: [Map, [String], [Modification, T]]} }
        copy: (values: Partial<ModifyByKey<T>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        
        public invoke(on: Map<string, T>): Map<string, T> {
            return new Map([...on, ...new Map(map(x => [x[0], ((it: [string, Modification<T>]): T => ((((): (T | null) => {
                const temp25 = (on.get(it[0]) ?? null);
                if(temp25 === null) { return null }
                return ((e: T): T => (it[1].invoke(e)))(temp25)
            })() ?? it[1].invokeDefault())))(x)], this.map.entries()))]);
        }
        public invokeDefault(): Map<string, T> {
            return new Map(map(x => [x[0], ((it: [string, Modification<T>]): T => (it[1].invokeDefault()))(x)], this.map.entries()));
        }
    }
    setUpDataClass(ModifyByKey)
}
export namespace Modification {
    //! Declares com.lightningkite.ktordb.Modification.RemoveKeys
    export class RemoveKeys<T extends any> extends Modification<Map<string, T>> {
        public constructor(public readonly fields: Set<string>) {
            super();
        }
        public static properties = ["fields"]
        public static propertyTypes(T: ReifiedType) { return {fields: [Set, [String]]} }
        copy: (values: Partial<RemoveKeys<T>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        
        public invoke(on: Map<string, T>): Map<string, T> {
            return new Map(filter(x => ((it: string): boolean => (!(this.fields.has(it))))(x[0]), on.entries()));
        }
        public invokeDefault(): Map<string, T> {
            return new Map([]);
        }
    }
    setUpDataClass(RemoveKeys)
}
export namespace Modification {
    //! Declares com.lightningkite.ktordb.Modification.OnField
    export class OnField<K extends any, V extends any> extends Modification<K> {
        public constructor(public readonly key: TProperty1<K, V>, public readonly modification: Modification<V>) {
            super();
        }
        public static properties = ["key", "modification"]
        public static propertyTypes(K: ReifiedType, V: ReifiedType) { return {key: [String, K, V], modification: [Modification, V]} }
        copy: (values: Partial<OnField<K, V>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        
        public invoke(on: K): K {
            return keySet(on, this.key, this.modification.invoke(reflectiveGet(on, this.key)));
        }
        public invokeDefault(): K {
            throw "Cannot mutate a field that doesn't exist";
        }
    }
    setUpDataClass(OnField)
}
