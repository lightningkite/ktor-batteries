import {Condition} from "./Condition";

type SubModification<T, K extends keyof T> = { K: Modification<T[K]> }
export type Modification<T> =
    { Chain: Array<Modification<T>> }
    | { IfNotNull: Modification<T> }
    | { Assign: T }
    | { CoerceAtMost: T }
    | { CoerceAtLeast: T }
    | { Increment: T }
    | { Multiply: T }
    | { AppendString: T }
    | { AppendList: T }
    | { AppendSet: T }
    | { Remove: Condition<any> }
    | { RemoveInstances: T }
    | { DropFirst: boolean }
    | { DropLast: boolean }
    | { PerElement: {
        condition: Condition<any>
        modification: Modification<any>
    } }
    | { Combine: T }
    | { ModifyByKey: Record<string, Modification<any>> }
    | { RemoveKeys: Array<string> }
    | SubModification<T, any>