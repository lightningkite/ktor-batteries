package com.lightningkite.lightningserver.db

import com.lightningkite.lightningserver.serialization.Serialization
import com.lightningkite.lightningserver.typed.*
import com.lightningkite.lightningdb.*
import com.lightningkite.lightningserver.ServerBuilder
import com.lightningkite.lightningserver.ServerRunner
import com.lightningkite.lightningserver.auth.AuthInfo
import com.lightningkite.lightningserver.core.LightningServerDsl
import com.lightningkite.lightningserver.core.ServerPath
import com.lightningkite.lightningserver.http.*
import com.lightningkite.lightningserver.serialization.parse
import com.lightningkite.lightningserver.serialization.serializerOrContextual
import kotlinx.coroutines.flow.toList
import kotlinx.html.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

data class AutoAdminSection<USER, T : HasId<*>>(
    val path: ServerPath,
    val type: KSerializer<T>,
    val authInfo: AuthInfo<USER>,
    val defaultItem: (USER) -> T,
    val getCollection: suspend ServerRunner.(USER) -> FieldCollection<T>
) {
    companion object {
        val known: MutableCollection<AutoAdminSection<*, *>> = ArrayList()
    }
}

/**
 * Creates the End point for the Admin Index, which will direct the user to each of the models available.
 */
@LightningServerDsl
fun ServerBuilder.Path.adminIndex() {
    get.handler {
        HttpResponse.html {
            head {}
            body {
                div {
                    for (section in AutoAdminSection.known) {
                        div {
                            a(href = section.path.toString()) {
                                +section.type.toString()
                            }
                        }
                    }
                }
            }
        }
    }
}


/**
 * Shortcut to create each of the endpoints required for the Auto Admin
 */
@LightningServerDsl
inline fun <reified USER, reified T : HasId<ID>, reified ID : Comparable<ID>> ServerBuilder.Path.adminPages(
    noinline defaultItem: (USER) -> T,
    noinline getCollection: suspend ServerRunner.(principal: USER) -> FieldCollection<T>
): Unit = adminPages(
    authInfo = AuthInfo(),
    serializer = serializerOrContextual(),
    idSerializer = serializerOrContextual(),
    defaultItem = defaultItem,
    getCollection = getCollection
)


/**
 * Shortcut to create each of the endpoints required for the Auto Admin
 */
@LightningServerDsl
fun <USER, T : HasId<ID>, ID : Comparable<ID>> ServerBuilder.Path.adminPages(
    authInfo: AuthInfo<USER>,
    serializer: KSerializer<T>,
    idSerializer: KSerializer<ID>,
    defaultItem: (USER) -> T,
    getCollection: suspend ServerRunner.(principal: USER) -> FieldCollection<T>
) {
    val name = serializer.descriptor.serialName.substringAfterLast('.')
    get("{id}/").handler {
        val secured = getCollection(authInfo.checker(server.authorizationMethod(this, it)))
        val item = secured.get(serialization.parseUrlPartOrBadRequest(it.parts["id"]!!, idSerializer))
        HttpResponse.html {
            head { includeFormScript() }
            body {
                form(method = FormMethod.post) {
                    id = "data-form"
                    insideHtmlForm(
                        serialization = serialization,
                        title = name ?: "Model",
                        jsEditorName = "editor",
                        serializer = serializer,
                        defaultValue = item,
                        collapsed = false
                    )
                    button {
                        +"Save"
                    }
                    a(href = "delete/") {
                        +"Delete"
                    }
                }
            }
        }
    }
    get("{id}/delete/").handler {
        HttpResponse.html {
            body {
                p { +"Are you sure?" }
                form {
                    button(formMethod = ButtonFormMethod.post) { +"Yes" }
                }
            }
        }
    }
    post("{id}/delete/").handler {
        getCollection(authInfo.checker(server.authorizationMethod(this, it))).deleteOneById(
            serialization.parseUrlPartOrBadRequest(it.parts["id"]!!, idSerializer)
        )
        HttpResponse.redirectToGet("../..")
    }
    post("{id}/").handler {
        val item: T = it.body!!.parse(this, serializer)
        getCollection(authInfo.checker(server.authorizationMethod(this, it))).replaceOneById(
            serialization.parseUrlPartOrBadRequest(it.parts["id"]!!, idSerializer), item
        )
        HttpResponse.redirectToGet("..")
    }
    get("create/").handler {
        val user = authInfo.checker(server.authorizationMethod(this, it))
        HttpResponse.html {
            head { includeFormScript() }
            body {
                form(method = FormMethod.post) {
                    id = "data-form"
                    insideHtmlForm<T>(
                        serialization = serialization,
                        title = name ?: "Model",
                        jsEditorName = "editor",
                        serializer = serializer,
                        defaultValue = defaultItem(user),
                        collapsed = false
                    )
                    button {
                        +"Save"
                    }
                }
            }
        }
    }
    post("create/").handler {
        val item: T = it.body!!.parse(this, serializer)
        getCollection(authInfo.checker(server.authorizationMethod(this, it))).insertOne(item)
        HttpResponse.redirectToGet("..")
    }
    get("/").handler {
        val secured = getCollection(authInfo.checker(server.authorizationMethod(this, it)))
        val items = secured.query(
            Query(
                condition = Condition.Always(),
                orderBy = listOf(),
                skip = it.queryParameter("skip")?.toIntOrNull() ?: 0,
                limit = it.queryParameter("limit")?.toIntOrNull() ?: 25,
            )
        ).toList()
        val propItems = items.map { serialization.properties.encodeToStringMap(serializer, it) }
        val keys = propItems.flatMap { it.keys }.distinct()
        HttpResponse.html {
            body {
                a(href = "create/") {
                    +"Create"
                }
                table {
                    tr {
                        for (key in keys) {
                            th { +key }
                        }
                    }
                    for (item in propItems) {
                        tr {
                            for (key in keys) {
                                td {
                                    if (key == "_id") {
                                        a(href = item["_id"]!! + "/") {
                                            +(item[key] ?: "-")
                                        }
                                    } else {
                                        +(item[key] ?: "-")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }.also {
        AutoAdminSection.known.add(
            AutoAdminSection(
                path = this.path,
                type = serializer,
                authInfo = authInfo,
                defaultItem = defaultItem,
                getCollection = getCollection,
            )
        )
    }
}
