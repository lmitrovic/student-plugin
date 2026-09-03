package com.github.lmitrovic.studenttestingintellijplugin.tracking

import com.github.lmitrovic.studenttestingintellijplugin.config.RafConfig
import com.github.lmitrovic.studenttestingintellijplugin.util.JsonBuilder
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandEvent
import com.intellij.openapi.command.CommandListener
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent
import java.time.LocalDateTime
import javax.swing.Timer
import kotlin.text.RegexOption

class FeedbackDashboard(
    private val project: Project,
    private val studentId: String,
    private val parentDisposable: Disposable
) {

    private val log = Logger.getInstance(FeedbackDashboard::class.java)
    private val apiClient = FeedbackApiClient()

    private var keystrokeCount = 0
    private var deletionBursts = mutableListOf<Int>()
    private var currentBurstSize = 0
    private var inDeletionMode = false
    private var lastEventTime = 0L
    private val burstTimeoutMs = 2000L
    private var classLines = 0
    private var compileErrors = 0
    private var runtimeErrors = 0
    private var lastLinesCount = 0
    private var currentFileStartTime = System.currentTimeMillis()
    private var currentFileName = ""
    private val snapshotHistory = mutableListOf<Set<String>>()

    fun start() {
        registerSafely("registerDocumentListener") { registerDocumentListener() }
        registerSafely("registerCommandListener") { registerCommandListener() }
        registerSafely("registerKeyDispatcher") { registerKeyDispatcher() }
        registerSafely("registerFileSwitchListener") { registerFileSwitchListener() }
        registerSafely("registerCompileErrorListener") { registerCompileErrorListener() }
        registerSafely("startFeedbackTimer") { startFeedbackTimer() }
        log.info("Feedback Dashboard listeneri pokrenuti za: $studentId")
    }

    fun finish() {
        try {
            apiClient.notifyFinished(studentId)
        } catch (e: Throwable) {
            log.warn("FeedbackDashboard: notifyFinished nije uspeo: ${e.message}")
        }
    }

    private fun registerSafely(name: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            log.warn("FeedbackDashboard: $name nije uspeo", e)
        }
    }

    private fun registerDocumentListener() {
        EditorFactory.getInstance().addEditorFactoryListener(
            object : EditorFactoryListener {
                override fun editorCreated(event: EditorFactoryEvent) {
                    event.editor.document.addDocumentListener(object : DocumentListener {
                        override fun documentChanged(event: DocumentEvent) {
                            try {
                                classLines = event.document.lineCount
                                val added = event.newLength - event.oldLength
                                if (added in 1..100) {
                                    keystrokeCount += added
                                    log.debug("KR: +$added chars, total=$keystrokeCount")
                                } else if (added > 100) {
                                    log.debug("KR: ignorisana velika promena ($added chars) - Paste/Format")
                                }
                            } catch (e: Throwable) {
                                log.warn("FeedbackDashboard: documentChanged nije uspeo: ${e.message}")
                            }
                        }

                        override fun beforeDocumentChange(event: DocumentEvent) {
                            try {
                                val oldLength = event.oldLength
                                val newLength = event.newLength
                                if (oldLength > newLength) {
                                    val now = System.currentTimeMillis()
                                    val deletedCount = oldLength - newLength
                                    if (!inDeletionMode || (now - lastEventTime) > burstTimeoutMs) {
                                        if (inDeletionMode && currentBurstSize > 5) {
                                            deletionBursts.add(currentBurstSize)
                                        }
                                        currentBurstSize = 0
                                        inDeletionMode = true
                                    }
                                    currentBurstSize += deletedCount
                                    lastEventTime = now
                                }
                            } catch (e: Throwable) {
                                log.warn("FeedbackDashboard: beforeDocumentChange nije uspeo: ${e.message}")
                            }
                        }
                    }, parentDisposable)
                }
            },
            parentDisposable
        )
    }

    private fun registerCommandListener() {
        project.messageBus.connect(parentDisposable).subscribe(
            CommandListener.TOPIC,
            object : CommandListener {
                override fun beforeCommandFinished(event: CommandEvent) {
                    try {
                        val commandName = event.commandName ?: return
                        if (!commandName.contains("BackSpace", ignoreCase = true) &&
                            !commandName.contains("Delete", ignoreCase = true)
                        ) return

                        val currentEditor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
                        val now = System.currentTimeMillis()
                        val selectionModel = currentEditor.selectionModel

                        if (!inDeletionMode || (now - lastEventTime) > burstTimeoutMs) {
                            if (inDeletionMode && currentBurstSize > 5) {
                                deletionBursts.add(currentBurstSize)
                            }
                            currentBurstSize = 0
                            inDeletionMode = true
                        }

                        val deletedCount = if (selectionModel.hasSelection()) selectionModel.selectedText?.length ?: 1 else 1
                        currentBurstSize += deletedCount
                        lastEventTime = now
                        log.debug("DB: deleted $deletedCount chars, burst=$currentBurstSize")
                    } catch (e: Throwable) {
                        log.warn("FeedbackDashboard: beforeCommandFinished nije uspeo: ${e.message}")
                    }
                }
            }
        )
    }

    private fun registerKeyDispatcher() {
        val keyDispatcher = KeyEventDispatcher { keyEvent ->
            try {
                if (keyEvent.id == KeyEvent.KEY_PRESSED && keyEvent.keyCode == KeyEvent.VK_DELETE) {
                    val currentEditor = FileEditorManager.getInstance(project).selectedTextEditor
                    if (currentEditor != null) {
                        val now = System.currentTimeMillis()
                        val selectionModel = currentEditor.selectionModel

                        if (!inDeletionMode || (now - lastEventTime) > burstTimeoutMs) {
                            if (inDeletionMode && currentBurstSize > 5) {
                                deletionBursts.add(currentBurstSize)
                            }
                            currentBurstSize = 0
                            inDeletionMode = true
                        }

                        val deletedCount = if (selectionModel.hasSelection()) selectionModel.selectedText?.length ?: 1 else 1
                        currentBurstSize += deletedCount
                        lastEventTime = now
                        log.debug("DB: Delete key deleted $deletedCount chars, burst=$currentBurstSize")
                    }
                }
            } catch (e: Throwable) {
                log.warn("FeedbackDashboard: keyDispatcher nije uspeo: ${e.message}")
            }
            false
        }
        val kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager()
        kfm.addKeyEventDispatcher(keyDispatcher)
        Disposer.register(parentDisposable) { kfm.removeKeyEventDispatcher(keyDispatcher) }
    }

    private fun registerFileSwitchListener() {
        project.messageBus.connect(parentDisposable).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    try {
                        val now = System.currentTimeMillis()
                        if (event.oldFile != null && currentFileName.isNotEmpty()) {
                            log.debug("CFC: left file '$currentFileName', spent ${(now - currentFileStartTime) / 1000}s")
                        }
                        if (event.newFile != null) {
                            currentFileName = event.newFile!!.nameWithoutExtension
                            currentFileStartTime = now
                            log.debug("CFC: opened file '$currentFileName'")
                        }
                    } catch (e: Throwable) {
                        log.warn("FeedbackDashboard: selectionChanged nije uspeo: ${e.message}")
                    }
                }
            }
        )
    }

    private fun registerCompileErrorListener() {
        project.messageBus.connect(parentDisposable).subscribe(
            DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC,
            object : DaemonCodeAnalyzer.DaemonListener {
                override fun daemonFinished() {
                    ApplicationManager.getApplication().invokeLater {
                        try {
                            val currentEditor = FileEditorManager.getInstance(project)
                                .selectedTextEditor ?: return@invokeLater

                            val highlights = EditorErrors.errorHighlights(project, currentEditor.document)

                            compileErrors = highlights.count { h ->
                                val desc = h.description ?: ""
                                !desc.contains("runtime", ignoreCase = true) &&
                                    !desc.contains("exception", ignoreCase = true)
                            }
                            runtimeErrors = highlights.count { h ->
                                val desc = h.description ?: ""
                                desc.contains("runtime", ignoreCase = true) ||
                                    desc.contains("exception", ignoreCase = true)
                            }
                            log.debug("ER: compile=$compileErrors, runtime=$runtimeErrors")
                        } catch (e: Throwable) {
                            log.warn("FeedbackDashboard: daemonFinished nije uspeo: ${e.message}")
                        }
                    }
                }
            }
        )
    }

    private fun startFeedbackTimer() {
        val kolokvijumPocetak = System.currentTimeMillis()
        val intervalMs = RafConfig.FEEDBACK_INTERVAL_MS
        val intervalSec = intervalMs / 1000

        val feedbackTimer = Timer(intervalMs) {
            try {
                val now = System.currentTimeMillis()
                val tCurrentSec = ((now - kolokvijumPocetak) / 1000).toInt()

                val currentSnapshot = try { getAstSnapshot() } catch (e: Throwable) { emptySet() }
                snapshotHistory.add(currentSnapshot)
                while (snapshotHistory.size > 10) snapshotHistory.removeFirst()

                val csValue = if (snapshotHistory.size >= 2) calculateJaccardSimilarity(snapshotHistory) else 1.0
                val currentLines = classLines
                val deltaL = currentLines - lastLinesCount
                lastLinesCount = currentLines
                val timeOnClassSec = if (currentFileName.isNotEmpty()) (now - currentFileStartTime) / 1000 else 0

                if (inDeletionMode && currentBurstSize > 0) {
                    if (currentBurstSize > 5) {
                        deletionBursts.add(currentBurstSize)
                        log.debug("DB: final burst saved: $currentBurstSize")
                    }
                    inDeletionMode = false
                    currentBurstSize = 0
                }

                val json = JsonBuilder.obj(
                    "student_id" to studentId,
                    "timestamp" to LocalDateTime.now().toString(),
                    "window_start_sec" to (tCurrentSec - intervalSec),
                    "window_end_sec" to tCurrentSec,
                    "t_current_min" to (tCurrentSec / 60),
                    "t_total_min" to RafConfig.EXAM_DURATION_MINUTES,
                    "metrics" to mapOf(
                        "keystroke_count" to keystrokeCount,
                        "compile_errors" to compileErrors,
                        "runtime_errors" to runtimeErrors,
                        "time_on_class_seconds" to timeOnClassSec,
                        "class_line_count" to currentLines,
                        "delta_lines" to deltaL,
                        "deletion_bursts" to deletionBursts.sum(),
                        "ast_nodes" to currentSnapshot.toList(),
                        "cs_value" to csValue,
                        "snapshot_history_size" to snapshotHistory.size
                    )
                )

                log.debug("WINDOW ${tCurrentSec / intervalSec} | KR=$keystrokeCount DB=${deletionBursts.joinToString()} CS=$csValue " +
                    "compile=$compileErrors runtime=$runtimeErrors timeOnClass=${timeOnClassSec}s deltaL=$deltaL")

                keystrokeCount = 0
                compileErrors = 0
                runtimeErrors = 0
                deletionBursts = mutableListOf()

                ApplicationManager.getApplication().executeOnPooledThread {
                    apiClient.sendMetrics(json)
                }
            } catch (e: Throwable) {
                log.warn("FeedbackDashboard: feedbackTimer tick nije uspeo: ${e.message}")
            }
        }

        Disposer.register(parentDisposable) { feedbackTimer.stop() }
        feedbackTimer.start()
    }

    private fun getAstSnapshot(): Set<String> {
        val snapEditor = FileEditorManager.getInstance(project).selectedTextEditor
        val text = snapEditor?.document?.text ?: return emptySet()

        val nodes = mutableSetOf<String>()
        var inComment = false
        var inString = false

        for (line in text.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("//")) continue
            if (trimmed.startsWith("/*")) inComment = true
            if (inComment && trimmed.contains("*/")) { inComment = false; continue }
            if (inComment) continue

            if (trimmed.contains("\"") && !trimmed.startsWith("\"")) {
                if (inString) inString = false
                else if (trimmed.count { it == '"' } % 2 == 1) inString = true
            }
            if (inString) continue

            when {
                Regex("""^\s*(public|private|protected)?\s*(class|interface|enum)\s+(\w+)""").containsMatchIn(trimmed) ->
                    nodes.add("ClassDeclaration")
                Regex("""^\s*(public|private|protected)?\s*(static)?\s*(synchronized)?\s*(\w+)\s+(\w+)\s*\(""").containsMatchIn(trimmed) ->
                    nodes.add("MethodDeclaration")
                Regex("""^\s*if\s*\(""", RegexOption.IGNORE_CASE).containsMatchIn(trimmed) -> nodes.add("IfStatement")
                Regex("""^\s*for\s*\(""", RegexOption.IGNORE_CASE).containsMatchIn(trimmed) -> nodes.add("ForStatement")
                Regex("""^\s*while\s*\(""", RegexOption.IGNORE_CASE).containsMatchIn(trimmed) -> nodes.add("WhileStatement")
                Regex("""^\s*do\s*\{""", RegexOption.IGNORE_CASE).containsMatchIn(trimmed) -> nodes.add("DoWhileStatement")
                Regex("""^\s*return\s+""").containsMatchIn(trimmed) -> nodes.add("ReturnStatement")
                Regex("""^\s*try\s*\{""", RegexOption.IGNORE_CASE).containsMatchIn(trimmed) -> nodes.add("TryStatement")
                Regex("""^\s*catch\s*\(""", RegexOption.IGNORE_CASE).containsMatchIn(trimmed) -> nodes.add("CatchClause")
                Regex("""^\s*switch\s*\(""", RegexOption.IGNORE_CASE).containsMatchIn(trimmed) -> nodes.add("SwitchStatement")
                Regex("""^\s*case\s+""", RegexOption.IGNORE_CASE).containsMatchIn(trimmed) -> nodes.add("CaseClause")
                Regex("""^\s*break;?""", RegexOption.IGNORE_CASE).containsMatchIn(trimmed) -> nodes.add("BreakStatement")
            }
        }
        return nodes
    }

    private fun calculateJaccardSimilarity(snapshots: List<Set<String>>): Double {
        if (snapshots.size < 2) return 1.0
        var totalSimilarity = 0.0
        var pairCount = 0

        for (i in snapshots.indices) {
            for (j in i + 1 until snapshots.size) {
                val set1 = snapshots[i]
                val set2 = snapshots[j]
                totalSimilarity += when {
                    set1.isEmpty() && set2.isEmpty() -> 1.0
                    set1.isEmpty() || set2.isEmpty() -> 0.0
                    else -> set1.intersect(set2).size.toDouble() / set1.union(set2).size.toDouble()
                }
                pairCount++
            }
        }
        return if (pairCount > 0) totalSimilarity / pairCount else 1.0
    }
}
