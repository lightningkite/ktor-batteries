// Package: com.lightningkite.ktordb
// Generated by Khrysalis - this file will be overwritten.
import KhrysalisRuntime
import Foundation

public final class MultiplexMessage : CustomStringConvertible, Hashable, Codable {
    public var channel: String
    public var path: String?
    public var start: Bool
    public var end: Bool
    public var data: String?
    public var error: String?
    public init(channel: String, path: String? = nil, start: Bool = false, end: Bool = false, data: String? = nil, error: String? = nil) {
        self.channel = channel
        self.path = path
        self.start = start
        self.end = end
        self.data = data
        self.error = error
        //Necessary properties should be initialized now
    }
    convenience required public init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        self.init(
            channel: try values.decode(String.self, forKey: .channel),
            path: values.contains(.path) ? try values.decode(String?.self, forKey: .path) : nil,
            start: values.contains(.start) ? try values.decode(Bool.self, forKey: .start) : false,
            end: values.contains(.end) ? try values.decode(Bool.self, forKey: .end) : false,
            data: values.contains(.data) ? try values.decode(String?.self, forKey: .data) : nil,
            error: values.contains(.error) ? try values.decode(String?.self, forKey: .error) : nil
        )
    }
    
    enum CodingKeys: String, CodingKey {
        case channel = "channel"
        case path = "path"
        case start = "start"
        case end = "end"
        case data = "data"
        case error = "error"
    }
    
    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(self.channel, forKey: .channel)
        try container.encodeIfPresent(self.path, forKey: .path)
        try container.encode(self.start, forKey: .start)
        try container.encode(self.end, forKey: .end)
        try container.encodeIfPresent(self.data, forKey: .data)
        try container.encodeIfPresent(self.error, forKey: .error)
    }
    
    public func hash(into hasher: inout Hasher) {
        hasher.combine(channel)
        hasher.combine(path)
        hasher.combine(start)
        hasher.combine(end)
        hasher.combine(data)
        hasher.combine(error)
        
    }
    public static func == (lhs: MultiplexMessage, rhs: MultiplexMessage) -> Bool { return lhs.channel == rhs.channel && lhs.path == rhs.path && lhs.start == rhs.start && lhs.end == rhs.end && lhs.data == rhs.data && lhs.error == rhs.error }
    public var description: String { return "MultiplexMessage(channel=\(String(kotlin: self.channel)), path=\(String(kotlin: self.path)), start=\(String(kotlin: self.start)), end=\(String(kotlin: self.end)), data=\(String(kotlin: self.data)), error=\(String(kotlin: self.error)))" }
    public func copy(channel: String? = nil, path: String?? = .some(nil), start: Bool? = nil, end: Bool? = nil, data: String?? = .some(nil), error: String?? = .some(nil)) -> MultiplexMessage { return MultiplexMessage(channel: channel ?? self.channel, path: invertOptional(path) ?? self.path, start: start ?? self.start, end: end ?? self.end, data: invertOptional(data) ?? self.data, error: invertOptional(error) ?? self.error) }
}
