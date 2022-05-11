"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Modification = void 0;
// Package: com.lightningkite.ktordb
// Generated by Khrysalis - this file will be overwritten.
const Condition_1 = require("./Condition");
const DataClassProperty_1 = require("./DataClassProperty");
const khrysalis_runtime_1 = require("@lightningkite/khrysalis-runtime");
const iter_tools_es_1 = require("iter-tools-es");
//! Declares com.lightningkite.ktordb.Modification
class Modification {
    constructor() {
    }
    hashCode() { throw undefined; }
    equals(other) { throw undefined; }
    invoke(on) { throw undefined; }
    invokeDefault() { throw undefined; }
    then(other) {
        return new Modification.Chain([this, other]);
    }
}
exports.Modification = Modification;
(function (Modification) {
    //! Declares com.lightningkite.ktordb.Modification.Chain
    class Chain extends Modification {
        constructor(modifications) {
            super();
            this.modifications = modifications;
        }
        static propertyTypes(T) { return { modifications: [Array, [Modification, T]] }; }
        invoke(on) {
            return (0, iter_tools_es_1.reduce)(on, (item, mod) => (mod.invoke(item)), this.modifications);
        }
        invokeDefault() {
            const on = this.modifications[0].invokeDefault();
            return (0, iter_tools_es_1.reduce)(on, (item, mod) => (mod.invoke(item)), this.modifications.slice(1));
        }
    }
    Chain.properties = ["modifications"];
    Modification.Chain = Chain;
    (0, khrysalis_runtime_1.setUpDataClass)(Chain);
})(Modification = exports.Modification || (exports.Modification = {}));
(function (Modification) {
    //! Declares com.lightningkite.ktordb.Modification.IfNotNull
    class IfNotNull extends Modification {
        constructor(modification) {
            super();
            this.modification = modification;
        }
        static propertyTypes(T) { return { modification: [Modification, T] }; }
        invoke(on) {
            return (() => {
                const temp6 = on;
                if (temp6 === null) {
                    return null;
                }
                return ((it) => (this.modification.invoke(it)))(temp6);
            })();
        }
        invokeDefault() {
            return null;
        }
    }
    IfNotNull.properties = ["modification"];
    Modification.IfNotNull = IfNotNull;
    (0, khrysalis_runtime_1.setUpDataClass)(IfNotNull);
})(Modification = exports.Modification || (exports.Modification = {}));
(function (Modification) {
    //! Declares com.lightningkite.ktordb.Modification.Assign
    class Assign extends Modification {
        constructor(value) {
            super();
            this.value = value;
        }
        static propertyTypes(T) { return { value: T }; }
        invoke(on) {
            return this.value;
        }
        invokeDefault() {
            return this.value;
        }
    }
    Assign.properties = ["value"];
    Modification.Assign = Assign;
    (0, khrysalis_runtime_1.setUpDataClass)(Assign);
})(Modification = exports.Modification || (exports.Modification = {}));
(function (Modification) {
    //! Declares com.lightningkite.ktordb.Modification.CoerceAtMost
    class CoerceAtMost extends Modification {
        constructor(value) {
            super();
            this.value = value;
        }
        static propertyTypes(T) { return { value: T }; }
        invoke(on) {
            return (0, khrysalis_runtime_1.cMin)(on, this.value);
        }
        invokeDefault() {
            return this.value;
        }
    }
    CoerceAtMost.properties = ["value"];
    Modification.CoerceAtMost = CoerceAtMost;
    (0, khrysalis_runtime_1.setUpDataClass)(CoerceAtMost);
})(Modification = exports.Modification || (exports.Modification = {}));
(function (Modification) {
    //! Declares com.lightningkite.ktordb.Modification.CoerceAtLeast
    class CoerceAtLeast extends Modification {
        constructor(value) {
            super();
            this.value = value;
        }
        static propertyTypes(T) { return { value: T }; }
        invoke(on) {
            return (0, khrysalis_runtime_1.cMax)(on, this.value);
        }
        invokeDefault() {
            return this.value;
        }
    }
    CoerceAtLeast.properties = ["value"];
    Modification.CoerceAtLeast = CoerceAtLeast;
    (0, khrysalis_runtime_1.setUpDataClass)(CoerceAtLeast);
})(Modification = exports.Modification || (exports.Modification = {}));
(function (Modification) {
    //! Declares com.lightningkite.ktordb.Modification.Increment
    class Increment extends Modification {
        constructor(by) {
            super();
            this.by = by;
        }
        static propertyTypes(T) { return { by: T }; }
        invoke(on) {
            return (on + this.by);
        }
        invokeDefault() {
            return this.by;
        }
    }
    Increment.properties = ["by"];
    Modification.Increment = Increment;
    (0, khrysalis_runtime_1.setUpDataClass)(Increment);
})(Modification = exports.Modification || (exports.Modification = {}));
(function (Modification) {
    //! Declares com.lightningkite.ktordb.Modification.Multiply
    class Multiply extends Modification {
        constructor(by) {
            super();
            this.by = by;
        }
        static propertyTypes(T) { return { by: T }; }
        invoke(on) {
            return (on * this.by);
        }
        invokeDefault() {
            return (() => {
                if (typeof (this.by) === "number") {
                    return 0;
                }
                else if (typeof (this.by) === "number") {
                    return 0;
                }
                else if (typeof (this.by) === "number") {
                    return 0;
                }
                else if (typeof (this.by) === "number") {
                    return 0;
                }
                else if (typeof (this.by) === "number") {
                    return 0;
                }
                else if (typeof (this.by) === "number") {
                    return 0.0;
                }
                else {
                    throw undefined;
                }
            })();
        }
    }
    Multiply.properties = ["by"];
    Modification.Multiply = Multiply;
    (0, khrysalis_runtime_1.setUpDataClass)(Multiply);
})(Modification = exports.Modification || (exports.Modification = {}));
(function (Modification) {
    //! Declares com.lightningkite.ktordb.Modification.AppendString
    class AppendString extends Modification {
        constructor(value) {
            super();
            this.value = value;
        }
        static propertyTypes() { return { value: [String] }; }
        invoke(on) {
            return on + this.value;
        }
        invokeDefault() {
            return this.value;
        }
    }
    AppendString.properties = ["value"];
    Modification.AppendString = AppendString;
    (0, khrysalis_runtime_1.setUpDataClass)(AppendString);
})(Modification = exports.Modification || (exports.Modification = {}));
(function (Modification) {
    //! Declares com.lightningkite.ktordb.Modification.AppendList
    class AppendList extends Modification {
        constructor(items) {
            super();
            this.items = items;
        }
        static propertyTypes(T) { return { items: [Array, T] }; }
        invoke(on) {
            return on.concat(this.items);
        }
        invokeDefault() {
            return this.items;
        }
    }
    AppendList.properties = ["items"];
    Modification.AppendList = AppendList;
    (0, khrysalis_runtime_1.setUpDataClass)(AppendList);
})(Modification = exports.Modification || (exports.Modification = {}));
(function (Modification) {
    //! Declares com.lightningkite.ktordb.Modification.AppendSet
    class AppendSet extends Modification {
        constructor(items) {
            super();
            this.items = items;
        }
        static propertyTypes(T) { return { items: [Array, T] }; }
        invoke(on) {
            return (0, iter_tools_es_1.toArray)(new khrysalis_runtime_1.EqualOverrideSet((on.concat(this.items))));
        }
        invokeDefault() {
            return this.items;
        }
    }
    AppendSet.properties = ["items"];
    Modification.AppendSet = AppendSet;
    (0, khrysalis_runtime_1.setUpDataClass)(AppendSet);
})(Modification = exports.Modification || (exports.Modification = {}));
(function (Modification) {
    //! Declares com.lightningkite.ktordb.Modification.Remove
    class Remove extends Modification {
        constructor(condition) {
            super();
            this.condition = condition;
        }
        static propertyTypes(T) { return { condition: [Condition_1.Condition, T] }; }
        invoke(on) {
            return on.filter((it) => ((!this.condition.invoke(it))));
        }
        invokeDefault() {
            return [];
        }
    }
    Remove.properties = ["condition"];
    Modification.Remove = Remove;
    (0, khrysalis_runtime_1.setUpDataClass)(Remove);
})(Modification = exports.Modification || (exports.Modification = {}));
(function (Modification) {
    //! Declares com.lightningkite.ktordb.Modification.RemoveInstances
    class RemoveInstances extends Modification {
        constructor(items) {
            super();
            this.items = items;
        }
        static propertyTypes(T) { return { items: [Array, T] }; }
        invoke(on) {
            return (0, khrysalis_runtime_1.xIterableMinusMultiple)(on, this.items);
        }
        invokeDefault() {
            return [];
        }
    }
    RemoveInstances.properties = ["items"];
    Modification.RemoveInstances = RemoveInstances;
    (0, khrysalis_runtime_1.setUpDataClass)(RemoveInstances);
})(Modification = exports.Modification || (exports.Modification = {}));
(function (Modification) {
    //! Declares com.lightningkite.ktordb.Modification.DropFirst
    class DropFirst extends Modification {
        constructor() {
            super();
        }
        invoke(on) {
            return on.slice(1);
        }
        invokeDefault() {
            return [];
        }
        hashCode() {
            return 1;
        }
        equals(other) {
            return ((0, khrysalis_runtime_1.tryCastClass)(other, Modification.DropFirst)) !== null;
        }
    }
    Modification.DropFirst = DropFirst;
})(Modification = exports.Modification || (exports.Modification = {}));
(function (Modification) {
    //! Declares com.lightningkite.ktordb.Modification.DropLast
    class DropLast extends Modification {
        constructor() {
            super();
        }
        invoke(on) {
            return on.slice(0, -1);
        }
        invokeDefault() {
            return [];
        }
        hashCode() {
            return 1;
        }
        equals(other) {
            return ((0, khrysalis_runtime_1.tryCastClass)(other, Modification.DropLast)) !== null;
        }
    }
    Modification.DropLast = DropLast;
})(Modification = exports.Modification || (exports.Modification = {}));
(function (Modification) {
    //! Declares com.lightningkite.ktordb.Modification.PerElement
    class PerElement extends Modification {
        constructor(condition, modification) {
            super();
            this.condition = condition;
            this.modification = modification;
        }
        static propertyTypes(T) { return { condition: [Condition_1.Condition, T], modification: [Modification, T] }; }
        invoke(on) {
            return on.map((it) => (this.condition.invoke(it) ? this.modification.invoke(it) : it));
        }
        invokeDefault() {
            return [];
        }
    }
    PerElement.properties = ["condition", "modification"];
    Modification.PerElement = PerElement;
    (0, khrysalis_runtime_1.setUpDataClass)(PerElement);
})(Modification = exports.Modification || (exports.Modification = {}));
(function (Modification) {
    //! Declares com.lightningkite.ktordb.Modification.Combine
    class Combine extends Modification {
        constructor(map) {
            super();
            this.map = map;
        }
        static propertyTypes(T) { return { map: [Map, [String], T] }; }
        invoke(on) {
            return new Map([...on, ...this.map]);
        }
        invokeDefault() {
            return this.map;
        }
    }
    Combine.properties = ["map"];
    Modification.Combine = Combine;
    (0, khrysalis_runtime_1.setUpDataClass)(Combine);
})(Modification = exports.Modification || (exports.Modification = {}));
(function (Modification) {
    //! Declares com.lightningkite.ktordb.Modification.ModifyByKey
    class ModifyByKey extends Modification {
        constructor(map) {
            super();
            this.map = map;
        }
        static propertyTypes(T) { return { map: [Map, [String], [Modification, T]] }; }
        invoke(on) {
            return new Map([...on, ...new Map((0, iter_tools_es_1.map)(x => [x[0], ((it) => {
                        var _a;
                        return (((_a = (() => {
                            var _a;
                            const temp25 = ((_a = on.get(it[0])) !== null && _a !== void 0 ? _a : null);
                            if (temp25 === null) {
                                return null;
                            }
                            return ((e) => (it[1].invoke(e)))(temp25);
                        })()) !== null && _a !== void 0 ? _a : it[1].invokeDefault()));
                    })(x)], this.map.entries()))]);
        }
        invokeDefault() {
            return new Map((0, iter_tools_es_1.map)(x => [x[0], ((it) => (it[1].invokeDefault()))(x)], this.map.entries()));
        }
    }
    ModifyByKey.properties = ["map"];
    Modification.ModifyByKey = ModifyByKey;
    (0, khrysalis_runtime_1.setUpDataClass)(ModifyByKey);
})(Modification = exports.Modification || (exports.Modification = {}));
(function (Modification) {
    //! Declares com.lightningkite.ktordb.Modification.RemoveKeys
    class RemoveKeys extends Modification {
        constructor(fields) {
            super();
            this.fields = fields;
        }
        static propertyTypes(T) { return { fields: [Set, [String]] }; }
        invoke(on) {
            return new Map((0, iter_tools_es_1.filter)(x => ((it) => (!(this.fields.has(it))))(x[0]), on.entries()));
        }
        invokeDefault() {
            return new Map([]);
        }
    }
    RemoveKeys.properties = ["fields"];
    Modification.RemoveKeys = RemoveKeys;
    (0, khrysalis_runtime_1.setUpDataClass)(RemoveKeys);
})(Modification = exports.Modification || (exports.Modification = {}));
(function (Modification) {
    //! Declares com.lightningkite.ktordb.Modification.OnField
    class OnField extends Modification {
        constructor(key, modification) {
            super();
            this.key = key;
            this.modification = modification;
        }
        static propertyTypes(K, V) { return { key: [String, K, V], modification: [Modification, V] }; }
        invoke(on) {
            return (0, DataClassProperty_1.keySet)(on, this.key, this.modification.invoke((0, DataClassProperty_1.keyGet)(on, this.key)));
        }
        invokeDefault() {
            throw "Cannot mutate a field that doesn't exist";
        }
    }
    OnField.properties = ["key", "modification"];
    Modification.OnField = OnField;
    (0, khrysalis_runtime_1.setUpDataClass)(OnField);
})(Modification = exports.Modification || (exports.Modification = {}));
//# sourceMappingURL=Modification.js.map