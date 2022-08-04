// Package: com.lightningkite.ktordb.live
// Generated by Khrysalis - this file will be overwritten.
import KhrysalisRuntime
import RxSwift
import RxSwiftPlus
import Foundation

public final class LiveWriteModelApi<Model : HasId> : WriteModelApi<Model> {
    public var url: String
    public var serializer: Model.Type
    public init(url: String, token: String, headers: Dictionary<String, String>, serializer: Model.Type) {
        self.url = url
        self.serializer = serializer
        self.authHeaders = headers.plus(dictionaryOf(Pair("Authorization", "Bearer \(String(kotlin: token))")))
        super.init()
        //Necessary properties should be initialized now
    }
    
    
    
    
    private let authHeaders: Dictionary<String, String>
    
    override public func post(_ value: Model) -> Single<Model> {
        return HttpClient.INSTANCE.call(url: self.url, method: HttpClient.INSTANCE.POST, headers: self.authHeaders, body: value.toJsonRequestBody()).readJson(serializer: Model.self);
    }
    
    override public func postBulk(values: Array<Model>) -> Single<Array<Model>> {
        return HttpClient.INSTANCE.call(url: "\(String(kotlin: self.url))/bulk", method: HttpClient.INSTANCE.POST, headers: self.authHeaders, body: values.toJsonRequestBody()).readJson(serializer: Array<Model>.self);
    }
    
    override public func upsert(_ value: Model, id: UUIDFor<Model>) -> Single<Model> {
        return HttpClient.INSTANCE.call(url: "\(String(kotlin: self.url))/\(value._id)", method: HttpClient.INSTANCE.POST, headers: self.authHeaders, body: value.toJsonRequestBody()).readJson(serializer: Model.self);
    }
    
    override public func put(_ value: Model) -> Single<Model> {
        return HttpClient.INSTANCE.call(url: "\(String(kotlin: self.url))/\(value._id)", method: HttpClient.INSTANCE.PUT, headers: self.authHeaders, body: value.toJsonRequestBody()).readJson(serializer: Model.self);
    }
    
    override public func putBulk(values: Array<Model>) -> Single<Array<Model>> {
        return HttpClient.INSTANCE.call(url: "\(String(kotlin: self.url))/bulk", method: HttpClient.INSTANCE.PUT, headers: self.authHeaders, body: values.toJsonRequestBody()).readJson(serializer: Array<Model>.self);
    }
    
    override public func patch(id: UUIDFor<Model>, modification: Modification<Model>) -> Single<Model> {
        return HttpClient.INSTANCE.call(url: "\(String(kotlin: self.url))/\(id)", method: HttpClient.INSTANCE.PATCH, headers: self.authHeaders, body: modification.toJsonRequestBody()).readJson(serializer: Model.self);
    }
    
    override public func patchBulk(modification: MassModification<Model>) -> Single<Int> {
        return HttpClient.INSTANCE.call(url: "\(String(kotlin: self.url))/bulk", method: HttpClient.INSTANCE.PATCH, headers: self.authHeaders, body: modification.toJsonRequestBody())
            .flatMap { (it) -> Single<String> in it.readText() }
            .map { (it) -> Int in Int(it)! };
    }
    
    override public func delete(id: UUIDFor<Model>) -> Single<Void> {
        return HttpClient.INSTANCE.call(url: "\(String(kotlin: self.url))/\(id)", method: HttpClient.INSTANCE.DELETE, headers: self.authHeaders).discard();
    }
    
    override public func deleteBulk(_ condition: Condition<Model>) -> Single<Void> {
        return HttpClient.INSTANCE.call(url: "\(String(kotlin: self.url))/bulk", method: HttpClient.INSTANCE.DELETE, headers: self.authHeaders, body: condition.toJsonRequestBody()).discard();
    }
}
public final class LiveWriteModelApiCompanion {
    public init() {
        //Necessary properties should be initialized now
    }
    public static let INSTANCE = LiveWriteModelApiCompanion()
    
    public func create<Model : HasId>(root: String, path: String, token: String, headers: Dictionary<String, String> = dictionaryOf()) -> LiveWriteModelApi<Model> {
        return LiveWriteModelApi<Model>(url: "\(String(kotlin: root))\(String(kotlin: path))", token: token, headers: headers, serializer: Model.self);
    }
}
