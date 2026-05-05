package space.diomentia.ehzdiagnosticstracker.api

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.coroutines.resume

private const val MDNS_SERVICE_TYPE = "_ehz-backend._tcp"
private const val UDP_PORT = 5370
private const val UDP_MAGIC = "EHZ_DISCOVER_V1"

@Serializable
private data class UdpDiscoveryReply(
    val ip: String,
    val port: Int,
    val apiBasePath: String,
    val version: Int
)

suspend fun discoverBackend(context: Context, timeoutMs: Long = 2000L): String? =
    discoverViaMdns(context, timeoutMs) ?: discoverViaUdpBroadcast(timeoutMs)

@Suppress("DEPRECATION")
private suspend fun discoverViaMdns(context: Context, timeoutMs: Long): String? =
    withTimeoutOrNull(timeoutMs) {
        suspendCancellableCoroutine { cont ->
            val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
            var discoveryListener: NsdManager.DiscoveryListener? = null

            val resolveListener = object : NsdManager.ResolveListener {
                override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                    if (cont.isActive) cont.resume(null)
                }

                override fun onServiceResolved(info: NsdServiceInfo) {
                    if (cont.isActive) cont.resume("http://${info.host.hostAddress}:${info.port}")
                }
            }

            discoveryListener = object : NsdManager.DiscoveryListener {
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    if (cont.isActive) cont.resume(null)
                }

                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
                override fun onDiscoveryStarted(serviceType: String) {}
                override fun onDiscoveryStopped(serviceType: String) {}
                override fun onServiceFound(info: NsdServiceInfo) {
                    try {
                        nsdManager.stopServiceDiscovery(this)
                    } catch (_: Exception) {
                    }
                    nsdManager.resolveService(info, resolveListener)
                }

                override fun onServiceLost(info: NsdServiceInfo) {}
            }.also { listener ->
                cont.invokeOnCancellation {
                    try {
                        nsdManager.stopServiceDiscovery(listener)
                    } catch (_: Exception) {
                    }
                }
                nsdManager.discoverServices(MDNS_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
            }
        }
    }

private suspend fun discoverViaUdpBroadcast(timeoutMs: Long): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            DatagramSocket().use { socket ->
                socket.broadcast = true
                socket.soTimeout = timeoutMs.toInt()
                val magic = UDP_MAGIC.toByteArray(Charsets.US_ASCII)
                socket.send(
                    DatagramPacket(
                        magic,
                        magic.size,
                        InetAddress.getByName("255.255.255.255"),
                        UDP_PORT
                    )
                )
                val buf = ByteArray(1024)
                val packet = DatagramPacket(buf, buf.size)
                socket.receive(packet)
                val reply = Json.decodeFromString<UdpDiscoveryReply>(
                    String(packet.data, 0, packet.length, Charsets.UTF_8)
                )
                "http://${reply.ip}:${reply.port}"
            }
        }.getOrNull()
    }
