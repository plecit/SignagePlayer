package com.example.tvsignage.xmds

import com.example.tvsignage.core.DeviceInfo
import com.example.tvsignage.core.LogEntry
import com.example.tvsignage.core.SecurePreferences
import com.example.tvsignage.core.SystemLogger
import com.example.tvsignage.stats.StatRecord
import com.example.tvsignage.xmds.models.MediaInventoryItem
import com.example.tvsignage.xmds.models.RegisterResult
import com.example.tvsignage.xmds.models.RequiredFile
import com.example.tvsignage.xmds.models.ScheduleData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class XmdsClient(private val securePreferences: SecurePreferences) {

    private val mediaTypeXml = "text/xml; charset=utf-8".toMediaType()

    private val httpClient: OkHttpClient by lazy {
        createHttpClient()
    }

    private fun createHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)

        if (securePreferences.trustSelfSignedSsl) {
            try {
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })
                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCerts, SecureRandom())
                builder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                builder.hostnameVerifier { _, _ -> true }
            } catch (e: Exception) {
                SystemLogger.e(TAG, "Failed to configure trust-all SSL context", e)
            }
        }

        return builder.build()
    }

    private fun getEndpointUrl(method: String): String {
        val baseUrl = securePreferences.cmsUrl
        return "$baseUrl/xmds.php?v=7&method=$method"
    }

    private suspend fun executeSoapPost(method: String, soapEnvelope: String): String = withContext(Dispatchers.IO) {
        val url = getEndpointUrl(method)
        val requestBody = soapEnvelope.toRequestBody(mediaTypeXml)
        val request = Request.Builder()
            .url(url)
            .header("SOAPAction", "\"urn:xmds#$method\"")
            .post(requestBody)
            .build()

        var attempt = 0
        val maxAttempts = 3
        var lastException: Exception? = null

        while (attempt < maxAttempts) {
            attempt++
            try {
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw Exception("HTTP ${response.code}: ${response.message}")
                    }
                    return@withContext response.body?.string() ?: ""
                }
            } catch (e: Exception) {
                lastException = e
                SystemLogger.w(TAG, "Call $method attempt $attempt failed: ${e.message}")
                if (attempt < maxAttempts) {
                    kotlinx.coroutines.delay((1000L * attempt))
                }
            }
        }
        throw lastException ?: Exception("Network call $method failed")
    }

    suspend fun registerDisplay(hardwareKey: String): RegisterResult {
        val envelope = SoapEnvelopeBuilder.buildRegisterDisplay(
            serverKey = securePreferences.serverKey,
            hardwareKey = hardwareKey,
            displayName = securePreferences.displayName,
            clientType = DeviceInfo.CLIENT_TYPE,
            clientVersion = DeviceInfo.CLIENT_VERSION,
            clientCode = DeviceInfo.CLIENT_CODE,
            operatingSystem = DeviceInfo.operatingSystem,
            macAddress = "02:00:00:00:00:00"
        )
        val response = executeSoapPost("RegisterDisplay", envelope)
        return XmdsResponseParser.parseRegisterDisplay(response)
    }

    suspend fun getRequiredFiles(hardwareKey: String): Pair<List<RequiredFile>, List<String>> {
        val envelope = SoapEnvelopeBuilder.buildRequiredFiles(
            serverKey = securePreferences.serverKey,
            hardwareKey = hardwareKey
        )
        val response = executeSoapPost("RequiredFiles", envelope)
        return XmdsResponseParser.parseRequiredFiles(response)
    }

    suspend fun getSchedule(hardwareKey: String): ScheduleData {
        val envelope = SoapEnvelopeBuilder.buildSchedule(
            serverKey = securePreferences.serverKey,
            hardwareKey = hardwareKey
        )
        val response = executeSoapPost("Schedule", envelope)
        return XmdsResponseParser.parseSchedule(response)
    }

    suspend fun getFileChunk(
        hardwareKey: String,
        fileId: Long,
        fileType: String,
        offset: Long,
        chunkSize: Long
    ): String {
        val envelope = SoapEnvelopeBuilder.buildGetFile(
            serverKey = securePreferences.serverKey,
            hardwareKey = hardwareKey,
            fileId = fileId,
            fileType = fileType,
            chunkOffset = offset,
            chunkSize = chunkSize
        )
        val response = executeSoapPost("GetFile", envelope)
        return XmdsResponseParser.parseGetFile(response)
    }

    suspend fun getResource(
        hardwareKey: String,
        layoutId: Long,
        regionId: String,
        mediaId: Long
    ): String {
        val envelope = SoapEnvelopeBuilder.buildGetResource(
            serverKey = securePreferences.serverKey,
            hardwareKey = hardwareKey,
            layoutId = layoutId,
            regionId = regionId,
            mediaId = mediaId
        )
        val response = executeSoapPost("GetResource", envelope)
        return XmdsResponseParser.parseGetResource(response)
    }

    suspend fun submitMediaInventory(hardwareKey: String, items: List<MediaInventoryItem>): Boolean {
        return try {
            val envelope = SoapEnvelopeBuilder.buildMediaInventory(
                serverKey = securePreferences.serverKey,
                hardwareKey = hardwareKey,
                items = items
            )
            executeSoapPost("MediaInventory", envelope)
            true
        } catch (e: Exception) {
            SystemLogger.w(TAG, "submitMediaInventory failed: ${e.message}")
            false
        }
    }

    suspend fun submitStats(hardwareKey: String, stats: List<StatRecord>): Boolean {
        if (stats.isEmpty()) return true
        return try {
            val envelope = SoapEnvelopeBuilder.buildSubmitStats(
                serverKey = securePreferences.serverKey,
                hardwareKey = hardwareKey,
                stats = stats
            )
            executeSoapPost("SubmitStats", envelope)
            true
        } catch (e: Exception) {
            SystemLogger.w(TAG, "submitStats failed: ${e.message}")
            false
        }
    }

    suspend fun submitLog(hardwareKey: String, logs: List<LogEntry>): Boolean {
        if (logs.isEmpty()) return true
        return try {
            val envelope = SoapEnvelopeBuilder.buildSubmitLog(
                serverKey = securePreferences.serverKey,
                hardwareKey = hardwareKey,
                logs = logs
            )
            executeSoapPost("SubmitLog", envelope)
            true
        } catch (e: Exception) {
            SystemLogger.w(TAG, "submitLog failed: ${e.message}")
            false
        }
    }

    suspend fun notifyStatus(hardwareKey: String, statusXml: String): Boolean {
        return try {
            val envelope = SoapEnvelopeBuilder.buildNotifyStatus(
                serverKey = securePreferences.serverKey,
                hardwareKey = hardwareKey,
                statusXml = statusXml
            )
            executeSoapPost("NotifyStatus", envelope)
            true
        } catch (e: Exception) {
            SystemLogger.w(TAG, "notifyStatus failed: ${e.message}")
            false
        }
    }

    suspend fun submitScreenShot(hardwareKey: String, base64Jpeg: String): Boolean {
        return try {
            val envelope = SoapEnvelopeBuilder.buildSubmitScreenShot(
                serverKey = securePreferences.serverKey,
                hardwareKey = hardwareKey,
                base64Jpeg = base64Jpeg
            )
            executeSoapPost("SubmitScreenShot", envelope)
            true
        } catch (e: Exception) {
            SystemLogger.w(TAG, "submitScreenShot failed: ${e.message}")
            false
        }
    }

    companion object {
        private const val TAG = "XmdsClient"
    }
}
