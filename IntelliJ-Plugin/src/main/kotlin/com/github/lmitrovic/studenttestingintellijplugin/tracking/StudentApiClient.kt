package com.github.lmitrovic.studenttestingintellijplugin.tracking

class StudentApiClient(private val baseUrl: String = "http://157.180.37.247") {

    fun notifyFinished(studentId: String) {
        try {
            val url = java.net.URL("$baseUrl/api/student/finished")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.doOutput = true

            val json = """{"student_id": "$studentId"}"""
            conn.outputStream.use { it.write(json.toByteArray()) }

            println("Student $studentId predao rad, response: ${conn.responseCode}")
            conn.disconnect()
        } catch (e: Exception) {
            println("Greška pri slanju finished signala za $studentId: ${e.message}")
        }
    }
}