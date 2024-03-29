package com.lightningkite.ktorbatteries.routes

import io.ktor.http.*
import io.ktor.server.routing.*

private val docNames = HashMap<Route, String?>()
public var Route.docName: String?
    get() = docNames[this]
    set(value) { docNames[this] = value }

private val Route.recursiveChildren: Sequence<Route>
    get() = sequenceOf(this) + this.children.asSequence().flatMap { it.recursiveChildren }

private fun Route.commonAncestor(with: Route): Route? {
    val depthA = generateSequence(this) { it.parent }.count()
    val depthB = generateSequence(with) { it.parent }.count()
    var a = this
    var b = with
    if (depthA > depthB) {
        repeat(depthA - depthB) { a = a.parent ?: return null }
    } else {
        repeat(depthB - depthA) { b = b.parent ?: return null }
    }
    while (a !== b) {
        a = a.parent ?: return null
        b = b.parent ?: return null
    }
    return a
}

fun Route.pathRelativeTo(base: Route): String? {
    var pathBeforeAncestor = ""
    var pathAfterAncestor = ""
    val depthA = generateSequence(base) { it.parent }.count()
    val depthB = generateSequence(this) { it.parent }.count()
    var a = base
    var b = this
    if (depthA > depthB) {
        repeat(depthA - depthB) {
            a = a.parent ?: return null
            if (a.selector.segment.isNotBlank()) {
                pathBeforeAncestor += "../"
            }
        }
    } else {
        repeat(depthB - depthA) {
            pathAfterAncestor = b.selector.segment + pathAfterAncestor
            b = b.parent ?: return null
        }
    }
    while (a !== b) {
        if (a.selector.segment.isNotBlank()) {
            pathBeforeAncestor += "../"
        }
        pathAfterAncestor = b.selector.segment + pathAfterAncestor
        a = a.parent ?: return null
        b = b.parent ?: return null
    }
    return "." + pathBeforeAncestor + pathAfterAncestor
}

private val RouteSelector.segment: String
    get() = when (this) {
        is PathSegmentConstantRouteSelector -> "/$value"
        is PathSegmentParameterRouteSelector -> "/\${$name}"
        is PathSegmentOptionalParameterRouteSelector -> "/\${$name}"
        is PathSegmentWildcardRouteSelector -> "/anything"
        is PathSegmentTailcardRouteSelector -> "/\${${this.name}}"
        is TrailingSlashRouteSelector -> "/"
        else -> ""
    }

val RouteSelector.maybeMethod: HttpMethod?
    get() = if (this is HttpMethodRouteSelector) this.method else null

val Route.fullPath: String
    get() = (parent?.fullPath ?: "") + selector.segment