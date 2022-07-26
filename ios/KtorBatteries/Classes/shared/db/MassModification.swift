// Package: com.lightningkite.lightningdb
// Generated by Khrysalis - this file will be overwritten.
import KhrysalisRuntime
import Foundation

public final class MassModification<T : Codable & Hashable> : CustomStringConvertible, Hashable, Codable {
    public var condition: Condition<T>
    public var modification: Modification<T>
    public init(condition: Condition<T>, modification: Modification<T>) {
        self.condition = condition
        self.modification = modification
        //Necessary properties should be initialized now
    }
    convenience required public init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        self.init(
            condition: try values.decode(Condition<T>.self, forKey: .condition),
            modification: try values.decode(Modification<T>.self, forKey: .modification)
        )
    }
    
    enum CodingKeys: String, CodingKey {
        case condition = "condition"
        case modification = "modification"
    }
    
    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(self.condition, forKey: .condition)
        try container.encode(self.modification, forKey: .modification)
    }
    
    public func hash(into hasher: inout Hasher) {
        hasher.combine(condition)
        hasher.combine(modification)
        
    }
    public static func == (lhs: MassModification, rhs: MassModification) -> Bool { return lhs.condition == rhs.condition && lhs.modification == rhs.modification }
    public var description: String { return "MassModification(condition=\(String(kotlin: self.condition)), modification=\(String(kotlin: self.modification)))" }
    public func copy(condition: Condition<T>? = nil, modification: Modification<T>? = nil) -> MassModification<T> { return MassModification(condition: condition ?? self.condition, modification: modification ?? self.modification) }
}

