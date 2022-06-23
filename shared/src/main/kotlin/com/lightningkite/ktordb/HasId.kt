@file:SharedCode
package com.lightningkite.ktordb

import com.lightningkite.khrysalis.*
import kotlinx.serialization.Serializable
import java.util.*

@SwiftProtocolExtends("Codable", "Hashable")
interface HasId<ID : Comparable<ID>> {
    val _id: ID
}

object HasIdFields {
    fun <T: HasId<ID>, ID: Comparable<ID>> _id() = T::_id
}

@SwiftProtocolExtends("Codable", "Hashable")
interface HasEmail {
    val email: String
}

object HasEmailFields {
    fun <T: HasEmail> email() = DataClassProperty<T, String>(
        name = "email",
        get = { it.email },
        set = { _, _ -> fatalError() },
        compare = compareBy { it.email }
    )
}
