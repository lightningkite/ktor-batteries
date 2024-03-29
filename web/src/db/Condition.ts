// Package: com.lightningkite.ktordb
// Generated by Khrysalis - this file will be overwritten.
import { EqualOverrideSet, ReifiedType, TProperty1, reflectiveGet, safeCompare, safeEq, setUpDataClass, tryCastClass } from '@lightningkite/khrysalis-runtime'

//! Declares com.lightningkite.ktordb.Condition
export class Condition<T extends any> {
    protected constructor() {
    }
    
    public hashCode(): number { throw undefined; }
    public equals(other: (any | null)): boolean { throw undefined; }
    
    public invoke(on: T): boolean { throw undefined; }
    public simplify(): Condition<T> {
        return this;
    }
    
    public and(other: Condition<T>): Condition.And<T> {
        return new Condition.And<T>([this, other]);
    }
    public or(other: Condition<T>): Condition.Or<T> {
        return new Condition.Or<T>([this, other]);
    }
    public not(): Condition.Not<T> {
        return new Condition.Not<T>(this);
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
}
export namespace Condition {
    //! Declares com.lightningkite.ktordb.Condition.Never
    export class Never<T extends any> extends Condition<T> {
        public constructor() {
            super();
        }
        
        public invoke(on: T): boolean {
            return false;
        }
        public hashCode(): number {
            return 0;
        }
        public equals(other: (any | null)): boolean {
            return (tryCastClass<Condition.Never<T>>(other, Condition.Never)) !== null;
        }
    }
}
export namespace Condition {
    //! Declares com.lightningkite.ktordb.Condition.Always
    export class Always<T extends any> extends Condition<T> {
        public constructor() {
            super();
        }
        
