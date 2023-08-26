// Package: com.lightningkite.ktordb.live
// Generated by Khrysalis - this file will be overwritten.
import KhrysalisRuntime
import RxSwiftPlus
import Foundation

public final class LiveFullReadModelApi<Model : HasId> : FullReadModelApi<Model> {
    private var _read: LiveReadModelApi<Model>
    override public var read: LiveReadModelApi<Model> { get { return self._read } set(value) { self._read = value } }
    private var _observe: ObserveModelApi<Model>
    override public var observe: ObserveModelApi<Model> { get { return self._observe } set(value) { self._observe = value } }
    public init(read: LiveReadModelApi<Model>, observe: ObserveModelApi<Model>) {
        self._read = read
        self._observe = observe
        super.init()
        //Necessary properties should be initialized now
    }
    
    
}
public final class LiveFullReadModelApiCompanion {
    public init() {
        //Necessary properties should be initialized now
    }
    public static let INSTANCE = LiveFullReadModelApiCompanion()
    
    public func create<Model : HasId>(root: String, multiplexSocketUrl: String, path: String, token: String?, headers: Dictionary<String, String> = dictionaryOf()) -> LiveFullReadModelApi<Model> {
        return LiveFullReadModelApi<Model>(read: LiveReadModelApiCompanion.INSTANCE.create(root: root, path: path, token: token, headers: headers), observe: LiveObserveModelApiCompanion.INSTANCE.create(multiplexUrl: multiplexSocketUrl, token: token, headers: headers, path: path));
    }
}
