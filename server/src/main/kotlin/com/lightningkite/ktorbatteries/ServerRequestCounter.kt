package com.lightningkite.ktorbatteries

import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicLong

object ServerRequestCounter {
    val socketsOpened = AtomicLong(0)
    val socketsClosed = AtomicLong(0)
    val socketChannelsOpened = AtomicLong(0)
    val socketChannelsClosed = AtomicLong(0)
    val socketChannelMessagesSent = AtomicLong(0)
    val socketChannelMessagesReceived = AtomicLong(0)
    val webSocketsOpened = AtomicLong(0)
    val webSocketsClosed = AtomicLong(0)
    val webSocketReceived = AtomicLong(0)
    val webSocketSent = AtomicLong(0)
    val webSocketDataTotal = AtomicLong(0)
    val desktopSocketsOpened = AtomicLong(0)
    val desktopSocketsClosed = AtomicLong(0)
    val desktopSocketReceived = AtomicLong(0)
    val desktopSocketSent = AtomicLong(0)
    val destkopSocketDataTotal = AtomicLong(0)
    val androidSocketsOpened = AtomicLong(0)
    val androidSocketsClosed = AtomicLong(0)
    val androidSocketReceived = AtomicLong(0)
    val androidSocketSent = AtomicLong(0)
    val androidSocketDataTotal = AtomicLong(0)
    val iOSSocketsClosed = AtomicLong(0)
    val iOSSocketsOpened = AtomicLong(0)
    val iOSSocketReceived = AtomicLong(0)
    val iOSSocketSent = AtomicLong(0)
    val iOSSocketDataTotal = AtomicLong(0)
    val otherDeviceSocketsClosed = AtomicLong(0)
    val otherDeviceSocketsOpened = AtomicLong(0)
    val otherDeviceSocketReceived = AtomicLong(0)
    val otherDeviceSocketSent = AtomicLong(0)
    val otherSocketDataTotal = AtomicLong(0)
    val restCalls = AtomicLong(0)
    val webRest = AtomicLong(0)
    val desktopRest = AtomicLong(0)
    val androidRest = AtomicLong(0)
    val iOSRest = AtomicLong(0)
    val otherDeviceRest = AtomicLong(0)
    val internalErrors = AtomicLong(0)
    val errorResponses = AtomicLong(0)
    val authFailures = AtomicLong(0)
}

fun ServerRequestCounter.toResponse(): ServerRequestResponse = ServerRequestResponse(
    socketsOpened = socketsOpened.get(),
    socketsClosed = socketsClosed.get(),
    socketChannelsOpened = socketChannelsOpened.get(),
    socketChannelsClosed = socketChannelsClosed.get(),
    socketChannelMessagesSent = socketChannelMessagesSent.get(),
    socketChannelMessagesReceived = socketChannelMessagesReceived.get(),
    webSocketsOpened = webSocketsOpened.get(),
    webSocketsClosed = webSocketsClosed.get(),
    webSocketReceived = webSocketReceived.get(),
    webSocketSent = webSocketSent.get(),
    webSocketDataTotal = webSocketDataTotal.get(),
    desktopSocketsOpened = desktopSocketsOpened.get(),
    desktopSocketsClosed = desktopSocketsClosed.get(),
    desktopSocketReceived = desktopSocketReceived.get(),
    desktopSocketSent = desktopSocketSent.get(),
    destkopSocketDataTotal = destkopSocketDataTotal.get(),
    androidSocketsOpened = androidSocketsOpened.get(),
    androidSocketsClosed = androidSocketsClosed.get(),
    androidSocketReceived = androidSocketReceived.get(),
    androidSocketSent = androidSocketSent.get(),
    androidSocketDataTotal = androidSocketDataTotal.get(),
    iOSSocketsClosed = iOSSocketsClosed.get(),
    iOSSocketsOpened = iOSSocketsOpened.get(),
    iOSSocketReceived = iOSSocketReceived.get(),
    iOSSocketSent = iOSSocketSent.get(),
    iOSSocketDataTotal = iOSSocketDataTotal.get(),
    otherDeviceSocketsClosed = otherDeviceSocketsClosed.get(),
    otherDeviceSocketsOpened = otherDeviceSocketsOpened.get(),
    otherDeviceSocketReceived = otherDeviceSocketReceived.get(),
    otherDeviceSocketSent = otherDeviceSocketSent.get(),
    otherSocketDataTotal = otherSocketDataTotal.get(),
    restCalls = restCalls.get(),
    webRest = webRest.get(),
    desktopRest = desktopRest.get(),
    androidRest = androidRest.get(),
    iOSRest = iOSRest.get(),
    otherDeviceRest = otherDeviceRest.get(),
    internalErrors = internalErrors.get(),
    errorResponses = errorResponses.get(),
    authFailures = authFailures.get(),
)


@Serializable
data class ServerRequestResponse(
    val socketsOpened: Long,
    val socketsClosed: Long,
    val socketChannelsOpened: Long,
    val socketChannelsClosed: Long,
    val socketChannelMessagesSent: Long,
    val socketChannelMessagesReceived: Long,
    val webSocketsOpened: Long,
    val webSocketsClosed: Long,
    val webSocketReceived: Long,
    val webSocketSent: Long,
    val webSocketDataTotal: Long,
    val desktopSocketsOpened: Long,
    val desktopSocketsClosed: Long,
    val desktopSocketReceived: Long,
    val desktopSocketSent: Long,
    val destkopSocketDataTotal: Long,
    val androidSocketsOpened: Long,
    val androidSocketsClosed: Long,
    val androidSocketReceived: Long,
    val androidSocketSent: Long,
    val androidSocketDataTotal: Long,
    val iOSSocketsClosed: Long,
    val iOSSocketsOpened: Long,
    val iOSSocketReceived: Long,
    val iOSSocketSent: Long,
    val iOSSocketDataTotal: Long,
    val otherDeviceSocketsClosed: Long,
    val otherDeviceSocketsOpened: Long,
    val otherDeviceSocketReceived: Long,
    val otherDeviceSocketSent: Long,
    val otherSocketDataTotal: Long,
    val restCalls: Long,
    val webRest: Long,
    val desktopRest: Long,
    val androidRest: Long,
    val iOSRest: Long,
    val otherDeviceRest: Long,
    val internalErrors: Long,
    val errorResponses: Long,
    val authFailures: Long,
)