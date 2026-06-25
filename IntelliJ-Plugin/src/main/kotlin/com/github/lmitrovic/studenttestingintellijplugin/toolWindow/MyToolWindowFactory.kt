package com.github.lmitrovic.studenttestingintellijplugin.toolWindow

import com.github.lmitrovic.studenttestingintellijplugin.MyBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import raflms.studentstub.api.StudentStubService
import raflms.studentstub.api.datamodel.TestWithAssignments
import raflms.studentstub.config.ConfigFactory
import java.awt.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import javax.swing.*
import javax.swing.border.EmptyBorder
import kotlin.properties.Delegates
import raflms.trackingstub.api.TrackingStubService
import raflms.trackingstub.config.ConfigFactory as TrackingConfigFactory
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.event.EditorEventMulticaster
import kotlin.text.RegexOption


class MyToolWindowFactory : ToolWindowFactory {

    private lateinit var studentsFirstNameTF: JTextField
    private lateinit var studentsLastNameTF: JTextField
    private lateinit var studentsStudyProgramTF: JTextField
    private lateinit var studentsIndexNumberTF: JTextField
    private lateinit var studentsStartYearTF: JTextField
    private lateinit var studentsTaskGroupTF: JTextField
    private lateinit var studentsTermCB: JComboBox<Any>
    private lateinit var classroomNameTF: JTextField
    private lateinit var outputArea: JTextArea
    private lateinit var cloningReportArea: JTextArea
    private lateinit var testGroupCB: JComboBox<Any>
    private lateinit var subjectCB: JComboBox<Any>

    private lateinit var mainPanel: JPanel
    private lateinit var initialPanel: JPanel
    private lateinit var formPanel: JPanel
    private lateinit var studentEnrollmentInfoPanel: JPanel
    private lateinit var studentsTestSpecificPanel: JPanel
    private lateinit var fieldsPanel: JPanel
    private lateinit var afterClonedPanel: JPanel
    private lateinit var contentFactory: ContentFactory
    private lateinit var trackingService: TrackingStubService

    private var isSuccess by Delegates.notNull<Boolean>()
    private var currentStudentId: String = ""


