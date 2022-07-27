// Package: com.lightningkite.ktordb.mock
// Generated by Khrysalis - this file will be overwritten.
import KhrysalisRuntime
import RxSwift
import Foundation

public final class MockReadModelApi<Model : HasId> : ReadModelApi<Model> where Model.ID == UUID {
    public var table: MockTable<Model>
    public init(table: MockTable<Model>) {
        self.table = table
        super.init()
        //Necessary properties should be initialized now
    }
    
    
    override public func list(_ query: Query<Model>) -> Single<Array<Model>> {
        return Single.just(Array(Array(self.table
                .asList()
                .filter({ (item) -> Bool in query.condition.invoke(on: item) })
            .sorted(by: getListComparator(query.orderBy) ?? compareBy(selector: { (it) in it._id })).dropFirst(query.skip)).prefix(query.limit)));
    }
    
    override public func get(id: UUIDFor<Model>) -> Single<Model> {
        return (self.table.getItem(id: id)).map { (it) in
            return Single.just(it)
        } ?? Single.error(ItemNotFound(message: "404 item with key \(id) not found"));
    }
}


