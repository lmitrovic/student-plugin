package com.github.lmitrovic.studenttestingintellijplugin.config

/**
 * Jedno mesto za sve podešljive vrednosti plugina. Ranije su URL-ovi i konstante
 * bili raštrkani (i hardkodovani) po više fajlova.
 *
 * TODO: server trenutno radi na običnom HTTP-u preko IP adrese; preći na HTTPS
 * čim server dobije sertifikat.
 */
object RafConfig {

    /** Bazni URL RAF LMS servera za feedback/metrike i proveru verzije plugina. */
    const val SERVER_BASE_URL: String = "http://157.180.37.247"

    /** Folder u home direktorijumu u koji se privremeno skida zadatak. */
    const val DOWNLOAD_FOLDER_NAME: String = "student-plugin-temp"

    /**
     * Folder u home direktorijumu u koji se pravi kopija sadržaja projekta pre nego što
     * ga preuzimanje zadatka pregazi. Kopija se briše nakon uspešne predaje rada.
     */
    const val BACKUP_FOLDER_NAME: String = ".raf-lms-backup"

    /** Pretpostavljeno trajanje kolokvijuma u minutima (feedback prozor). */
    const val EXAM_DURATION_MINUTES: Int = 180

    /** Interval slanja feedback metrika, u milisekundama. */
    const val FEEDBACK_INTERVAL_MS: Int = 20_000

    /** Tajmauti za HTTP pozive, u milisekundama. */
    const val HTTP_CONNECT_TIMEOUT_MS: Int = 5_000
    const val HTTP_READ_TIMEOUT_MS: Int = 5_000

    val feedbackDataUrl: String get() = "$SERVER_BASE_URL/api/data"
    val studentFinishedUrl: String get() = "$SERVER_BASE_URL/api/student/finished"
    val pluginUpdateXmlUrl: String get() = "$SERVER_BASE_URL/updatePluginsStudent.xml"
}
