package com.github.lmitrovic.studenttestingintellijplugin.session

import com.github.lmitrovic.studenttestingintellijplugin.tracking.ActivityTracker
import com.github.lmitrovic.studenttestingintellijplugin.tracking.FeedbackDashboard
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import raflms.trackingstub.api.TrackingStubService

/**
 * Vlasnik svih tracking listenera / tajmera za jednu izradu zadatka. Sve se kači na
 * interni [Disposable] koji se uredno gasi na kraju izrade ili pri zatvaranju projekta -
 * tako da listeneri, `KeyEventDispatcher`, `WindowFocusListener` i Swing tajmeri ne cure.
 *
 * [start] je idempotentan: ponovni poziv (npr. posle restarta IDE-a kad se sesija obnavlja)
 * ne kači listenere dvaput dok je prethodni skup još aktivan.
 */
@Service(Service.Level.PROJECT)
class StudentTrackingSession(private val project: Project) : Disposable {

    private val log = thisLogger()

    private var running: Disposable? = null
    private var feedbackDashboard: FeedbackDashboard? = null

    val isRunning: Boolean
        @Synchronized get() = running != null

    @Synchronized
    fun start(trackingService: TrackingStubService, studentId: String, taskId: String) {
        if (running != null) {
            log.info("Tracking već aktivan za $studentId - preskačem ponovno kačenje.")
            return
        }
        val disposable = Disposer.newDisposable("raf-student-tracking")
        Disposer.register(this, disposable)
        running = disposable

        try {
            trackingService.startTracking(studentId, taskId)
        } catch (e: Throwable) {
            log.warn("startTracking nije uspeo za $studentId", e)
        }
        log.info("Tracking pokrenut: $studentId / $taskId")

        try {
            ActivityTracker(project, trackingService, studentId, disposable).start()
        } catch (e: Throwable) {
            log.warn("ActivityTracker nije pokrenut", e)
        }
        feedbackDashboard = try {
            FeedbackDashboard(project, studentId, disposable).also { it.start() }
        } catch (e: Throwable) {
            log.warn("FeedbackDashboard nije pokrenut", e)
            null
        }
    }

    /** Poziva se pri finalnoj predaji: javi serveru da je student završio, pa ugasi listenere. */
    @Synchronized
    fun finishAndStop() {
        try {
            feedbackDashboard?.finish()
        } catch (e: Throwable) {
            log.warn("FeedbackDashboard.finish nije uspeo", e)
        }
        stop()
    }

    @Synchronized
    fun stop() {
        running?.let { Disposer.dispose(it) }
        running = null
        feedbackDashboard = null
    }

    override fun dispose() = stop()

    companion object {
        fun getInstance(project: Project): StudentTrackingSession = project.service()
    }
}
