package com.example.tvsignage.core

import android.app.ActivityManager
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import java.net.NetworkInterface
import java.util.Collections

object DeviceInfo {

    const val CLIENT_TYPE = "android"
    const val CLIENT_VERSION = "1.0.0"
    const val CLIENT_CODE = 100

    val operatingSystem: String
        get() = "Android TV ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}, ${Build.MODEL})"

    fun getMacAddress(context: Context): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (intf.name.equals("wlan0", ignoreCase = true) || intf.name.equals("eth0", ignoreCase = true)) {
                    val mac = intf.hardwareAddress ?: continue
                    val sb = StringBuilder()
                    for (b in mac) {
                        sb.append(String.format("%02X:", b))
                    }
                    if (sb.isNotEmpty()) {
                        sb.deleteCharAt(sb.length - 1)
                    }
                    return sb.toString()
                }
            }
        } catch (e: Exception) {
            SystemLogger.w("DeviceInfo", "Failed to retrieve MAC address: ${e.message}")
        }
        return "02:00:00:00:00:00"
    }

    fun getIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val host = addr.hostAddress ?: ""
                        if (!host.contains(':')) { // IPv4
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            SystemLogger.w("DeviceInfo", "Failed to retrieve IP address: ${e.message}")
        }
        return "127.0.0.1"
    }

    fun getAvailableDiskSpaceMb(context: Context): Long {
        return try {
            val stat = StatFs(context.filesDir.path)
            (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024)
        } catch (e: Exception) {
            0L
        }
    }

    fun getTotalDiskSpaceMb(context: Context): Long {
        return try {
            val stat = StatFs(context.filesDir.path)
            (stat.blockCountLong * stat.blockSizeLong) / (1024 * 1024)
        } catch (e: Exception) {
            0L
        }
    }

    fun getAvailableMemoryMb(context: Context): Long {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            memInfo.availMem / (1024 * 1024)
        } catch (e: Exception) {
            0L
        }
    }
}
