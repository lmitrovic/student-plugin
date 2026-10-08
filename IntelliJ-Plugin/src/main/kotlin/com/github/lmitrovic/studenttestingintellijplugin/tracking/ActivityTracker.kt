package com.github.lmitrovic.studenttestingintellijplugin.tracking

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupEvent
import com.intellij.codeInsight.lookup.LookupListener
import com.intellij.codeInsight.lookup.LookupManagerListener
import com.intellij.execution.ExecutionListener
import com.intellij.execution.ExecutionManager
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionResult
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
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
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.wm.WindowManager
import raflms.trackingstub.api.TrackingStubService
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import javax.swing.Timer

class ActivityTracker(
    private val project: Project,
    private val trackingService: TrackingStubService,
    private val studentId: String,
    private val parentDisposable: Disposable
) {

    private val log = Logger.getInstance(ActivityTracker::class.java)

    fun start() {
        registerSafely("registerCodeChangeTracker") { registerCodeChangeTracker() }
        registerSafely("registerCompilationTracker") { registerCompilationTracker() }
        registerSafely("registerFocusTracker") { registerFocusTracker() }
        registerSafely("registerInactivityTracker") { registerInactivityTracker() }
        registerSafely("registerFileSwitchTracker") { registerFileSwitchTracker() }
        registerSafely("registerErrorTracker") { registerErrorTracker() }
        registerSafely("registerFileOpenCloseTracker") { registerFileOpenCloseTracker() }
        registerSafely("registerTextChangeAggTracker") { registerTextChangeAggTracker() }
        registerSafely("registerEditorActionTracker") { registerEditorActionTracker() }
        registerSafely("registerAutocompleteTracker") { registerAutocompleteTracker() }
        registerSafely("registerTestExecutionTracker") { registerTestExecutionTracker() }
        log.info("RAF tracking listeneri pokrenuti za: $studentId")
    }

    private fun registerSafely(name: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            log.warn("ActivityTracker: $name nije uspeo", e)
        }
    }

    private fun safeLog(block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            log.warn("ActivityTracker: logEvent nije uspeo: ${e.message}")
        }
    }

    private fun editorListener(register: (EditorFactoryEvent) -> Unit) {
        EditorFactory.getInstance().addEditorFactoryListener(
            object : EditorFactoryListener {
                override fun editorCreated(event: EditorFactoryEvent) = register(event)
            },
            parentDisposable
        )
    }

    private fun registerCodeChangeTracker() {
        val lastCodeChangeTime = mutableMapOf<String, Long>()
        editorListener { event ->
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
                        log.warn("ActivityTracker: CODE_CHANGE handler nije uspeo: ${e.message}")
                    }
                }
            }, parentDisposable)
        }
    }

    private fun registerCompilationTracker() {
        ApplicationManager.getApplication().messageBus.connect(parentDisposable).subscribe(
            AnActionListener.TOPIC,
            object : AnActionListener {
                override fun beforeActionPerformed(action: AnAction, event: AnActionEvent) {
                    try {
                        val actionId = ActionManager.getInstance().getId(action)
                        if (actionId != null && isCompilationAction(actionId)) {
                            safeLog { trackingService.logEvent("COMPILATION_STARTED", studentId, mapOf("actionId" to actionId)) }
                        }
                    } catch (e: Throwable) {
                        log.warn("ActivityTracker: beforeActionPerformed nije uspeo: ${e.message}")
                    }
                }

                override fun afterActionPerformed(action: AnAction, event: AnActionEvent, result: AnActionResult) {
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
                        log.warn("ActivityTracker: afterActionPerformed nije uspeo: ${e.message}")
                    }
                }
            }
        )
    }

    private fun registerFocusTracker() {
        val ideFrame = WindowManager.getInstance().getFrame(project) ?: return
        val listener = object : WindowFocusListener {
            override fun windowGainedFocus(e: WindowEvent) {
                safeLog { trackingService.logEvent("FOCUS_GAINED", studentId) }
            }
            override fun windowLostFocus(e: WindowEvent) {
                safeLog { trackingService.logEvent("FOCUS_LOST", studentId) }
            }
        }
        ideFrame.addWindowFocusListener(listener)
        Disposer.register(parentDisposable) { ideFrame.removeWindowFocusListener(listener) }
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
                log.warn("ActivityTracker: inactivityTimer nije uspeo: ${e.message}")
            }
        }
        startTimer(inactivityTimer)

        editorListener { event ->
            event.editor.document.addDocumentListener(object : DocumentListener {
                override fun documentChanged(e: DocumentEvent) {
                    try {
                        lastActivityTime = System.currentTimeMillis()
                        inactivityReported = false
                        val file = FileDocumentManager.getInstance().getFile(event.editor.document)
                        currentFileForInactivity = file?.name ?: "unknown"
                    } catch (e: Throwable) {
                        log.warn("ActivityTracker: inactivity documentChanged nije uspeo: ${e.message}")
                    }
                }
            }, parentDisposable)
        }
    }

    private fun registerFileSwitchTracker() {
        var fileOpenedAt = System.currentTimeMillis()

        project.messageBus.connect(parentDisposable).subscribe(
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
                        fileOpenedAt = now
                    } catch (e: Throwable) {
                        log.warn("ActivityTracker: FILE_SWITCH handler nije uspeo: ${e.message}")
                    }
                }
            }
        )
    }

    private fun registerErrorTracker() {
        val lastErrorTime = mutableMapOf<String, Long>()
        val errorThrottleMs = 5_000L

        project.messageBus.connect(parentDisposable).subscribe(
            DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC,
            object : DaemonCodeAnalyzer.DaemonListener {
                override fun daemonFinished() {
                    ApplicationManager.getApplication().invokeLater {
                        try {
                            val currentEditor = FileEditorManager.getInstance(project)
                                .selectedTextEditor ?: return@invokeLater

                            val vFile = FileDocumentManager.getInstance()
                                .getFile(currentEditor.document)
                            // RISK-14: relativna putanja; RISK-15: tip greske, ne tekst poruke
                            val fileName = vFile?.let { relativePath(it) } ?: "unknown"

                            EditorErrors.errorHighlights(project, currentEditor.document).forEach { highlight ->
                                val throttleKey = "$fileName:${highlight.startOffset}"
                                val now = System.currentTimeMillis()
                                val last = lastErrorTime[throttleKey] ?: 0L
                                if (now - last >= errorThrottleMs) {
                                    lastErrorTime[throttleKey] = now
                                    val line = currentEditor.document.getLineNumber(highlight.startOffset) + 1
                                    val errorCategory = highlight.description
                                        ?.let { categorizeError(it) } ?: "UNKNOWN"
                                    safeLog {
                                        trackingService.logEvent(
                                            "ERROR_DETECTED", studentId,
                                            mapOf(
                                                "file" to fileName,
                                                "errorCategory" to errorCategory,
                                                "line" to line
                                            )
                                        )
                                    }
                                }
                            }
                        } catch (e: Throwable) {
                            log.warn("ActivityTracker: ERROR_DETECTED handler nije uspeo: ${e.message}")
                        }
                    }
                }
            }
        )
    }

    private fun registerFileOpenCloseTracker() {
        val fileOpenTimes = mutableMapOf<String, Long>()

        project.messageBus.connect(parentDisposable).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                    fileOpenTimes[file.path] = System.currentTimeMillis()
                    safeLog {
                        trackingService.logEvent(
                            "FILE_OPENED", studentId,
                            mapOf("filePath" to relativePath(file), "fileType" to (file.extension ?: "unknown"))
                        )
                    }
                }

                override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
                    val openedAt = fileOpenTimes.remove(file.path) ?: return
                    val timeOpenSeconds = (System.currentTimeMillis() - openedAt) / 1000
                    safeLog {
                        trackingService.logEvent(
                            "FILE_CLOSED", studentId,
                            mapOf("filePath" to relativePath(file), "timeOpenSeconds" to timeOpenSeconds)
                        )
                    }
                }
            }
        )
    }

    private fun registerTextChangeAggTracker() {
        data class FileStats(
            var linesAdded: Int = 0,
            var linesDeleted: Int = 0,
            var charsAdded: Int = 0,
            var charsDeleted: Int = 0
        )

        val pending = mutableMapOf<String, FileStats>()

        val flushTimer = Timer(7_000) {
            try {
                val snapshot = pending.toMap()
                pending.clear()
                snapshot.forEach { (filePath, stats) ->
                    if (stats.charsAdded > 0 || stats.charsDeleted > 0 || stats.linesAdded > 0 || stats.linesDeleted > 0) {
                        safeLog {
                            trackingService.logEvent(
                                "TEXT_CHANGED_AGG", studentId,
                                mapOf(
                                    "filePath" to filePath,
                                    "linesAdded" to stats.linesAdded,
                                    "linesDeleted" to stats.linesDeleted,
                                    "charsAdded" to stats.charsAdded,
                                    "charsDeleted" to stats.charsDeleted
                                )
                            )
                        }
                    }
                }
            } catch (e: Throwable) {
                log.warn("ActivityTracker: TEXT_CHANGED_AGG flush nije uspeo: ${e.message}")
            }
        }
        startTimer(flushTimer)

        editorListener { event ->
            val editor = event.editor
            editor.document.addDocumentListener(object : DocumentListener {
                override fun documentChanged(e: DocumentEvent) {
                    try {
                        val file = FileDocumentManager.getInstance().getFile(editor.document)
                        val filePath = file?.let { relativePath(it) } ?: "unknown"
                        val stats = pending.getOrPut(filePath) { FileStats() }
                        val oldText = e.oldFragment.toString()
                        val newText = e.newFragment.toString()
                        stats.charsAdded += maxOf(0, newText.length - oldText.length)
                        stats.charsDeleted += maxOf(0, oldText.length - newText.length)
                        stats.linesAdded += newText.count { it == '\n' }
                        stats.linesDeleted += oldText.count { it == '\n' }
                    } catch (e: Throwable) {
                        log.warn("ActivityTracker: TEXT_CHANGED_AGG documentChanged nije uspeo: ${e.message}")
                    }
                }
            }, parentDisposable)
        }
    }

    private fun registerEditorActionTracker() {
        var consecutiveUndos = 0
        var lastActionId = ""
        var pendingCopyLength = 0
        // RISK-13 fix: ne citamo sistemski clipboard (sadrzaj van IDE-a).
        // Duzinu paste-a merimo iz razlike duzine dokumenta pre i posle akcije.
        var docLengthBeforePaste = 0

        ApplicationManager.getApplication().messageBus.connect(parentDisposable).subscribe(
            AnActionListener.TOPIC,
            object : AnActionListener {
                override fun beforeActionPerformed(action: AnAction, event: AnActionEvent) {
                    try {
                        val actionId = ActionManager.getInstance().getId(action) ?: return
                        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
                        when (actionId) {
                            "\$Copy" -> pendingCopyLength = editor.selectionModel.selectedText?.length ?: 0
                            "\$Paste" -> docLengthBeforePaste = editor.document.textLength
                        }
                    } catch (e: Throwable) {
                        log.warn("ActivityTracker: editorAction beforeActionPerformed nije uspeo: ${e.message}")
                    }
                }

                override fun afterActionPerformed(action: AnAction, event: AnActionEvent, result: AnActionResult) {
                    if (!result.isPerformed) return
                    try {
                        val actionId = ActionManager.getInstance().getId(action) ?: return
                        val editor = event.getData(CommonDataKeys.EDITOR)
                        val file = editor?.let { FileDocumentManager.getInstance().getFile(it.document) }
                        val filePath = file?.let { relativePath(it) } ?: "unknown"

                        when {
                            actionId == "\$Copy" -> safeLog {
                                trackingService.logEvent("COPY_ACTION", studentId, mapOf("filePath" to filePath, "length" to pendingCopyLength))
                            }
                            actionId == "\$Paste" -> {
                                val pasteLength = maxOf(0, (editor?.document?.textLength ?: docLengthBeforePaste) - docLengthBeforePaste)
                                safeLog {
                                    trackingService.logEvent("PASTE_ACTION", studentId, mapOf("filePath" to filePath, "length" to pasteLength))
                                }
                            }
                            actionId == "\$Undo" -> {
                                consecutiveUndos = if (lastActionId == "\$Undo") consecutiveUndos + 1 else 1
                                safeLog {
                                    trackingService.logEvent(
                                        "UNDO_ACTION", studentId,
                                        mapOf("filePath" to filePath, "consecutiveUndos" to consecutiveUndos)
                                    )
                                }
                            }
                            isSearchAction(actionId) -> safeLog {
                                trackingService.logEvent(
                                    "SEARCH_USED", studentId,
                                    mapOf("searchType" to actionId, "queryLength" to 0)
                                )
                            }
                        }
                        lastActionId = actionId
                    } catch (e: Throwable) {
                        log.warn("ActivityTracker: editorAction afterActionPerformed nije uspeo: ${e.message}")
                    }
                }
            }
        )
    }

    private fun registerAutocompleteTracker() {
        project.messageBus.connect(parentDisposable).subscribe(
            LookupManagerListener.TOPIC,
            object : LookupManagerListener {
                override fun activeLookupChanged(oldLookup: Lookup?, newLookup: Lookup?) {
                    newLookup?.addLookupListener(object : LookupListener {
                        override fun itemSelected(event: LookupEvent) {
                            try {
                                val item = event.item ?: return
                                val file = FileDocumentManager.getInstance().getFile(newLookup.editor.document)
                                val filePath = file?.let { relativePath(it) } ?: "unknown"
                                // RISK-15 fix: saljemo tip dopune (METHOD/FIELD/CLASS/KEYWORD/OTHER),
                                // ne sam string - koji bi otkrio kod studenta
                                val completionType = item.psiElement?.let { psi ->
                                    when (psi::class.simpleName) {
                                        "PsiMethod", "KtFunction" -> "METHOD"
                                        "PsiField", "KtProperty" -> "FIELD"
                                        "PsiClass", "KtClass" -> "CLASS"
                                        "PsiKeyword" -> "KEYWORD"
                                        else -> "OTHER"
                                    }
                                } ?: "OTHER"
                                safeLog {
                                    trackingService.logEvent(
                                        "AUTOCOMPLETE_USED", studentId,
                                        mapOf("filePath" to filePath, "completionType" to completionType)
                                    )
                                }
                            } catch (e: Throwable) {
                                log.warn("ActivityTracker: AUTOCOMPLETE_USED handler nije uspeo: ${e.message}")
                            }
                        }
                    })
                }
            }
        )
    }

    private fun registerTestExecutionTracker() {
        project.messageBus.connect(parentDisposable).subscribe(
            ExecutionManager.EXECUTION_TOPIC,
            object : ExecutionListener {
                override fun processStarted(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
                    safeLog {
                        trackingService.logEvent(
                            "TEST_EXECUTION_STARTED", studentId,
                            mapOf("executorId" to executorId, "configName" to env.runProfile.name)
                        )
                    }
                }

                override fun processTerminated(
                    executorId: String,
                    env: ExecutionEnvironment,
                    handler: ProcessHandler,
                    exitCode: Int
                ) {
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
                }
            }
        )
    }

    private fun startTimer(timer: Timer) {
        Disposer.register(parentDisposable) { timer.stop() }
        timer.start()
    }

    // RISK-15 fix: kategorise greske bez slanja teksta poruke (koji sadrzi imena promenljivih).
    private fun categorizeError(message: String): String = when {
        message.contains("cannot find symbol", ignoreCase = true) ||
        message.contains("unresolved reference", ignoreCase = true) -> "UNRESOLVED_SYMBOL"
        message.contains("incompatible types", ignoreCase = true) ||
        message.contains("type mismatch", ignoreCase = true) -> "TYPE_ERROR"
        message.contains("';' expected", ignoreCase = true) ||
        message.contains("syntax error", ignoreCase = true) -> "SYNTAX_ERROR"
        message.contains("null pointer", ignoreCase = true) ||
        message.contains("nullable", ignoreCase = true) -> "NULL_SAFETY"
        message.contains("unused", ignoreCase = true) -> "UNUSED_SYMBOL"
        else -> "OTHER"
    }

    // RISK-14 fix: vraca putanju relativnu u odnosu na koren projekta umesto apsolutne.
    // Apsolutna putanja (/Users/marko.petrovic/...) otkriva korisnicko ime na privatnom racunaru.
    private fun relativePath(file: VirtualFile): String {
        val projectBase = project.baseDir ?: return file.name
        return VfsUtilCore.getRelativePath(file, projectBase) ?: file.name
    }

    private fun isSearchAction(actionId: String): Boolean {
        return actionId in setOf("Find", "FindNext", "FindPrevious", "FindInPath", "Replace", "ReplaceInPath")
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
