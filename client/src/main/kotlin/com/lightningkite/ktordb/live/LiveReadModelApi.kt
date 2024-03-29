@file:SharedCode
package com.lightningkite.ktordb.live

import com.lightningkite.khrysalis.SharedCode
import com.lightningkite.ktordb.*
import com.lightningkite.ktordb.HasId
import com.lightningkite.ktordb.UUIDFor
import com.lightningkite.rx.okhttp.HttpClient
import com.lightningkite.rx.okhttp.defaultJsonMapper
import com.lightningkite.rx.okhttp.readJson
import com.lightningkite.rx.okhttp.toJsonRequestBody
import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.serializer
import okhttp3.Response
import java.util.*

class LiveReadModelApi<Model : HasId<UUID>>(
    val url: String,
    token: String?,
    headers: Map<String, String> = mapOf(),
    val serializer: KSerializer<Model>,
    val querySerializer: KSerializer<Query<Model>>,
) : ReadModelApi<Model>() {


    companion object {
        inline fun <reified Model : HasId<UUID>> create(
            root: String,
            path: String,
            token: String?,
            headers: Map<String, String> = mapOf(),
        ): LiveReadModelApi<Model> =
            LiveReadModelApi("$root$path", token, headers, defaultJsonMapper.serializersModule.serializer(), defaultJsonMapper.serializersModule.serializer())
    }

    private val authHeaders = token?.let { headers + mapOf("Authorization" to "Bearer $it") } ?: headers

    override fun list(query: Query<Model>): Single<List<Model>> = HttpClient.call(
        url = "$url/query",
        method = HttpClient.POST,
        headers = authHeaders,
        body = query.toJsonRequestBody(querySerializer),
    ).readJson(ListSerializer(serializer))


    override fun get(id: UUIDFor<Model>): Single<Model> = HttpClient.call(
        url = "$url/$id",
        method = HttpClient.GET,
        headers = authHeaders,
    ).readJson(serializer)

}

