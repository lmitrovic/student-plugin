package com.github.nikolajr93.studenttestingintellijplugin.toolWindow

import com.github.nikolajr93.studenttestingintellijplugin.MyBundle
import com.github.nikolajr93.studenttestingintellijplugin.api.RafApiClient
import com.github.nikolajr93.studenttestingintellijplugin.api.Student
import com.github.nikolajr93.studenttestingintellijplugin.api.StudentInfoDto
import com.github.nikolajr93.studenttestingintellijplugin.services.MyProjectService
import com.google.gson.Gson
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.service
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
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.Label
import com.intellij.ui.content.ContentFactory
import raflms.studentstub.api.StudentStubService
import raflms.studentstub.api.datamodel.TestWithAssignments
import raflms.studentstub.config.ConfigFactory
import tracking.EventTracker
import java.awt.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.util.*
import javax.swing.*
import javax.swing.Timer
import javax.swing.border.EmptyBorder
import kotlin.properties.Delegates


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

    private lateinit var studentsIndexCombined: String
    private lateinit var studentReturnedJSON: String
    private lateinit var studentReturnedJSONLocal: String
    private lateinit var localStudentObject: Student
    private lateinit var localStudentDTObject: StudentInfoDto
    private lateinit var remoteStudentObject1: Student
    private lateinit var remoteStudentObject2: StudentInfoDto

    private lateinit var gson: Gson

    private lateinit var mainPanel: JPanel
    private lateinit var initialPanel: JPanel
    private lateinit var formPanel: JPanel
    private lateinit var studentEnrollmentInfoPanel: JPanel
    private lateinit var studentsTestSpecificPanel: JPanel
    private lateinit var comboBoxPanel: JPanel
    private lateinit var fieldsPanel: JPanel
    private lateinit var afterClonedPanel: JPanel
    private lateinit var toolWindow: ToolWindow
    private lateinit var contentFactory: ContentFactory

    private lateinit var studentIndex: String
    private var isSuccess by Delegates.notNull<Boolean>()

    // Add event tracker
    private lateinit var eventTracker: EventTracker

    init {
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
        gson = Gson()
        isSuccess = false

        var firstDigitPos = MyBundle.username.indexOfFirst { it.isDigit() }
        var lastDigitPos = MyBundle.username.indexOfLast { it.isDigit() }
        MyBundle.builtStudentId =
            MyBundle.username.substring(lastDigitPos + 1).uppercase(Locale.getDefault()) +
            MyBundle.username.substring(firstDigitPos, lastDigitPos - 1) + LocalDate.now().year / 100 +
            MyBundle.username.substring(lastDigitPos - 1, lastDigitPos + 1)

        print(MyBundle.builtStudentId)
//        Ne radi ako API nije podignut
        var studentReturnedString = RafApiClient.getStudent(MyBundle.builtStudentId)
        MyBundle.returnedStudentString = studentReturnedString
        remoteStudentObject2 = gson.fromJson(studentReturnedString, StudentInfoDto::class.java)
        MyBundle.returnedStudent2 = remoteStudentObject2
        remoteStudentObject2 = MyBundle.returnedStudent2

        if (MyBundle.currUsername.length > 3) {
            MyBundle.username = MyBundle.currUsername
        }
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        eventTracker = EventTracker(project)

        val studentService = StudentStubService(ConfigFactory.createConfig())

        val struggleTracker = eventTracker.struggleTracker
        val errorTracker = eventTracker.errorTracker

        struggleTracker?.trackCompilationPattern(isSuccess)
        errorTracker?.trackError("Compilation failed", "SYNTAX_ERROR")

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
                                val timeSpent = currentTime - lastTime
                                val data = hashMapOf<String, Any>(
                                    "line" to lastLine,
                                    "timeSpentMillis" to timeSpent
                                )
                                eventTracker.logEvent("LINE_VIEW_DURATION", MyBundle.builtStudentId, data)

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
        val myToolWindow = MyToolWindow(toolWindow)

        this.contentFactory = ContentFactory.getInstance()

        // Get instance of ProjectManagerEx for project handling
        val projectManager = ProjectManager.getInstance()

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

        classroomNameTF.text = MyBundle.classroom

        val subjectLabelText = JLabel("Assignment:")
        subjectLabelText.minimumSize = Dimension(20, subjectLabelText.minimumSize.height)
        subjectLabelText.preferredSize = Dimension(80, subjectLabelText.preferredSize.height)

        val allTestsWithAssigmentsData: MutableList<TestWithAssignments> = studentService.allTestsWithAssigmentsData

        val subjectChoices = allTestsWithAssigmentsData.map { it.testName }.toTypedArray()
        testGroupCB = JComboBox()
        studentsTermCB = JComboBox()
        subjectCB = JComboBox(subjectChoices)
        subjectCB.isEnabled = true

        subjectCB.addActionListener {
            val selectedTestName = subjectCB.selectedItem as? String ?: return@addActionListener
            val selectedTest = allTestsWithAssigmentsData
                .find { it.testName == selectedTestName } ?: return@addActionListener

            val assignments = selectedTest.assigments ?: return@addActionListener
            val groups = assignments.mapNotNull { it.group }
                .distinct()
                .toTypedArray()

            val terms = assignments.mapNotNull { it.term }
                .distinct()
                .toTypedArray()

            testGroupCB.model = DefaultComboBoxModel(groups)
            studentsTermCB.model = DefaultComboBoxModel(terms)
        }

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
                    subjectCB.selectedItem?.toString(),
                    testGroupCB.selectedItem?.toString(),
                    studentsTermCB.selectedItem?.toString(),
                    Paths.get(System.getProperty("user.home"), MyBundle.downloadFolder).toString()
                )

                // `invokeLater` schedules this task to run on the Event Dispatch Thread (EDT).
                ApplicationManager.getApplication().invokeLater {
                    if (isSuccess) {

                        ApplicationManager.getApplication().invokeLater {

                            val projectPath = project.basePath ?: return@invokeLater
                            val projectDir = File(projectPath)

                            val base = File(System.getProperty("user.home"), MyBundle.downloadFolder)

                            val first = base.listFiles { obj: File? -> obj!!.isDirectory() }[0]
                            val second = first.listFiles { obj: File? -> obj!!.isDirectory() }[0]

                            val assignmentSource = File(second.absolutePath)

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
                                MyBundle.repoCloned = true
                                commitButton.isVisible = false
                                signInButton.isVisible = false
                                finalSubmissionButton.isVisible = true

                                ApplicationManager.getApplication().invokeLater(Runnable {

                                    val virtualFile =
                                        LocalFileSystem.getInstance().refreshAndFindFileByPath(project.basePath!!)
                                    virtualFile?.refresh(false, true)

                                    // Log that student can now start coding
                                    eventTracker.logEvent("CODING_PHASE_START", MyBundle.builtStudentId)
                                })
                            }
                        }
                    } else {
                        cloningReportArea.text = "Failed to clone repository."
                        // Log failed clone
                        eventTracker.logEvent("REPOSITORY_CLONE_FAILED", MyBundle.builtStudentId)
                    }
                }
            }

        }

        var isPushSuccess = false;

        commitButton.preferredSize = Dimension(100, 30)
        commitButton.maximumSize = Dimension(100, 30)
        commitButton.addActionListener {
            // Log submission attempt
            eventTracker.logEvent("SUBMISSION_ATTEMPT", MyBundle.builtStudentId, HashMap<String, Any>().apply {
                put("attemptTime", System.currentTimeMillis())
            })

            // Get the current project
            val currentProject = ProjectManager.getInstance().openProjects[0]

            // Save all open files
            FileDocumentManager.getInstance().saveAllDocuments()

            // Log that files were saved before submission
            eventTracker.logEvent("FILES_SAVED_BEFORE_SUBMISSION", MyBundle.builtStudentId)

            // Start a background thread for the blocking operations
            ApplicationManager.getApplication().executeOnPooledThread {
                // Use the project base path as the repository path. Replace "newBranch" with the desired branch name.
                studentService.setProjectRoot(currentProject.basePath)
                isPushSuccess = studentService.submitAssignment(false)

                // If the push operation is successful, close and dispose of the project

                if(isPushSuccess)
                    showSuccessPopup()
            }
        }

        finalSubmissionButton.addActionListener {
            // Log submission attempt
            eventTracker.logEvent("SUBMISSION_ATTEMPT", MyBundle.builtStudentId, HashMap<String, Any>().apply {
                put("attemptTime", System.currentTimeMillis())
            })

            // Get the current project
            val currentProject = ProjectManager.getInstance().openProjects[0]

            // Save all open files
            FileDocumentManager.getInstance().saveAllDocuments()

            // Log that files were saved before submission
            eventTracker.logEvent("FILES_SAVED_BEFORE_SUBMISSION", MyBundle.builtStudentId)

            // Start a background thread for the blocking operations
            ApplicationManager.getApplication().executeOnPooledThread {

                studentService.setProjectRoot(currentProject.basePath)
                isPushSuccess = studentService.submitAssignment(true)

                if (isPushSuccess) {
                    finalSubmissionButton.isEnabled = false
                    eventTracker.logEvent("SUBMISSION_PUSH_SUCCESS", MyBundle.builtStudentId)

                    val confirmationDialog = JOptionPane.showConfirmDialog(
                        null,
                        "Da li ste sigurni da želite da predate rad?",
                        "Potvrda o predaji rada",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                    )

                    if (confirmationDialog == JOptionPane.YES_OPTION) {
                        // Log final submission confirmation
                        eventTracker.logEvent(
                            "SUBMISSION_CONFIRMED",
                            MyBundle.builtStudentId,
                            HashMap<String, Any>().apply {
                                put("finalSubmissionTime", System.currentTimeMillis())
                            })

                        // Stop tracking before closing
                        eventTracker.stopTracking(MyBundle.builtStudentId)
                    } else {
                        // Log submission cancelled
                        eventTracker.logEvent("SUBMISSION_CANCELLED", MyBundle.builtStudentId)
                    }
                } else {
                    // Log failed push
                    eventTracker.logEvent("SUBMISSION_PUSH_FAILED", MyBundle.builtStudentId)

                    println("Failed to push changes to new branch.")

                    //  Dodato samo radi demonstracije
                    val confirmationDialog = JOptionPane.showConfirmDialog(
                        null,
                        "Da li ste sigurni da želite da predate rad?",
                        "Potvrda o predaji rada",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                    )
                    if (confirmationDialog == JOptionPane.YES_OPTION) {
                        eventTracker.logEvent("SUBMISSION_CONFIRMED_DESPITE_FAILURE", MyBundle.builtStudentId)
                        eventTracker.stopTracking(MyBundle.builtStudentId)
                    }
                }
            }
        }

        commitButton.preferredSize = Dimension(100, 30)
        commitButton.maximumSize = Dimension(100, 30)


        fieldsPanel.add(signInButton)
        commitButton.isVisible = false
        finalSubmissionButton.isVisible = false
        if (MyBundle.repoCloned) {
            finalSubmissionButton.isVisible = true
            commitButton.isVisible = true
        }
        fieldsPanel.add(commitButton)
        fieldsPanel.add(Box.createRigidArea(Dimension(0, 5))) // Vertikalni razmak od 10 piksela
        fieldsPanel.add(finalSubmissionButton)

        val checkProjectButton = JButton("Check Project")

        initialPanel.add(Label(System.getProperty("user.name")))
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



    private fun showSuccessPopup() {
        val frame = JFrame().apply {
            title = "Uspešno"
            isUndecorated = true
            preferredSize = Dimension(300, 60) // Manje dimenzije
            defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
            background = Color(0, 0, 0, 0)
        }

        // Panel sa svetlo zelenom bojom (#DBF7EE)
        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(219, 247, 238) // #DBF7EE u RGB
            border = BorderFactory.createEmptyBorder(15, 20, 15, 20) // Manji padding

            val label = JLabel("Uspešno sačuvan rad", SwingConstants.CENTER).apply {
                foreground = Color(0, 0, 0) // Crni tekst
                font = font.deriveFont(Font.BOLD, 13f) // Manja font veličina
                alignmentX = Component.CENTER_ALIGNMENT
            }

            add(label)
        }

        frame.contentPane = panel
        frame.pack()

        // Centriranje sa pomeranjem na gore
        val dim = Toolkit.getDefaultToolkit().screenSize
        frame.setLocation(dim.width/2 - frame.size.width/2, dim.height/4 - frame.size.height/2)

        // Postavljamo prozor da bude polu-transparentan na početku
        frame.opacity = 0f
        frame.isVisible = true

        // Animacija pojavljivanja
        Timer(20, null).apply {
            var counter = 0f
            addActionListener {
                if (counter < 1f) {
                    counter += 0.05f
                    frame.opacity = counter
                } else {
                    stop()
                }
            }
            start()
        }

        // Automatsko zatvaranje sa fade-out animacijom nakon 2 sekunde
        Timer(2000) { _ ->
            Timer(20, null).apply {
                var counter = 1f
                addActionListener {
                    if (counter > 0f) {
                        counter -= 0.05f
                        frame.opacity = counter
                    } else {
                        frame.dispose()
                        stop()
                    }
                }
                start()
            }
        }.apply {
            isRepeats = false
            start()
        }
    }

    private fun getClassroom(input: String): String {
        if (input.length != 5) return ""

        val prefix = input[0].lowercaseChar()
        val digit = input[2]  // jer je format: G0x00 → interesuje nas 'x' na indeksu 2

        return when (prefix) {
            'g' -> "RG $digit"
            'u' -> "RAF $digit"
            else -> ""
        }
    }


    override fun shouldBeAvailable(project: Project) = true

    class MyToolWindow(toolWindow: ToolWindow) {

        private val service = toolWindow.project.service<MyProjectService>()

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            val label = JBLabel(MyBundle.message("randomLabel", "?"))

            add(label)
            add(JButton(MyBundle.message("shuffle")).apply {
                addActionListener {
                    label.text = MyBundle.message("randomLabel", service.getRandomNumber())
                }
            })
        }
    }
}