    init {
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
        isSuccess = false
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {

        val studentService = StudentStubService(ConfigFactory.createConfig())
        trackingService = TrackingStubService(TrackingConfigFactory.createConfig())

        /**
         * LOGOVANJE KLIKTANJA POCETAK
         */
        val editorManager = FileEditorManager.getInstance(project)

        project.messageBus.connect()
            .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
                override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                    val editor = editorManager.selectedTextEditor ?: return

                    editor.caretModel.addCaretListener(object : CaretListener {
                        var lastLine = editor.caretModel.logicalPosition.line
                        var lastTime = System.currentTimeMillis()

                        override fun caretPositionChanged(event: CaretEvent) {
                            val currentLine = event.newPosition.line
                            val currentTime = System.currentTimeMillis()

                            if (currentLine != lastLine) {
                                lastLine = currentLine
                                lastTime = currentTime
                            }
                        }
                    })
                }
            })

        /**
         * LOGOVANJE KLIKTANJA KRAJ
         */

        this.contentFactory = ContentFactory.getInstance()

        // Create a panel to hold all components
        mainPanel = JPanel(BorderLayout())

        this.afterClonedPanel = JPanel()
        afterClonedPanel.layout = BoxLayout(afterClonedPanel, BoxLayout.Y_AXIS)
        afterClonedPanel.border = BorderFactory.createEmptyBorder(10, 20, 10, 20)

        this.formPanel = JPanel(BorderLayout())
        formPanel.border = BorderFactory.createEmptyBorder(10, 20, 10, 20)

        this.fieldsPanel = JPanel(GridLayout(0, 1))

        this.initialPanel = JPanel()
        initialPanel.layout = BoxLayout(initialPanel, BoxLayout.Y_AXIS)
        initialPanel.border = BorderFactory.createEmptyBorder(10, 20, 10, 20)

        // Add input fields labels
        studentsFirstNameTF = JTextField(20)
        studentsFirstNameTF.preferredSize = Dimension(300, 24)

        //studentsFirstNameTF.text = remoteStudentObject2.firstName
        fieldsPanel.add(makeField("First name:", studentsFirstNameTF))


        studentsLastNameTF = JTextField(20)
        studentsLastNameTF.preferredSize = Dimension(300, 24)
        // studentsLastNameTF.text = remoteStudentObject2.lastName
        fieldsPanel.add(makeField("Last name:", studentsLastNameTF))

        this.studentEnrollmentInfoPanel = JPanel(GridLayout(0, 4))

        studentsStudyProgramTF = JTextField(20)
        studentsStudyProgramTF.preferredSize = Dimension(50, 24)
        //studentsStudyProgramTF.text = remoteStudentObject2.studyProgramShort
        studentEnrollmentInfoPanel.add(makeField("Program:", studentsStudyProgramTF))

        studentsIndexNumberTF = JTextField(20)
        studentsIndexNumberTF.preferredSize = Dimension(70, 24)
        //studentsIndexNumberTF.text = remoteStudentObject2.indexNumber.toString()
        studentEnrollmentInfoPanel.add(makeSmallField("Num:", studentsIndexNumberTF))

        studentsStartYearTF = JTextField(20)
        studentsStartYearTF.preferredSize = Dimension(70, 24)
        //studentsStartYearTF.text = remoteStudentObject2.startYear
        studentEnrollmentInfoPanel.add(makeSmallField("Year:", studentsStartYearTF))

        studentsTaskGroupTF = JTextField(20)
        studentsTaskGroupTF.preferredSize = Dimension(70, 24)
        studentEnrollmentInfoPanel.add(makeSmallField("Group:", studentsTaskGroupTF))

        fieldsPanel.add(studentEnrollmentInfoPanel)

        this.studentsTestSpecificPanel = JPanel(GridLayout(0, 4))

        classroomNameTF = JTextField(20)
        classroomNameTF.preferredSize = Dimension(300, 24)

        val subjectLabelText = JLabel("Assignment:")
        subjectLabelText.minimumSize = Dimension(20, subjectLabelText.minimumSize.height)
        subjectLabelText.preferredSize = Dimension(80, subjectLabelText.preferredSize.height)

        val allTestsWithAssigmentsData: MutableList<TestWithAssignments> = studentService.allTestsWithAssigmentsData

        val subjectChoices = allTestsWithAssigmentsData.map { it.testName }.toTypedArray()
        testGroupCB = JComboBox()
        studentsTermCB = JComboBox()
        subjectCB = JComboBox(subjectChoices)
        subjectCB.isEnabled = true

        fun updateGroupsAndTerms() {
            val selectedTestName = subjectCB.selectedItem as? String ?: return
            val selectedTest = allTestsWithAssigmentsData
                .find { it.testName == selectedTestName } ?: return
            val assignments = selectedTest.assigments ?: return
            testGroupCB.model = DefaultComboBoxModel(assignments.mapNotNull { it.group }.distinct().toTypedArray())
            studentsTermCB.model = DefaultComboBoxModel(assignments.mapNotNull { it.term }.distinct().toTypedArray())
        }

        subjectCB.addActionListener { updateGroupsAndTerms() }
        updateGroupsAndTerms()

        studentsTestSpecificPanel.add(subjectLabelText)
        studentsTestSpecificPanel.add(subjectCB)
        studentsTestSpecificPanel.add(testGroupCB)
        studentsTestSpecificPanel.add(studentsTermCB)

        fieldsPanel.add(studentsTestSpecificPanel)

        outputArea = JTextArea();
        cloningReportArea = JTextArea();

        val commitButton = JButton("Commit")
        val finalSubmissionButton = JButton("Final Submission")

        // Spinner dok se ucitava
        val progressBar = JProgressBar().apply {
            isIndeterminate = true
            isVisible = false
        }

        val signInButton = JButton("Begin")

        signInButton.addActionListener {

            //kreiraj privremeni download folder ako ne postoji, ako postoji obrisi sve iz njega
            val downloadPath = Paths.get(System.getProperty("user.home"), MyBundle.downloadFolder)
            if (Files.exists(downloadPath)) {
                downloadPath.toFile().listFiles()?.forEach { it.deleteRecursively() }
            } else {
                Files.createDirectory(downloadPath)
            }

            // Run the clone operation in a worker thread to avoid blocking the UI.
            ApplicationManager.getApplication().executeOnPooledThread {

                isSuccess = studentService.startAssigment(
                    studentsIndexNumberTF.text.toInt(),
                    studentsStartYearTF.text,
                    studentsStudyProgramTF.text,
                    studentsTaskGroupTF.text,
                    studentsFirstNameTF.text,
                    studentsLastNameTF.text,
                    subjectCB.selectedItem?.toString(),
                    testGroupCB.selectedItem?.toString(),
                    studentsTermCB.selectedItem?.toString(),
                    Paths.get(System.getProperty("user.home"), MyBundle.downloadFolder).toString()
                )

                // `invokeLater` schedules this task to run on the Event Dispatch Thread (EDT).
                ApplicationManager.getApplication().invokeLater(label@{
                    if (isSuccess) {
                        val studentId =
                            "${studentsStudyProgramTF.text}-${studentsIndexNumberTF.text}-${studentsStartYearTF.text}"
                        val taskId =
                            "${subjectCB.selectedItem}-${testGroupCB.selectedItem}-${studentsTermCB.selectedItem}"
                        trackingService.startTracking(studentId, taskId)
                        currentStudentId = studentId
                        println("=== TRACKING STARTED: $studentId, $taskId ===")

                        // ──────────────────────────────────────────────────────────────────────────
// RAF TRACKING LISTENERS — sends events to activitytrackingapi via trackingstub
// ──────────────────────────────────────────────────────────────────────────


                        val lastCodeChangeTime = mutableMapOf<String, Long>()
// 1. CODE CHANGE TRACKER
                        com.intellij.openapi.editor.EditorFactory.getInstance()
                            .addEditorFactoryListener(
                                object : com.intellij.openapi.editor.event.EditorFactoryListener {
                                    override fun editorCreated(event: com.intellij.openapi.editor.event.EditorFactoryEvent) {
                                        val editor = event.editor
                                        editor.document.addDocumentListener(
                                            object : com.intellij.openapi.editor.event.DocumentListener {
                                                override fun documentChanged(e: com.intellij.openapi.editor.event.DocumentEvent) {
                                                    val file = com.intellij.openapi.fileEditor.FileDocumentManager
                                                        .getInstance().getFile(editor.document)
                                                    val charsAdded = e.newLength - e.oldLength
                                                    val fileName = file?.name ?: "unknown"
                                                    val now = System.currentTimeMillis()
                                                    val last = lastCodeChangeTime[fileName] ?: 0L
                                                    if (now - last >= 500) {
                                                        lastCodeChangeTime[fileName] = now
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
                                            }
                                        )
                                    }
                                },
                                project
                            )

// 2. COMPILATION TRACKER
                        project.messageBus.connect().subscribe(
                            com.intellij.openapi.actionSystem.ex.AnActionListener.TOPIC,
                            object : com.intellij.openapi.actionSystem.ex.AnActionListener {
                                override fun beforeActionPerformed(
                                    action: com.intellij.openapi.actionSystem.AnAction,
                                    event: com.intellij.openapi.actionSystem.AnActionEvent
                                ) {
                                    val actionId = com.intellij.openapi.actionSystem.ActionManager.getInstance().getId(action)
                                    if (actionId != null && isCompilationAction(actionId)) {
                                        trackingService.logEvent(
                                            "COMPILATION_STARTED", studentId,
                                            mapOf("actionId" to actionId)
                                        )
                                    }
                                }

                                override fun afterActionPerformed(
                                    action: com.intellij.openapi.actionSystem.AnAction,
                                    event: com.intellij.openapi.actionSystem.AnActionEvent,
                                    result: com.intellij.openapi.actionSystem.AnActionResult
                                ) {
                                    val actionId = com.intellij.openapi.actionSystem.ActionManager.getInstance().getId(action)
                                    if (actionId != null && isCompilationAction(actionId)) {
                                        trackingService.logEvent(
                                            "COMPILATION_FINISHED", studentId,
                                            mapOf(
                                                "actionId" to actionId,
                                                "success" to result.isPerformed
                                            )
                                        )
                                    }
                                }
                            }
                        )

// 3. FOCUS TRACKER
                        val ideFrame = com.intellij.openapi.wm.WindowManager.getInstance().getFrame(project)
                        ideFrame?.addWindowFocusListener(object : java.awt.event.WindowFocusListener {
                            override fun windowGainedFocus(e: java.awt.event.WindowEvent) {
                                trackingService.logEvent("FOCUS_GAINED", studentId)
                            }
                            override fun windowLostFocus(e: java.awt.event.WindowEvent) {
                                trackingService.logEvent("FOCUS_LOST", studentId)
                            }
                        })

                        // 4. INACTIVITY TRACKER
                        val inactivityThresholdMs = 60_000L // 60 seconds
                        var lastActivityTime = System.currentTimeMillis()
                        var inactivityReported = false
                        var currentFileForInactivity = ""

// Update activity on any code change
                        val inactivityTimer = javax.swing.Timer(10_000) {
                            val now = System.currentTimeMillis()
                            val inactiveDuration = now - lastActivityTime
                            if (inactiveDuration >= inactivityThresholdMs && !inactivityReported) {
                                inactivityReported = true
                                trackingService.logEvent(
                                    "INACTIVITY", studentId,
                                    mapOf(
                                        "durationSeconds" to inactiveDuration / 1000,
                                        "file" to currentFileForInactivity
                                    )
                                )
                            }
                        }
                        inactivityTimer.start()

// Reset inactivity on code change — subscribe to document changes
                        com.intellij.openapi.editor.EditorFactory.getInstance()
                            .addEditorFactoryListener(
                                object : com.intellij.openapi.editor.event.EditorFactoryListener {
                                    override fun editorCreated(event: com.intellij.openapi.editor.event.EditorFactoryEvent) {
                                        event.editor.document.addDocumentListener(
                                            object : com.intellij.openapi.editor.event.DocumentListener {
                                                override fun documentChanged(e: com.intellij.openapi.editor.event.DocumentEvent) {
                                                    lastActivityTime = System.currentTimeMillis()
                                                    inactivityReported = false
                                                    val file = com.intellij.openapi.fileEditor.FileDocumentManager
                                                        .getInstance().getFile(event.editor.document)
                                                    currentFileForInactivity = file?.name ?: "unknown"
                                                }
                                            }
                                        )
                                    }
                                },
                                project
                            )

// 5. FILE SWITCH TRACKER
                        var currentOpenFile = ""
                        var fileOpenedAt = System.currentTimeMillis()

                        project.messageBus.connect().subscribe(
                            com.intellij.openapi.fileEditor.FileEditorManagerListener.FILE_EDITOR_MANAGER,
                            object : com.intellij.openapi.fileEditor.FileEditorManagerListener {
                                override fun selectionChanged(event: com.intellij.openapi.fileEditor.FileEditorManagerEvent) {
                                    val now = System.currentTimeMillis()
                                    val newFile = event.newFile?.name ?: return
                                    val oldFile = event.oldFile?.name ?: ""

                                    if (oldFile.isNotEmpty() && newFile != oldFile) {
                                        val timeSpentMs = now - fileOpenedAt
                                        trackingService.logEvent(
                                            "FILE_SWITCH", studentId,
                                            mapOf(
                                                "fromFile" to oldFile,
                                                "toFile" to newFile,
                                                "timeSpentMs" to timeSpentMs
                                            )
                                        )
                                    }

                                    currentOpenFile = newFile
                                    fileOpenedAt = now

                                    // Reset inactivity on file switch too
                                    lastActivityTime = now
                                    inactivityReported = false
                                    currentFileForInactivity = newFile
                                }
                            }
                        )

                        // 6. ERROR DETECTED TRACKER
                        val lastErrorTime = mutableMapOf<String, Long>()
                        val errorThrottleMs = 5_000L // don't repeat same error within 5 seconds

                        project.messageBus.connect().subscribe(
                            com.intellij.codeInsight.daemon.DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC,
                            object : com.intellij.codeInsight.daemon.DaemonCodeAnalyzer.DaemonListener {
                                override fun daemonFinished() {
                                    ApplicationManager.getApplication().invokeLater {
                                        val currentEditor = FileEditorManager.getInstance(project)
                                            .selectedTextEditor ?: return@invokeLater

                                        val file = FileDocumentManager.getInstance()
                                            .getFile(currentEditor.document)?.name ?: "unknown"

                                        val highlights = com.intellij.codeInsight.daemon.impl
                                            .DaemonCodeAnalyzerImpl.getHighlights(
                                                currentEditor.document,
                                                com.intellij.lang.annotation.HighlightSeverity.ERROR,
                                                project
                                            )

                                        highlights.forEach { highlight ->
                                            val errorMessage = highlight.description ?: return@forEach
                                            val throttleKey = "$file:$errorMessage"
                                            val now = System.currentTimeMillis()
                                            val last = lastErrorTime[throttleKey] ?: 0L

                                            if (now - last >= errorThrottleMs) {
                                                lastErrorTime[throttleKey] = now
                                                trackingService.logEvent(
                                                    "ERROR_DETECTED", studentId,
                                                    mapOf(
                                                        "file" to file,
                                                        "errorMessage" to errorMessage,
                                                        "line" to (highlight.actualStartOffset).toDouble()
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        )

// 7. TEST EXECUTION TRACKER
                        project.messageBus.connect().subscribe(
                            com.intellij.execution.ExecutionManager.EXECUTION_TOPIC,
                            object : com.intellij.execution.ExecutionListener {
                                override fun processStarted(
                                    executorId: String,
                                    env: com.intellij.execution.runners.ExecutionEnvironment,
                                    handler: com.intellij.execution.process.ProcessHandler
                                ) {
                                    trackingService.logEvent(
                                        "TEST_EXECUTION_STARTED", studentId,
                                        mapOf(
                                            "executorId" to executorId,
                                            "configName" to env.runProfile.name
                                        )
                                    )
                                }

                                override fun processTerminated(
                                    executorId: String,
                                    env: com.intellij.execution.runners.ExecutionEnvironment,
                                    handler: com.intellij.execution.process.ProcessHandler,
                                    exitCode: Int
                                ) {
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
                        )

                        println("=== RAF TRACKING LISTENERS STARTED for: $studentId ===")

                        // ******************** ZARKO JE OVO DODAO ZA LISTENERE ZA ISPIT
// ──────────────────────────────────────────────────────────────────────────
// REAL-TIME FEEDBACK DASHBOARD
// ──────────────────────────────────────────────────────────────────────────

// Brojači
                        var keystrokeCount = 0
                        var deletionBursts = mutableListOf<Int>()
                        var currentBurstSize = 0
                        var inDeletionMode = false
                        var lastEventTime = 0L
                        val BURST_TIMEOUT_MS = 2000L
                        var classLines = 0
                        var compileErrors = 0
                        var runtimeErrors = 0
                        var lastLinesCount = 0
                        var currentFileStartTime = System.currentTimeMillis()
                        var currentFileName = ""

                        val snapshotHistory = mutableListOf<Set<String>>()

// 1. DOCUMENT LISTENER — za keystroke brojanje i brisanje
                        com.intellij.openapi.editor.EditorFactory.getInstance()
                            .addEditorFactoryListener(
                                object : com.intellij.openapi.editor.event.EditorFactoryListener {
                                    override fun editorCreated(
                                        event: com.intellij.openapi.editor.event.EditorFactoryEvent
                                    ) {
                                        event.editor.document.addDocumentListener(
                                            object : com.intellij.openapi.editor.event.DocumentListener {
                                                override fun documentChanged(
                                                    event: com.intellij.openapi.editor.event.DocumentEvent
                                                ) {
                                                    classLines = event.document.lineCount
                                                    val added = event.newLength - event.oldLength
                                                    if (added > 0) {
                                                        keystrokeCount += added
                                                        println("KR DEBUG: +$added chars, total=$keystrokeCount")
                                                    }
                                                }

                                                override fun beforeDocumentChange(
                                                    event: com.intellij.openapi.editor.event.DocumentEvent
                                                ) {
                                                    val oldLength = event.oldLength
                                                    val newLength = event.newLength
                                                    if (oldLength > newLength) {
                                                        val now = System.currentTimeMillis()
                                                        val deletedCount = oldLength - newLength
                                                        if (!inDeletionMode ||
                                                            (now - lastEventTime) > BURST_TIMEOUT_MS) {
                                                            if (inDeletionMode &&
                                                                currentBurstSize > 5) {
                                                                deletionBursts.add(currentBurstSize)
                                                            }
                                                            currentBurstSize = 0
                                                            inDeletionMode = true
                                                        }
                                                        currentBurstSize += deletedCount
                                                        lastEventTime = now
                                                    }
                                                }
                                            }
                                        )
                                    }
                                },
                                project
                            )

// 2. COMMAND LISTENER — za backspace/delete brisanje
                        project.messageBus.connect().subscribe(
                            com.intellij.openapi.command.CommandListener.TOPIC,
                            object : com.intellij.openapi.command.CommandListener {
                                override fun beforeCommandFinished(event: com.intellij.openapi.command.CommandEvent) {
                                    val commandName = event.commandName ?: return
                                    if (commandName.contains("BackSpace", ignoreCase = true) ||
                                        commandName.contains("Delete", ignoreCase = true)
                                    ) {
                                        val currentEditor =
                                            FileEditorManager.getInstance(project).selectedTextEditor ?: return
                                        val now = System.currentTimeMillis()
                                        val selectionModel = currentEditor.selectionModel

                                        if (!inDeletionMode || (now - lastEventTime) > BURST_TIMEOUT_MS) {
                                            if (inDeletionMode && currentBurstSize > 0 && currentBurstSize > 5) {
                                                deletionBursts.add(currentBurstSize)
                                            }
                                            currentBurstSize = 0
                                            inDeletionMode = true
                                        }

                                        val deletedCount = if (selectionModel.hasSelection()) {
                                            selectionModel.selectedText?.length ?: 1
                                        } else {
                                            1
                                        }

                                        currentBurstSize += deletedCount
                                        lastEventTime = now
                                        println("DB DEBUG: Deleted $deletedCount chars, burst size=$currentBurstSize")
                                    }
                                }
                            }
                        )

// 3. KEY DISPATCHER — za Delete taster
                        val keyDispatcher = java.awt.KeyEventDispatcher { keyEvent ->
                            if (keyEvent.id == java.awt.event.KeyEvent.KEY_PRESSED &&
                                keyEvent.keyCode == java.awt.event.KeyEvent.VK_DELETE
                            ) {
                                val currentEditor = FileEditorManager.getInstance(project).selectedTextEditor
                                if (currentEditor != null) {
                                    val now = System.currentTimeMillis()
                                    val selectionModel = currentEditor.selectionModel

                                    if (!inDeletionMode || (now - lastEventTime) > BURST_TIMEOUT_MS) {
                                        if (inDeletionMode && currentBurstSize > 0 && currentBurstSize > 5) {
                                            deletionBursts.add(currentBurstSize)
                                        }
                                        currentBurstSize = 0
                                        inDeletionMode = true
                                    }

                                    val deletedCount = if (selectionModel.hasSelection()) {
                                        selectionModel.selectedText?.length ?: 1
                                    } else {
                                        1
                                    }

                                    currentBurstSize += deletedCount
                                    lastEventTime = now
                                    println("DB DEBUG: Delete key deleted $deletedCount chars, burst=$currentBurstSize")
                                }
                            }
                            false
                        }

                        java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager()
                            .addKeyEventDispatcher(keyDispatcher)

// 4. FILE EDITOR MANAGER LISTENER — za CFC (vreme na klasi)
                        project.messageBus.connect().subscribe(
                            com.intellij.openapi.fileEditor.FileEditorManagerListener.FILE_EDITOR_MANAGER,
                            object : com.intellij.openapi.fileEditor.FileEditorManagerListener {
                                override fun selectionChanged(event: com.intellij.openapi.fileEditor.FileEditorManagerEvent) {
                                    val newFile = event.newFile
                                    val oldFile = event.oldFile
                                    val now = System.currentTimeMillis()

                                    if (oldFile != null && currentFileName.isNotEmpty()) {
                                        val timeSpent = now - currentFileStartTime
                                        println("CFC DEBUG: Left file '$currentFileName', spent ${timeSpent / 1000}s")
                                    }

                                    if (newFile != null) {
                                        currentFileName = newFile.nameWithoutExtension
                                        currentFileStartTime = now
                                        println("CFC DEBUG: Opened file '$currentFileName'")
                                    }
                                }
                            }
                        )

                        // 5. FUNKCIJA ZA PRAVLJENJE AST SNAPSHOT-A
                        fun getAstSnapshot(): Set<String> {
                            val snapEditor = FileEditorManager.getInstance(project).selectedTextEditor
                            val text = snapEditor?.document?.text ?: return emptySet()

                            val nodes = mutableSetOf<String>()
                            val lines = text.lines()
                            var inComment = false
                            var inString = false

                            for (line in lines) {
                                val trimmed = line.trim()
                                if (trimmed.isEmpty()) continue
                                if (trimmed.startsWith("//")) continue
                                if (trimmed.startsWith("/*")) inComment = true
                                if (inComment && trimmed.contains("*/")) {
                                    inComment = false; continue
                                }
                                if (inComment) continue

                                if (trimmed.contains("\"") && !trimmed.startsWith("\"")) {
                                    if (inString) inString = false
                                    else if (trimmed.count { it == '"' } % 2 == 1) inString = true
                                }
                                if (inString) continue

                                when {
                                    Regex("""^\s*(public|private|protected)?\s*(class|interface|enum)\s+(\w+)""").containsMatchIn(
                                        trimmed
                                    ) ->
                                        nodes.add("ClassDeclaration")

                                    Regex("""^\s*(public|private|protected)?\s*(static)?\s*(synchronized)?\s*(\w+)\s+(\w+)\s*\(""").containsMatchIn(
                                        trimmed
                                    ) ->
                                        nodes.add("MethodDeclaration")

                                    Regex("""^\s*if\s*\(""", RegexOption.IGNORE_CASE).containsMatchIn(trimmed) ->
                                        nodes.add("IfStatement")

                                    Regex("""^\s*for\s*\(""", RegexOption.IGNORE_CASE).containsMatchIn(trimmed) ->
                                        nodes.add("ForStatement")

                                    Regex("""^\s*while\s*\(""", RegexOption.IGNORE_CASE).containsMatchIn(trimmed) ->
                                        nodes.add("WhileStatement")

                                    Regex("""^\s*do\s*\{""", RegexOption.IGNORE_CASE).containsMatchIn(trimmed) ->
                                        nodes.add("DoWhileStatement")

                                    Regex("""^\s*return\s+""").containsMatchIn(trimmed) ->
                                        nodes.add("ReturnStatement")

                                    Regex("""^\s*try\s*\{""", RegexOption.IGNORE_CASE).containsMatchIn(trimmed) ->
                                        nodes.add("TryStatement")

                                    Regex("""^\s*catch\s*\(""", RegexOption.IGNORE_CASE).containsMatchIn(trimmed) ->
                                        nodes.add("CatchClause")

                                    Regex("""^\s*switch\s*\(""", RegexOption.IGNORE_CASE).containsMatchIn(trimmed) ->
                                        nodes.add("SwitchStatement")

                                    Regex("""^\s*case\s+""", RegexOption.IGNORE_CASE).containsMatchIn(trimmed) ->
                                        nodes.add("CaseClause")

                                    Regex("""^\s*break;?""", RegexOption.IGNORE_CASE).containsMatchIn(trimmed) ->
                                        nodes.add("BreakStatement")
                                }
                            }
                            return nodes
                        }

                        // 6. FUNKCIJA ZA JACCARD SLIČNOST
                        fun calculateJaccardSimilarity(snapshots: List<Set<String>>): Double {
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

// 7. COMPILE ERRORS LISTENER
                        project.messageBus.connect().subscribe(
                            com.intellij.codeInsight.daemon.DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC,
                            object : com.intellij.codeInsight.daemon.DaemonCodeAnalyzer.DaemonListener {
                                override fun daemonFinished() {
                                    ApplicationManager.getApplication().invokeLater {
                                        val currentEditor = FileEditorManager.getInstance(project)
                                            .selectedTextEditor ?: return@invokeLater

                                        val highlights = com.intellij.codeInsight.daemon.impl
                                            .DaemonCodeAnalyzerImpl.getHighlights(
                                                currentEditor.document,
                                                com.intellij.lang.annotation.HighlightSeverity.ERROR,
                                                project
                                            )

                                        compileErrors += highlights.count { h ->
                                            val desc = h.description ?: ""
                                            !desc.contains("runtime", ignoreCase = true) &&
                                                    !desc.contains("exception", ignoreCase = true)
                                        }

                                        runtimeErrors += highlights.count { h ->
                                            val desc = h.description ?: ""
                                            desc.contains("runtime", ignoreCase = true) ||
                                                    desc.contains("exception", ignoreCase = true)
                                        }

                                        println("ER DEBUG: compile=$compileErrors, runtime=$runtimeErrors")
                                    }
                                }
                            }
                        )

// 8. TIMER — šalje paket svakih 20 sekundi
                        val kolokvijumPocetak = System.currentTimeMillis()

                        val feedbackTimer = javax.swing.Timer(20000) {
                            val now = System.currentTimeMillis()
                            val tCurrentSec = ((now - kolokvijumPocetak) / 1000).toInt()

                            val currentSnapshot = getAstSnapshot()
                            snapshotHistory.add(currentSnapshot)
                            while (snapshotHistory.size > 10) snapshotHistory.removeFirst()

                            val csValue = if (snapshotHistory.size >= 2) {
                                calculateJaccardSimilarity(snapshotHistory)
                            } else {
                                1.0
                            }

                            val currentLines = classLines
                            val deltaL = currentLines - lastLinesCount
                            lastLinesCount = currentLines

                            val timeOnClassSec = if (currentFileName.isNotEmpty()) {
                                (now - currentFileStartTime) / 1000
                            } else {
                                0
                            }

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
    "timestamp": "${java.time.LocalDateTime.now()}",
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
                                    val url = java.net.URL("http://157.180.37.247/api/data")
                                    val conn = url.openConnection() as java.net.HttpURLConnection
                                    conn.requestMethod = "POST"
                                    conn.setRequestProperty("Content-Type", "application/json")
                                    conn.connectTimeout = 5000
                                    conn.readTimeout = 5000
                                    conn.doOutput = true
                                    conn.outputStream.use { it.write(json.toByteArray()) }
                                    val responseCode = conn.responseCode
                                    println("Feedback API: $responseCode | student: $studentId")
                                    conn.disconnect()
                                } catch (e: Exception) {
                                    println("Greška pri slanju metrika: ${e.message}")
                                }
                            }
                        }

                        feedbackTimer.start()
                        println("Feedback Dashboard listeneri pokrenuti za: $studentId")
                        // ******************** OVDE SE ZAVRSAVAJU ZARKOVI LISTENERI

                        ApplicationManager.getApplication().invokeLater {

                            val projectPath = project.basePath ?: return@invokeLater
                            val projectDir = File(projectPath)

                            val base = File(System.getProperty("user.home"), MyBundle.downloadFolder)

                            val assignmentSource = base

                            FileDocumentManager.getInstance().saveAllDocuments()
                            VirtualFileManager.getInstance().syncRefresh()

                            WriteAction.run<Throwable> {

                                val projectVf = LocalFileSystem.getInstance()
                                    .refreshAndFindFileByIoFile(projectDir) ?: return@run

                                // obriši stare fajlove
                                projectVf.children
                                    .filter { it.name != ".idea" && it.name != ".git" }
                                    .forEach { it.delete(this) }

                                val sourceVf = LocalFileSystem.getInstance()
                                    .refreshAndFindFileByIoFile(assignmentSource) ?: return@run

                                // kopiraj nove fajlove iz preuzetog projekta (bez .idea)
                                sourceVf.children
                                    .filter { it.name != ".idea" && it.name != ".git" }
                                    .forEach { child ->
                                        VfsUtil.copy(this, child, projectVf)
                                    }

                                //obrisi privremeni folder
                                File(System.getProperty("user.home"), MyBundle.downloadFolder).deleteRecursively()
                            }

                            VirtualFileManager.getInstance().syncRefresh()
                        }


                        studentsFirstNameTF.isEnabled = false
                        studentsLastNameTF.isEnabled = false
                        studentsStudyProgramTF.isEnabled = false
                        studentsIndexNumberTF.isEnabled = false
                        studentsStartYearTF.isEnabled = false
                        classroomNameTF.isEnabled = false
                        subjectCB.isEnabled = false
                        testGroupCB.isEnabled = false
                        studentsTermCB.isEnabled = false
                        studentsTaskGroupTF.isEnabled = false

                        //  Overwriting the opened project
                        ApplicationManager.getApplication().executeOnPooledThread {
//                                val isSuccess = GitServerHttpService.cloneRepository(Config.SSH_LOCAL_PATH_1)
                            if (isSuccess) {
                                progressBar.isVisible = true
                                signInButton.isEnabled = false
                                commitButton.isVisible = false
                                signInButton.isVisible = false
                                finalSubmissionButton.isVisible = true

                                ApplicationManager.getApplication().invokeLater(Runnable {

                                    val virtualFile =
                                        LocalFileSystem.getInstance().refreshAndFindFileByPath(project.basePath!!)
                                    virtualFile?.refresh(false, true)
                                })
                            }
                        }
                    } else {
                        cloningReportArea.text = "Failed to clone repository."
                    }
                })
            }

        }


        commitButton.preferredSize = Dimension(100, 30)
        commitButton.maximumSize = Dimension(100, 30)

        commitButton.addActionListener {
            trackingService.logEvent("SUBMISSION_ATTEMPT", currentStudentId, mapOf("type" to "commit"))
            val currentProject = ProjectManager.getInstance().openProjects[0]
            FileDocumentManager.getInstance().saveAllDocuments()

            // Start a background thread for the blocking operations
            ApplicationManager.getApplication().executeOnPooledThread {
                // Use the project base path as the repository path. Replace "newBranch" with the desired branch name.
                studentService.setProjectRoot(currentProject.basePath)
                val isPushSuccess = studentService.submitAssignment(false)

                // If the push operation is successful, close and dispose of the project
                if (isPushSuccess) {
                    JOptionPane.showMessageDialog(
                        null,
                        "Uspešno ste predali rad!",
                        "Uspešno",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                }
            }
        }

        finalSubmissionButton.addActionListener {
            val confirmationDialog = JOptionPane.showConfirmDialog(
                null,
                "Da li ste sigurni da želite da predate rad?",
                "Potvrda o predaji rada",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            )

            if (confirmationDialog == JOptionPane.YES_OPTION) {
                trackingService.logEvent("SUBMISSION_ATTEMPT", currentStudentId, mapOf("type" to "final"))

                val currentProject = ProjectManager.getInstance().openProjects[0]
                FileDocumentManager.getInstance().saveAllDocuments()

                // Start a background thread for the blocking operations
                ApplicationManager.getApplication().executeOnPooledThread {

                    studentService.setProjectRoot(currentProject.basePath)
                    val isPushSuccess = studentService.submitAssignment(true)

                    trackingService.stopTracking(currentStudentId)

                    if (isPushSuccess) {

                        finalSubmissionButton.isEnabled = false
                        //showSuccessPopup()
                        JOptionPane.showMessageDialog(
                            null,
                            "Uspešno ste predali rad!",
                            "Uspešno",
                            JOptionPane.INFORMATION_MESSAGE
                        )
                    } else {
                        println("Failed to push changes to new branch.")
                        //  Dodato samo radi demonstracije
                        JOptionPane.showMessageDialog(
                            null,
                            "Greška tokom predaje rada!",
                            "Greška",
                            JOptionPane.INFORMATION_MESSAGE
                        )
                    }
                }
            }

        }

        commitButton.preferredSize = Dimension(100, 30)
        commitButton.maximumSize = Dimension(100, 30)

        fieldsPanel.add(signInButton)
        commitButton.isVisible = false
        finalSubmissionButton.isVisible = false

        fieldsPanel.add(commitButton)
        fieldsPanel.add(Box.createRigidArea(Dimension(0, 5))) // Vertikalni razmak od 10 piksela
        fieldsPanel.add(finalSubmissionButton)

        val checkProjectButton = JButton("Check Project")

        initialPanel.add(JLabel(System.getProperty("user.name")))
        initialPanel.add(checkProjectButton)

        mainPanel.add(fieldsPanel, BorderLayout.NORTH)

        // Add the panel to the ToolWindow
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(mainPanel, "", false)
        toolWindow.contentManager.addContent(content)

    }


    private fun makeField(name: String, field: JTextField): JPanel {
        val labelText = JLabel(name)
        labelText.minimumSize = Dimension(20, labelText.minimumSize.height)
        labelText.preferredSize = Dimension(80, labelText.preferredSize.height)
        labelText.border = BorderFactory.createCompoundBorder(
            EmptyBorder(0, 0, 0, 10),  // Padding around the label
            EmptyBorder(0, 0, 0, 0)
        )

        val labelPanel = JPanel()
        labelPanel.layout = BoxLayout(labelPanel, BoxLayout.X_AXIS)
        labelPanel.add(labelText)
        labelPanel.add(Box.createHorizontalGlue())  // This will take all extra space

        val fieldPanel = JPanel(BorderLayout())
        fieldPanel.add(labelPanel, BorderLayout.WEST)
        fieldPanel.add(field, BorderLayout.CENTER)

        return fieldPanel
    }

    private fun makeSmallField(name: String, field: JTextField): JPanel {
        val labelText = JLabel(name)
        labelText.minimumSize = Dimension(20, labelText.minimumSize.height)
        labelText.preferredSize = Dimension(60, labelText.preferredSize.height)
        labelText.border = BorderFactory.createCompoundBorder(
            EmptyBorder(0, 0, 0, 10),  // Padding around the label
            EmptyBorder(0, 0, 0, 0)
        )

        val labelPanel = JPanel()
        labelPanel.layout = BoxLayout(labelPanel, BoxLayout.X_AXIS)
        labelPanel.add(labelText)
        labelPanel.add(Box.createHorizontalGlue())  // This will take all extra space

        val fieldPanel = JPanel(BorderLayout())
        fieldPanel.add(labelPanel, BorderLayout.WEST)
        fieldPanel.add(field, BorderLayout.CENTER)

        return fieldPanel
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

    override fun shouldBeAvailable(project: Project) = true
}
