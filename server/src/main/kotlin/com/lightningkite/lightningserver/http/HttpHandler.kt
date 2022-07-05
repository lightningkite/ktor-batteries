package com.lightningkite.lightningserver.http

import com.lightningkite.lightningserver.ServerRunner

typealias HttpHandler = suspend ServerRunner.(HttpRequest) -> HttpResponse