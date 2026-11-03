package com.example.tvsignage.report

import android.content.Context
import android.os.SystemClock
import com.example.tvsignage.core.DeviceInfo
import com.example.tvsignage.xmds.XmdsClient

class StatusNotifier(
    private val context: Context,
    private val xmdsClient: XmdsClient
) {

    suspend fun sendStatus(hardwareKey: String, currentLayoutId: Long): Boolean {
        val totalDiskMb = DeviceInfo.getTotalDiskSpaceMb(context)
        val freeDiskMb = DeviceInfo.getAvailableDiskSpaceMb(context)
        val availMemMb = DeviceInfo.getAvailableMemoryMb(context)
        val ip = DeviceInfo.getIpAddress()
        val uptimeSec = SystemClock.elapsedRealtime() / 1000

        val statusXml = """
            <status>
                <version>${DeviceInfo.CLIENT_VERSION}</version>
                <codeVersion>${DeviceInfo.CLIENT_CODE}</codeVersion>
                <os>${DeviceInfo.operatingSystem}</os>
                <ipAddress>$ip</ipAddress>
                <memoryFreeMb>$availMemMb</memoryFreeMb>
                <diskTotalMb>$totalDiskMb</diskTotalMb>
                <diskFreeMb>$freeDiskMb</diskFreeMb>
                <currentLayoutId>$currentLayoutId</currentLayoutId>
                <uptimeSeconds>$uptimeSec</uptimeSeconds>
            </status>
        """.trimIndent()

        return xmdsClient.notifyStatus(hardwareKey, statusXml)
    }
}
