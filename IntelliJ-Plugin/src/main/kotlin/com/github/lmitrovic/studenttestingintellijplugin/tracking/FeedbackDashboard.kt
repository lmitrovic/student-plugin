package com.github.lmitrovic.studenttestingintellijplugin.tracking

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandEvent
import com.intellij.openapi.command.CommandListener
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import javax.swing.Timer
import kotlin.text.RegexOption

class FeedbackDashboard(
    private val project: Project,
    private val studentId: String
) {

    private val apiClient = StudentApiClient()

    private var keystrokeCount = 0
    private var deletionBursts = mutableListOf<Int>()
    private var currentBurstSize = 0
    private var inDeletionMode = false
    private var lastEventTime = 0L
    private val BURST_TIMEOUT_MS = 2000L
    private var classLines = 0
    private var compileErrors = 0
    private var runtimeErrors = 0
    private var lastLinesCount = 0
    private var currentFileStartTime = System.currentTimeMillis()
    private var currentFileName = ""
    private val snapshotHistory = mutableListOf<Set<String>>()

    fun start() {
        try { registerDocumentListener() } catch (e: Throwable) { println("FeedbackDashboard: registerDocumentListener failed: ${e.message}") }
        try { registerCommandListener() } catch (e: Throwable) { println("FeedbackDashboard: registerCommandListener failed: ${e.message}") }
        try { registerKeyDispatcher() } catch (e: Throwable) { println("FeedbackDashboard: registerKeyDispatcher failed: ${e.message}") }
        try { registerFileSwitchListener() } catch (e: Throwable) { println("FeedbackDashboard: registerFileSwitchListener failed: ${e.message}") }
        try { registerCompileErrorListener() } catch (e: Throwable) { println("FeedbackDashboard: registerCompileErrorListener failed: ${e.message}") }
        try { startFeedbackTimer() } catch (e: Throwable) { println("FeedbackDashboard: startFeedbackTimer failed: ${e.message}") }
        println("Feedback Dashboard listeneri pokrenuti za: $studentId")
    }

    fun finish() {
        try {
            apiClient.notifyFinished(studentId)
        } catch (e: Throwable) {
            println("FeedbackDashboard: notifyFinished failed: ${e.message}")
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
                                if (added > 0 && added <= 100) {
                                    keystrokeCount += added
                                    println("KR DEBUG: +$added chars, total=$keystrokeCount")
                                } else if (added > 100) {
                                    println("KR DEBUG: IGNORISANA velika promena ($added chars) - Paste/Format")
                                }
                            } catch (e: Throwable) {
                                println("FeedbackDashboard: documentChanged failed: ${e.message}")
                            }
                        }

                        override fun beforeDocumentChange(event: DocumentEvent) {
                            try {
                                val oldLength = event.oldLength
                                val newLength = event.newLength
                                if (oldLength > newLength) {
                                    val now = System.currentTimeMillis()
                                    val deletedCount = oldLength - newLength
                                    if (!inDeletionMode || (now - lastEventTime) > BURST_TIMEOUT_MS) {
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
                                println("FeedbackDashboard: beforeDocumentChange failed: ${e.message}")
                            }
                        }
                    })
                }
            },
            project
        )
    }

    private fun registerCommandListener() {
        project.messageBus.connect().subscribe(
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

                        if (!inDeletionMode || (now - lastEventTime) > BURST_TIMEOUT_MS) {
                            if (inDeletionMode && currentBurstSize > 5) {
                                deletionBursts.add(currentBurstSize)
                            }
                            currentBurstSize = 0
                            inDeletionMode = true
                        }

                        val deletedCount = if (selectionModel.hasSelection()) selectionModel.selectedText?.length ?: 1 else 1
                        currentBurstSize += deletedCount
                        lastEventTime = now
                        println("DB DEBUG: Deleted $deletedCount chars, burst size=$currentBurstSize")
                    } catch (e: Throwable) {
                        println("FeedbackDashboard: beforeCommandFinished failed: ${e.message}")
                    }
                }
            }
        )
    }

    private fun registerKeyDispatcher() {
        val keyDispatcher = KeyEventDispatcher { keyEvent ->
            try {
                if (keyEvent.id == KeyEvent.KEY_PRESSED &&
                    keyEvent.keyCode == KeyEvent.VK_DELETE
                ) {
                    val currentEditor = FileEditorManager.getInstance(project).selectedTextEditor
                    if (currentEditor != null) {
                        val now = System.currentTimeMillis()
                        val selectionModel = currentEditor.selectionModel

                        if (!inDeletionMode || (now - lastEventTime) > BURST_TIMEOUT_MS) {
                            if (inDeletionMode && currentBurstSize > 5) {
                                deletionBursts.add(currentBurstSize)
                            }
                            currentBurstSize = 0
                            inDeletionMode = true
                        }

                        val deletedCount = if (selectionModel.hasSelection()) selectionModel.selectedText?.length ?: 1 else 1
                        currentBurstSize += deletedCount
                        lastEventTime = now
                        println("DB DEBUG: Delete key deleted $deletedCount chars, burst=$currentBurstSize")
                    }
                }
            } catch (e: Throwable) {
                println("FeedbackDashboard: keyDispatcher failed: ${e.message}")
            }
            false
        }
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyDispatcher)
    }

    private fun registerFileSwitchListener() {
        project.messageBus.connect().subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    try {
                        val now = System.currentTimeMillis()
                        if (event.oldFile != null && currentFileName.isNotEmpty()) {
                            println("CFC DEBUG: Left file '$currentFileName', spent ${(now - currentFileStartTime) / 1000}s")
                        }
                        if (event.newFile != null) {
                            currentFileName = event.newFile!!.nameWithoutExtension
                            currentFileStartTime = now
                            println("CFC DEBUG: Opened file '$currentFileName'")
                        }
                    } catch (e: Throwable) {
                        println("FeedbackDashboard: selectionChanged failed: ${e.message}")
                    }
                }
            }
        )
    }

    private fun registerCompileErrorListener() {
        project.messageBus.connect().subscribe(
            DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC,
            object : DaemonCodeAnalyzer.DaemonListener {
                override fun daemonFinished() {
                    ApplicationManager.getApplication().invokeLater {
                        try {
                            val currentEditor = FileEditorManager.getInstance(project)
                                .selectedTextEditor ?: return@invokeLater

                            val highlights = DaemonCodeAnalyzerImpl.getHighlights(
                                currentEditor.document,
                                HighlightSeverity.ERROR,
                                project
                            )


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
                            println("ER DEBUG: compile=$compileErrors, runtime=$runtimeErrors")
                        } catch (e: Throwable) {
                            println("FeedbackDashboard: daemonFinished failed: ${e.message}")
                        }
                    }
                }
            }
        )
    }

    private fun startFeedbackTimer() {
        val kolokvijumPocetak = System.currentTimeMillis()

        val feedbackTimer = Timer(20000) {
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
                        println("DB DEBUG: Final burst saved: $currentBurstSize")
                    }
                    inDeletionMode = false
                    currentBurstSize = 0
                }

                val nodesJson = if (currentSnapshot.isEmpty()) "[]"
                else currentSnapshot.joinToString(separator = "\", \"", prefix = "[\"", postfix = "\"]")

                val json = """
{
    "student_id": "$studentId",
    "timestamp": "${LocalDateTime.now()}",
    "window_start_sec": ${tCurrentSec - 20},
    "window_end_sec": $tCurrentSec,
    "t_current_min": ${tCurrentSec / 60},
    "t_total_min": 180,
    "metrics": {
        "keystroke_count": $keystrokeCount,
        "compile_errors": $compileErrors,
        "runtime_errors": $runtimeErrors,
        "time_on_class_seconds": $timeOnClassSec,
        "class_line_count": $currentLines,
        "delta_lines": $deltaL,
        "deletion_bursts": ${deletionBursts.sum()},
        "ast_nodes": $nodesJson,
        "cs_value": $csValue,
        "snapshot_history_size": ${snapshotHistory.size}
    }
}
    """.trimIndent()

                println("=== WINDOW ${tCurrentSec / 20} ===")
                println("KR: $keystrokeCount, DB: ${deletionBursts.joinToString()}, CS: $csValue")
                println("ER: compile=$compileErrors, runtime=$runtimeErrors")
                println("CFC: timeOnClass=$timeOnClassSec sec, deltaL=$deltaL")

                keystrokeCount = 0
                compileErrors = 0
                runtimeErrors = 0
                deletionBursts = mutableListOf()

                ApplicationManager.getApplication().executeOnPooledThread {
                    try {
                        val url = URL("http://157.180.37.247/api/data")
                        val conn = url.openConnection() as HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.connectTimeout = 5000
                        conn.readTimeout = 5000
                        conn.doOutput = true
                        conn.outputStream.use { it.write(json.toByteArray()) }
                        val responseCode = conn.responseCode
                        println("Feedback API: $responseCode | student: $studentId")
                        conn.disconnect()
                    } catch (e: Throwable) {
                        println("Greška pri slanju metrika: ${e.message}")
                    }
                }
            } catch (e: Throwable) {
                println("FeedbackDashboard: feedbackTimer tick failed: ${e.message}")
            }
        }

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
