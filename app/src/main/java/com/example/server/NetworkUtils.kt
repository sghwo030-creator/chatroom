package com.example.server

import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkUtils {
    /**
     * Resolves the device's local IPv4 address on Wi-Fi or LAN.
     * Falls back to "127.0.0.1" if no external interface is found.
     */
    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "127.0.0.1"
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addresses = intf.inetAddresses ?: continue
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val hostAddress = addr.hostAddress
                        if (!hostAddress.isNullOrEmpty() && !hostAddress.startsWith("127.")) {
                            return hostAddress
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore error and fallback
        }
        return "127.0.0.1"
    }
}
