// Package: com.lightningkite.ktordb
// Generated by Khrysalis - this file will be overwritten.
import { Comparator, ReifiedType, TProperty1, compareBy, setUpDataClass } from '@lightningkite/khrysalis-runtime'

//! Declares com.lightningkite.ktordb.SortPart
export class SortPart<T extends any> {
    public constructor(public readonly field: (keyof T & string), public readonly ascending: boolean = true) {
    }
    public static properties = ["field", "ascending"]
    public static propertyTypes(T: ReifiedType) { return {field: [String, T], ascending: [Boolean]} }
    copy: (values: Partial<SortPart<T>>) => this;
    equals: (other: any) => boolean;
    hashCode: () => number;
    
    public static constructorKProperty1comSortPartTAnyBoolean<T extends any>(field: (keyof T & string), ascending: boolean = true) {
        let result = new SortPart<T>(field, ascending);
        
        return result;
    }
}
setUpDataClass(SortPart)

//! Declares com.lightningkite.ktordb.comparator>kotlin.collections.Listcom.lightningkite.ktordb.SortPartcom.lightningkite.ktordb.comparator.T
export function xListComparatorGet<T extends any>(this_: Array<SortPart<T>>): (Comparator<T> | null) {
    if (this_.length === 0) { return null }
    return (a: T, b: T): number => {
        for (const part of this_) {
            const result = compareBy(part.field as keyof T)(a, b);
            if (!(result === 0)) { return part.ascending ? result : (-result) }
        }
        return 0;
    };
}
