// Package: com.lightningkite.ktordb.live
// Generated by Khrysalis - this file will be overwritten.
import KhrysalisRuntime
import RxSwift
import RxSwiftPlus
import Foundation

public class LiveReadModelApi<Model : HasId> : ReadModelApi<Model> {
    public var url: String
    public var token: String
    public var serializer: Model.Type
    public init(url: String, token: String, serializer: Model.Type) {
        self.url = url
        self.token = token
        self.serializer = serializer
        super.init()
        //Necessary properties should be initialized now
    }
    
    
    override public func list(_ query: Query<Model>) -> Single<Array<Model>> {
        return HttpClient.INSTANCE.call(url: "\(String(kotlin: self.url))/query", method: HttpClient.INSTANCE.POST, headers: dictionaryOf(Pair("Authorization", "Bearer \(String(kotlin: self.token))")), body: query.toJsonRequestBody()).readJson(serializer: Array<Model>.self);
    }
    
    
    override public func get(id: UUIDFor<Model>) -> Single<Model> {
        return HttpClient.INSTANCE.call(url: "\(String(kotlin: self.url))/\(id)", method: HttpClient.INSTANCE.GET, headers: dictionaryOf(Pair("Authorization", "Bearer \(String(kotlin: self.token))"))).readJson(serializer: Model.self);
    }
    
}

