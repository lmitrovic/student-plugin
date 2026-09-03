package com.github.lmitrovic.studenttestingintellijplugin.tracking

import com.github.lmitrovic.studenttestingintellijplugin.config.RafConfig
import com.github.lmitrovic.studenttestingintellijplugin.util.JsonBuilder
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.HttpRequests
import java.net.HttpURLConnection

/**
 * Jedini put za direktne HTTP pozive plugina ka RAF LMS serveru (feedback metrike i
 * signal o završetku). Pozivati sa background thread-a.
 */
class FeedbackApiClient {

    private val log = Logger.getInstance(FeedbackApiClient::class.java)

    fun sendMetrics(jsonBody: String) = post(RafConfig.feedbackDataUrl, jsonBody)

    fun notifyFinished(studentId: String) =
        post(RafConfig.studentFinishedUrl, JsonBuilder.obj("student_id" to studentId))

    private fun post(url: String, body: String) {
        try {
            HttpRequests.post(url, "application/json")
                .connectTimeout(RafConfig.HTTP_CONNECT_TIMEOUT_MS)
                .readTimeout(RafConfig.HTTP_READ_TIMEOUT_MS)
                .connect { request ->
                    request.write(body)
                    val code = (request.connection as? HttpURLConnection)?.responseCode
                    log.info("POST $url -> $code")
                }
        } catch (e: Throwable) {
            log.warn("POST $url nije uspeo: ${e.message}")
        }
    }
}
