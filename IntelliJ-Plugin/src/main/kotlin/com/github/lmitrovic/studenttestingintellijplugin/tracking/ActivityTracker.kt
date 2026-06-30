package com.github.lmitrovic.studenttestingintellijplugin.tracking

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.execution.ExecutionListener
import com.intellij.execution.ExecutionManager
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionResult
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import raflms.trackingstub.api.TrackingStubService
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import javax.swing.Timer

class ActivityTracker(
    private val project: Project,
    private val trackingService: TrackingStubService,
    private val studentId: String
) {

    fun start() {
        try { registerCodeChangeTracker() } catch (e: Throwable) { println("ActivityTracker: registerCodeChangeTracker failed: ${e.message}") }
        try { registerCompilationTracker() } catch (e: Throwable) { println("ActivityTracker: registerCompilationTracker failed: ${e.message}") }
        try { registerFocusTracker() } catch (e: Throwable) { println("ActivityTracker: registerFocusTracker failed: ${e.message}") }
        try { registerInactivityTracker() } catch (e: Throwable) { println("ActivityTracker: registerInactivityTracker failed: ${e.message}") }
        try { registerFileSwitchTracker() } catch (e: Throwable) { println("ActivityTracker: registerFileSwitchTracker failed: ${e.message}") }
        try { registerErrorTracker() } catch (e: Throwable) { println("ActivityTracker: registerErrorTracker failed: ${e.message}") }
        try { registerTestExecutionTracker() } catch (e: Throwable) { println("ActivityTracker: registerTestExecutionTracker failed: ${e.message}") }
        println("=== RAF TRACKING LISTENERS STARTED for: $studentId ===")
    }

    private fun safeLog(block: () -> Unit) {
        try { block() } catch (e: Throwable) { println("ActivityTracker: logEvent failed: ${e.message}") }
    }

    private fun registerCodeChangeTracker() {
        val lastCodeChangeTime = mutableMapOf<String, Long>()
        EditorFactory.getInstance().addEditorFactoryListener(
            object : EditorFactoryListener {
                override fun editorCreated(event: EditorFactoryEvent) {
                    val editor = event.editor
                    editor.document.addDocumentListener(object : DocumentListener {
                        override fun documentChanged(e: DocumentEvent) {
                            try {
                                val file = FileDocumentManager.getInstance().getFile(editor.document)
                                val charsAdded = e.newLength - e.oldLength
                                val fileName = file?.name ?: "unknown"
                                val now = System.currentTimeMillis()
                                val last = lastCodeChangeTime[fileName] ?: 0L
                                if (now - last >= 500) {
                                    lastCodeChangeTime[fileName] = now
                                    safeLog {
                                        trackingService.logEvent(
                                            "CODE_CHANGE", studentId,
                                            mapOf(
                                                "file" to fileName,
                                                "line" to editor.caretModel.logicalPosition.line,
                                                "charsAdded" to charsAdded,
                                                "isDelete" to (charsAdded < 0)
                                            )
                                        )
                                    }
                                }
                            } catch (e: Throwable) {
                                println("ActivityTracker: CODE_CHANGE handler failed: ${e.message}")
                            }
                        }
                    })
                }
            },
            project
        )
    }

    private fun registerCompilationTracker() {
        project.messageBus.connect().subscribe(
            AnActionListener.TOPIC,
            object : AnActionListener {
                override fun beforeActionPerformed(
                    action: AnAction,
                    event: AnActionEvent
                ) {
                    try {
                        val actionId = ActionManager.getInstance().getId(action)
                        if (actionId != null && isCompilationAction(actionId)) {
                            safeLog { trackingService.logEvent("COMPILATION_STARTED", studentId, mapOf("actionId" to actionId)) }
                        }
                    } catch (e: Throwable) {
                        println("ActivityTracker: beforeActionPerformed failed: ${e.message}")
                    }
                }

                override fun afterActionPerformed(
                    action: AnAction,
                    event: AnActionEvent,
                    result: AnActionResult
                ) {
                    try {
                        val actionId = ActionManager.getInstance().getId(action)
                        if (actionId != null && isCompilationAction(actionId)) {
                            safeLog {
                                trackingService.logEvent(
                                    "COMPILATION_FINISHED", studentId,
                                    mapOf("actionId" to actionId, "success" to result.isPerformed)
                                )
                            }
                        }
                    } catch (e: Throwable) {
                        println("ActivityTracker: afterActionPerformed failed: ${e.message}")
                    }
                }
            }
        )
    }

    private fun registerFocusTracker() {
        val ideFrame = WindowManager.getInstance().getFrame(project)
        ideFrame?.addWindowFocusListener(object : WindowFocusListener {
            override fun windowGainedFocus(e: WindowEvent) {
                safeLog { trackingService.logEvent("FOCUS_GAINED", studentId) }
            }
            override fun windowLostFocus(e: WindowEvent) {
                safeLog { trackingService.logEvent("FOCUS_LOST", studentId) }
            }
        })
    }

    private fun registerInactivityTracker() {
        val inactivityThresholdMs = 60_000L
        var lastActivityTime = System.currentTimeMillis()
        var inactivityReported = false
        var currentFileForInactivity = ""

        val inactivityTimer = Timer(10_000) {
            try {
                val now = System.currentTimeMillis()
                val inactiveDuration = now - lastActivityTime
                if (inactiveDuration >= inactivityThresholdMs && !inactivityReported) {
                    inactivityReported = true
                    safeLog {
                        trackingService.logEvent(
                            "INACTIVITY", studentId,
                            mapOf("durationSeconds" to inactiveDuration / 1000, "file" to currentFileForInactivity)
                        )
                    }
                }
            } catch (e: Throwable) {
                println("ActivityTracker: inactivityTimer failed: ${e.message}")
            }
        }
        inactivityTimer.start()

        EditorFactory.getInstance().addEditorFactoryListener(
            object : EditorFactoryListener {
                override fun editorCreated(event: EditorFactoryEvent) {
                    event.editor.document.addDocumentListener(object : DocumentListener {
                        override fun documentChanged(e: DocumentEvent) {
                            try {
                                lastActivityTime = System.currentTimeMillis()
                                inactivityReported = false
                                val file = FileDocumentManager.getInstance().getFile(event.editor.document)
                                currentFileForInactivity = file?.name ?: "unknown"
                            } catch (e: Throwable) {
                                println("ActivityTracker: inactivity documentChanged failed: ${e.message}")
                            }
                        }
                    })
                }
            },
            project
        )
    }

    private fun registerFileSwitchTracker() {
        var currentOpenFile = ""
        var fileOpenedAt = System.currentTimeMillis()

        project.messageBus.connect().subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    try {
                        val now = System.currentTimeMillis()
                        val newFile = event.newFile?.name ?: return
                        val oldFile = event.oldFile?.name ?: ""

                        if (oldFile.isNotEmpty() && newFile != oldFile) {
                            val timeSpentMs = now - fileOpenedAt
                            safeLog {
                                trackingService.logEvent(
                                    "FILE_SWITCH", studentId,
                                    mapOf("fromFile" to oldFile, "toFile" to newFile, "timeSpentMs" to timeSpentMs)
                                )
                            }
                        }

                        currentOpenFile = newFile
                        fileOpenedAt = now
                    } catch (e: Throwable) {
                        println("ActivityTracker: FILE_SWITCH handler failed: ${e.message}")
                    }
                }
            }
        )
    }

    private fun registerErrorTracker() {
        val lastErrorTime = mutableMapOf<String, Long>()
        val errorThrottleMs = 5_000L

        project.messageBus.connect().subscribe(
            DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC,
            object : DaemonCodeAnalyzer.DaemonListener {
                override fun daemonFinished() {
                    ApplicationManager.getApplication().invokeLater {
                        try {
                            val currentEditor = FileEditorManager.getInstance(project)
                                .selectedTextEditor ?: return@invokeLater

                            val file = FileDocumentManager.getInstance()
                                .getFile(currentEditor.document)?.name ?: "unknown"

                            val highlights = DaemonCodeAnalyzerImpl.getHighlights(
                                    currentEditor.document,
                                    HighlightSeverity.ERROR,
                                    project
                                )

                            highlights.forEach { highlight ->
                                val errorMessage = highlight.description ?: return@forEach
                                val throttleKey = "$file:$errorMessage"
                                val now = System.currentTimeMillis()
                                val last = lastErrorTime[throttleKey] ?: 0L
                                if (now - last >= errorThrottleMs) {
                                    lastErrorTime[throttleKey] = now
                                    safeLog {
                                        trackingService.logEvent(
                                            "ERROR_DETECTED", studentId,
                                            mapOf(
                                                "file" to file,
                                                "errorMessage" to errorMessage,
                                                "line" to highlight.actualStartOffset.toDouble()
                                            )
                                        )
                                    }
                                }
                            }
                        } catch (e: Throwable) {
                            println("ActivityTracker: ERROR_DETECTED handler failed: ${e.message}")
                        }
                    }
                }
            }
        )
    }

    private fun registerTestExecutionTracker() {
        project.messageBus.connect().subscribe(
            ExecutionManager.EXECUTION_TOPIC,
            object : ExecutionListener {
                override fun processStarted(
                    executorId: String,
                    env: ExecutionEnvironment,
                    handler: ProcessHandler
                ) {
                    try {
                        safeLog {
                            trackingService.logEvent(
                                "TEST_EXECUTION_STARTED", studentId,
                                mapOf("executorId" to executorId, "configName" to env.runProfile.name)
                            )
                        }
                    } catch (e: Throwable) {
                        println("ActivityTracker: processStarted failed: ${e.message}")
                    }
                }

                override fun processTerminated(
                    executorId: String,
                    env: ExecutionEnvironment,
                    handler: ProcessHandler,
                    exitCode: Int
                ) {
                    try {
                        safeLog {
                            trackingService.logEvent(
                                "TEST_EXECUTION_FINISHED", studentId,
                                mapOf(
                                    "executorId" to executorId,
                                    "configName" to env.runProfile.name,
                                    "exitCode" to exitCode,
                                    "success" to (exitCode == 0)
                                )
                            )
                        }
                    } catch (e: Throwable) {
                        println("ActivityTracker: processTerminated failed: ${e.message}")
                    }
                }
            }
        )
    }

    private fun isCompilationAction(actionId: String): Boolean {
        return actionId.contains("Compile") ||
                actionId.contains("Build") ||
                actionId == "CompileDirty" ||
                actionId == "BuildProject" ||
                actionId == "RebuildProject" ||
                actionId == "CompileProject" ||
                actionId == "Run" ||
                actionId == "Debug"
    }
}