        public invoke(on: T): boolean {
            return true;
        }
        public hashCode(): number {
            return 1;
        }
        public equals(other: (any | null)): boolean {
            return (tryCastClass<Condition.Always<T>>(other, Condition.Always)) !== null;
        }
    }
}
export namespace Condition {
    //! Declares com.lightningkite.ktordb.Condition.And
    export class And<T extends any> extends Condition<T> {
        public constructor(public readonly conditions: Array<Condition<T>>) {
            super();
        }
        public static properties = ["conditions"]
        public static propertyTypes(T: ReifiedType) { return {conditions: [Array, [Condition, T]]} }
        copy: (values: Partial<And<T>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        
        public invoke(on: T): boolean {
            return this.conditions.every((it: Condition<T>): boolean => (it.invoke(on)));
        }
        public simplify(): Condition<T> {
            return this.conditions.length === 0 ? new Condition.Always<T>() : new Condition.And<T>([...new EqualOverrideSet(this.conditions)]);
        }
    }
    setUpDataClass(And)
}
export namespace Condition {
    //! Declares com.lightningkite.ktordb.Condition.Or
    export class Or<T extends any> extends Condition<T> {
        public constructor(public readonly conditions: Array<Condition<T>>) {
            super();
        }
        public static properties = ["conditions"]
        public static propertyTypes(T: ReifiedType) { return {conditions: [Array, [Condition, T]]} }
        copy: (values: Partial<Or<T>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        
        public invoke(on: T): boolean {
            return this.conditions.some((it: Condition<T>): boolean => (it.invoke(on)));
        }
        public simplify(): Condition<T> {
            return this.conditions.length === 0 ? new Condition.Never<T>() : new Condition.Or<T>([...new EqualOverrideSet(this.conditions)]);
        }
    }
    setUpDataClass(Or)
}
export namespace Condition {
    //! Declares com.lightningkite.ktordb.Condition.Not
    export class Not<T extends any> extends Condition<T> {
        public constructor(public readonly condition: Condition<T>) {
            super();
        }
        public static properties = ["condition"]
        public static propertyTypes(T: ReifiedType) { return {condition: [Condition, T]} }
        copy: (values: Partial<Not<T>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        
        public invoke(on: T): boolean {
            return (!this.condition.invoke(on));
        }
        public simplify(): Condition<T> {
            return ((tryCastClass<Condition.Not<T>>(this.condition, Condition.Not))?.condition ?? null) ?? this;
        }
    }
    setUpDataClass(Not)
}
export namespace Condition {
    //! Declares com.lightningkite.ktordb.Condition.Equal
    export class Equal<T extends any> extends Condition<T> {
        public constructor(public readonly value: T) {
            super();
        }
        public static properties = ["value"]
        public static propertyTypes(T: ReifiedType) { return {value: T} }
        copy: (values: Partial<Equal<T>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        public invoke(on: T): boolean {
            return safeEq(on, this.value);
    } }
    setUpDataClass(Equal)
}
export namespace Condition {
    //! Declares com.lightningkite.ktordb.Condition.NotEqual
    export class NotEqual<T extends any> extends Condition<T> {
        public constructor(public readonly value: T) {
            super();
        }
        public static properties = ["value"]
        public static propertyTypes(T: ReifiedType) { return {value: T} }
        copy: (values: Partial<NotEqual<T>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        public invoke(on: T): boolean {
            return !safeEq(on, this.value);
    } }
    setUpDataClass(NotEqual)
}
export namespace Condition {
    //! Declares com.lightningkite.ktordb.Condition.Inside
    export class Inside<T extends any> extends Condition<T> {
        public constructor(public readonly values: Array<T>) {
            super();
        }
        public static properties = ["values"]
        public static propertyTypes(T: ReifiedType) { return {values: [Array, T]} }
        copy: (values: Partial<Inside<T>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        public invoke(on: T): boolean {
            return this.values.some((x) => safeEq(on, x));
    } }
    setUpDataClass(Inside)
}
export namespace Condition {
    //! Declares com.lightningkite.ktordb.Condition.NotInside
    export class NotInside<T extends any> extends Condition<T> {
        public constructor(public readonly values: Array<T>) {
            super();
        }
        public static properties = ["values"]
        public static propertyTypes(T: ReifiedType) { return {values: [Array, T]} }
        copy: (values: Partial<NotInside<T>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        public invoke(on: T): boolean {
            return (!this.values.some((x) => safeEq(on, x)));
    } }
    setUpDataClass(NotInside)
}
export namespace Condition {
    //! Declares com.lightningkite.ktordb.Condition.GreaterThan
    export class GreaterThan<T extends any> extends Condition<T> {
        public constructor(public readonly value: T) {
            super();
        }
        public static properties = ["value"]
        public static propertyTypes(T: ReifiedType) { return {value: T} }
        copy: (values: Partial<GreaterThan<T>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        public invoke(on: T): boolean {
            return safeCompare(on, this.value) > 0;
    } }
    setUpDataClass(GreaterThan)
}
export namespace Condition {
    //! Declares com.lightningkite.ktordb.Condition.LessThan
    export class LessThan<T extends any> extends Condition<T> {
        public constructor(public readonly value: T) {
            super();
        }
        public static properties = ["value"]
        public static propertyTypes(T: ReifiedType) { return {value: T} }
        copy: (values: Partial<LessThan<T>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        public invoke(on: T): boolean {
            return safeCompare(on, this.value) < 0;
    } }
    setUpDataClass(LessThan)
}
export namespace Condition {
    //! Declares com.lightningkite.ktordb.Condition.GreaterThanOrEqual
    export class GreaterThanOrEqual<T extends any> extends Condition<T> {
        public constructor(public readonly value: T) {
            super();
        }
        public static properties = ["value"]
        public static propertyTypes(T: ReifiedType) { return {value: T} }
        copy: (values: Partial<GreaterThanOrEqual<T>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        public invoke(on: T): boolean {
            return safeCompare(on, this.value) >= 0;
    } }
    setUpDataClass(GreaterThanOrEqual)
}
export namespace Condition {
    //! Declares com.lightningkite.ktordb.Condition.LessThanOrEqual
    export class LessThanOrEqual<T extends any> extends Condition<T> {
        public constructor(public readonly value: T) {
            super();
        }
        public static properties = ["value"]
        public static propertyTypes(T: ReifiedType) { return {value: T} }
        copy: (values: Partial<LessThanOrEqual<T>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        public invoke(on: T): boolean {
            return safeCompare(on, this.value) <= 0;
    } }
    setUpDataClass(LessThanOrEqual)
}
export namespace Condition {
    //! Declares com.lightningkite.ktordb.Condition.StringContains
    export class StringContains extends Condition<string> {
        public constructor(public readonly value: string, public readonly ignoreCase: boolean = false) {
            super();
        }
        public static properties = ["value", "ignoreCase"]
        public static propertyTypes() { return {value: [String], ignoreCase: [Boolean]} }
        copy: (values: Partial<StringContains>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        public invoke(on: string): boolean {
            return (on.toLowerCase().indexOf(this.value.toLowerCase()) != -1);
    } }
    setUpDataClass(StringContains)
}
export namespace Condition {
    //! Declares com.lightningkite.ktordb.Condition.FullTextSearch
    export class FullTextSearch<T extends any> extends Condition<T> {
        public constructor(public readonly value: string, public readonly ignoreCase: boolean = false) {
            super();
        }
        public static properties = ["value", "ignoreCase"]
        public static propertyTypes(T: ReifiedType) { return {value: [String], ignoreCase: [Boolean]} }
        copy: (values: Partial<FullTextSearch<T>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        
        public invoke(on: T): boolean {
            throw "Not Implemented locally";
        }
    }
    setUpDataClass(FullTextSearch)
}
export namespace Condition {
    //! Declares com.lightningkite.ktordb.Condition.RegexMatches
    export class RegexMatches extends Condition<string> {
        public constructor(public readonly pattern: string, public readonly ignoreCase: boolean = false) {
            super();
            this.regex = new RegExp(this.pattern);
        }
        public static properties = ["pattern", "ignoreCase"]
        public static propertyTypes() { return {pattern: [String], ignoreCase: [Boolean]} }
        copy: (values: Partial<RegexMatches>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        
        public readonly regex: RegExp;
        public invoke(on: string): boolean {
            return this.regex.test(on);
        }
    }
    setUpDataClass(RegexMatches)
}
export namespace Condition {
    //! Declares com.lightningkite.ktordb.Condition.IntBitsClear
    export class IntBitsClear extends Condition<number> {
        public constructor(public readonly mask: number) {
            super();
        }
        public static properties = ["mask"]
        public static propertyTypes() { return {mask: [Number]} }
        copy: (values: Partial<IntBitsClear>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        public invoke(on: number): boolean {
            return (on & this.mask) === 0;
    } }
    setUpDataClass(IntBitsClear)
}
export namespace Condition {
    //! Declares com.lightningkite.ktordb.Condition.IntBitsSet
    export class IntBitsSet extends Condition<number> {
        public constructor(public readonly mask: number) {
            super();
        }
        public static properties = ["mask"]
        public static propertyTypes() { return {mask: [Number]} }
        copy: (values: Partial<IntBitsSet>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        public invoke(on: number): boolean {
            return (on & this.mask) === this.mask;
    } }
    setUpDataClass(IntBitsSet)
}
export namespace Condition {
    //! Declares com.lightningkite.ktordb.Condition.IntBitsAnyClear
    export class IntBitsAnyClear extends Condition<number> {
        public constructor(public readonly mask: number) {
            super();
        }
        public static properties = ["mask"]
        public static propertyTypes() { return {mask: [Number]} }
        copy: (values: Partial<IntBitsAnyClear>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        public invoke(on: number): boolean {
            return (on & this.mask) < this.mask;
    } }
    setUpDataClass(IntBitsAnyClear)
}
export namespace Condition {
    //! Declares com.lightningkite.ktordb.Condition.IntBitsAnySet
    export class IntBitsAnySet extends Condition<number> {
        public constructor(public readonly mask: number) {
            super();
        }
        public static properties = ["mask"]
        public static propertyTypes() { return {mask: [Number]} }
        copy: (values: Partial<IntBitsAnySet>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        public invoke(on: number): boolean {
            return (on & this.mask) > 0;
    } }
    setUpDataClass(IntBitsAnySet)
}
export namespace Condition {
    //! Declares com.lightningkite.ktordb.Condition.AllElements
    export class AllElements<E extends any> extends Condition<Array<E>> {
        public constructor(public readonly condition: Condition<E>) {
            super();
        }
        public static properties = ["condition"]
        public static propertyTypes(E: ReifiedType) { return {condition: [Condition, E]} }
        copy: (values: Partial<AllElements<E>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        public invoke(on: Array<E>): boolean {
            return on.every((it: E): boolean => (this.condition.invoke(it)));
    } }
    setUpDataClass(AllElements)
}
export namespace Condition {
    //! Declares com.lightningkite.ktordb.Condition.AnyElements
    export class AnyElements<E extends any> extends Condition<Array<E>> {
        public constructor(public readonly condition: Condition<E>) {
            super();
        }
        public static properties = ["condition"]
        public static propertyTypes(E: ReifiedType) { return {condition: [Condition, E]} }
        copy: (values: Partial<AnyElements<E>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        public invoke(on: Array<E>): boolean {
            return on.some((it: E): boolean => (this.condition.invoke(it)));
    } }
    setUpDataClass(AnyElements)
}
export namespace Condition {
    //! Declares com.lightningkite.ktordb.Condition.SizesEquals
    export class SizesEquals<E extends any> extends Condition<Array<E>> {
        public constructor(public readonly count: number) {
            super();
        }
        public static properties = ["count"]
        public static propertyTypes(E: ReifiedType) { return {count: [Number]} }
        copy: (values: Partial<SizesEquals<E>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        public invoke(on: Array<E>): boolean {
            return on.length === this.count;
    } }
    setUpDataClass(SizesEquals)
}
export namespace Condition {
    //! Declares com.lightningkite.ktordb.Condition.Exists
    export class Exists<V extends any> extends Condition<Map<string, V>> {
        public constructor(public readonly key: string) {
            super();
        }
        public static properties = ["key"]
        public static propertyTypes(V: ReifiedType) { return {key: [String]} }
        copy: (values: Partial<Exists<V>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        public invoke(on: Map<string, V>): boolean {
            return on.has(this.key);
    } }
    setUpDataClass(Exists)
}
export namespace Condition {
    //! Declares com.lightningkite.ktordb.Condition.OnKey
    export class OnKey<V extends any> extends Condition<Map<string, V>> {
        public constructor(public readonly key: string, public readonly condition: Condition<V>) {
            super();
        }
        public static properties = ["key", "condition"]
        public static propertyTypes(V: ReifiedType) { return {key: [String], condition: [Condition, V]} }
        copy: (values: Partial<OnKey<V>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        public invoke(on: Map<string, V>): boolean {
            return on.has(this.key) && this.condition.invoke((on.get(this.key) ?? null) as V);
    } }
    setUpDataClass(OnKey)
}
export namespace Condition {
    //! Declares com.lightningkite.ktordb.Condition.OnField
    export class OnField<K extends any, V extends any> extends Condition<K> {
        public constructor(public readonly key: TProperty1<K, V>, public readonly condition: Condition<V>) {
            super();
        }
        public static properties = ["key", "condition"]
        public static propertyTypes(K: ReifiedType, V: ReifiedType) { return {key: [String, K, V], condition: [Condition, V]} }
        copy: (values: Partial<OnField<K, V>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        public invoke(on: K): boolean {
            return this.condition.invoke(reflectiveGet(on, this.key));
    } }
    setUpDataClass(OnField)
}
export namespace Condition {
    //! Declares com.lightningkite.ktordb.Condition.IfNotNull
    export class IfNotNull<T extends any> extends Condition<(T | null)> {
        public constructor(public readonly condition: Condition<T>) {
            super();
        }
        public static properties = ["condition"]
        public static propertyTypes(T: ReifiedType) { return {condition: [Condition, T]} }
        copy: (values: Partial<IfNotNull<T>>) => this;
        equals: (other: any) => boolean;
        hashCode: () => number;
        public invoke(on: (T | null)): boolean {
            return on !== null && this.condition.invoke(on!);
    } }
    setUpDataClass(IfNotNull)
}
