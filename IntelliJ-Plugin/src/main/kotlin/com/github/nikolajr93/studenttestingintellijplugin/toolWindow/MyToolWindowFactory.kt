package com.github.nikolajr93.studenttestingintellijplugin.toolWindow


import com.github.nikolajr93.studenttestingintellijplugin.GitServerHttpService
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
    private lateinit var studentsTaskGroupCB: JComboBox<Any>
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

    //    private lateinit var objectMapper: ObjectMapper
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

//    private lateinit var connection: MessageBusConnection

    private lateinit var studentIndex: String
    private var isSuccess by Delegates.notNull<Boolean>()

    // Add event tracker
    private lateinit var eventTracker: EventTracker

    init {
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
        // Gson added
        gson = Gson()

        isSuccess = false

        //var uname = System.getenv("username");
        //uname = "znedeljkovic223d"
        //MyBundle.username = uname



        //MyBundle.computerName = System.getenv("computername")
//        if (MyBundle.computerName.startsWith("u", true)) {
//            MyBundle.classroom = "Raf" + MyBundle.computerName.substring(2, 3)
//        }



        //MyBundle.classroom = getClassroom(MyBundle.computerName)

        var firstDigitPos = MyBundle.username.indexOfFirst { it.isDigit() }
        var lastDigitPos = MyBundle.username.indexOfLast { it.isDigit() }
        MyBundle.builtStudentId = MyBundle.username.substring(lastDigitPos + 1).uppercase(Locale.getDefault()) +
//                MyBundle.username.substring(firstDigitPos, lastDigitPos - 1) + "20" +
                MyBundle.username.substring(firstDigitPos, lastDigitPos - 1) + LocalDate.now().year / 100 +
                MyBundle.username.substring(lastDigitPos - 1, lastDigitPos + 1)

//        var firstDigitPos1 = uname.indexOfFirst { it.isDigit() }
//        var lastDigitPos1 = uname.indexOfLast { it.isDigit() }
//        MyBundle.builtStudentId = uname.substring(lastDigitPos1+1).uppercase(Locale.getDefault()) +
////                MyBundle.username.substring(firstDigitPos, lastDigitPos - 1) + "20" +
//                uname.substring(firstDigitPos1, lastDigitPos1 - 1) + LocalDate.now().year/100 +
//                uname.substring(lastDigitPos1 -1, lastDigitPos1 +1)

//        Ucitavanje studentksih podataka iz tekstualnog fajla umesto dohvatanja sa APIja
//        studentReturnedJSONLocal = File(Config.STUDENT_INFO_FILE_PATH).readText()
//        localStudentObject = gson.fromJson(studentReturnedJSONLocal, Student::class.java)

//        Sa novijim DTO modelom
//        studentReturnedJSONLocal = File(Config.STUDENT_INFO_FILE_PATH1).readText()
//        localStudentDTObject = gson.fromJson(studentReturnedJSONLocal, StudentInfoDto::class.java)

//        Za sledecu verziju povuci indeks studenta iz environment variabli (user sadrzi indeks)
//        var studentReturnedString = RafApiClient.getStudent(MyBundle.studentId)
        print(MyBundle.builtStudentId)
//        Ne radi ako API nije podignut
        var studentReturnedString = RafApiClient.getStudent(MyBundle.builtStudentId)
        MyBundle.returnedStudentString = studentReturnedString
//        remoteStudentObject1 = gson.fromJson(studentReturnedString, Student::class.java)
        remoteStudentObject2 = gson.fromJson(studentReturnedString, StudentInfoDto::class.java)
//        MyBundle.returnedStudent = remoteStudentObject1
        MyBundle.returnedStudent2 = remoteStudentObject2

//        Rad sa StudentDTO iz fajla StudentInfo1
//        MyBundle.returnedStudent2 = localStudentDTObject
        remoteStudentObject2 = MyBundle.returnedStudent2


        //MyBundle.currUsername = System.getenv("username")
//        if(MyBundle.currUsername.contains("23")){
        if (MyBundle.currUsername.length > 3) {
            MyBundle.username = MyBundle.currUsername
        }

    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // Initialize event tracker early
        eventTracker = EventTracker(project)

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

        studentsFirstNameTF.text = remoteStudentObject2.firstName
        fieldsPanel.add(makeField("First name:", studentsFirstNameTF))


        studentsLastNameTF = JTextField(20)
        studentsLastNameTF.preferredSize = Dimension(300, 24)
//        studentsLastNameTF.text = localStudentObject.lastName
        studentsLastNameTF.text = remoteStudentObject2.lastName
        fieldsPanel.add(makeField("Last name:", studentsLastNameTF))

        this.studentEnrollmentInfoPanel = JPanel(GridLayout(0, 4))

        studentsStudyProgramTF = JTextField(20)
        studentsStudyProgramTF.preferredSize = Dimension(300, 24)

        studentsStudyProgramTF.text = remoteStudentObject2.studyProgramShort
        studentEnrollmentInfoPanel.add(makeField("Program:", studentsStudyProgramTF))

        studentsIndexNumberTF = JTextField(20)
        studentsIndexNumberTF.preferredSize = Dimension(300, 24)

        studentsIndexNumberTF.text = remoteStudentObject2.indexNumber.toString()
        studentEnrollmentInfoPanel.add(makeSmallField("Number:", studentsIndexNumberTF))

        studentsStartYearTF = JTextField(20)
        studentsStartYearTF.preferredSize = Dimension(300, 24)
        studentsStartYearTF.text = remoteStudentObject2.startYear
        studentEnrollmentInfoPanel.add(makeSmallField("Year:", studentsStartYearTF))

        val groupChoices = arrayOf("101", "102")
        studentsTaskGroupCB = JComboBox(groupChoices)
        studentEnrollmentInfoPanel.add(studentsTaskGroupCB)

        fieldsPanel.add(studentEnrollmentInfoPanel)

        this.studentsTestSpecificPanel = JPanel(GridLayout(0, 4))


        classroomNameTF = JTextField(20)
        classroomNameTF.preferredSize = Dimension(300, 24)

        classroomNameTF.text = MyBundle.classroom

//        val classroomLabelText = JLabel("Classroom:")
//        classroomLabelText.minimumSize = Dimension(20, classroomLabelText.minimumSize.height)
//        classroomLabelText.preferredSize = Dimension(70, classroomLabelText.preferredSize.height)
//        classroomLabelText.border = BorderFactory.createCompoundBorder(
//            EmptyBorder(0, 0, 0, 10),  // Padding around the label
//            EmptyBorder(0, 0, 0, 0)
//        )
//
//        studentsTestSpecificPanel.add(classroomLabelText)
//        studentsTestSpecificPanel.add(classroomNameTF)

        val termChoices = arrayOf("termin1", "termin2")
        studentsTermCB = JComboBox(termChoices)
        studentsTestSpecificPanel.add(studentsTermCB)

        val testGroupLabelText = JLabel("Asgmt. grp:")
        testGroupLabelText.minimumSize = Dimension(20, testGroupLabelText.minimumSize.height)
        testGroupLabelText.preferredSize = Dimension(70, testGroupLabelText.preferredSize.height)
        testGroupLabelText.border = BorderFactory.createCompoundBorder(
            EmptyBorder(0, 0, 0, 10),  // Padding around the label
            EmptyBorder(0, 0, 0, 0)
        )

//        val subjectLabelText = JLabel("Subject:")
        val subjectLabelText = JLabel("Asgmt:")
        subjectLabelText.minimumSize = Dimension(20, subjectLabelText.minimumSize.height)
        subjectLabelText.preferredSize = Dimension(70, subjectLabelText.preferredSize.height)
        subjectLabelText.border = BorderFactory.createCompoundBorder(
            EmptyBorder(0, 0, 0, 10),  // Padding around the label
            EmptyBorder(0, 0, 0, 0)
        )


        val assignmentChoices = arrayOf("grupa1", "grupa2")
        val subjectChoices = arrayOf("testoop", "VP", "NVP", "MSA", "SK", "TS")
        // Create a JComboBox with the choices
        testGroupCB = JComboBox(assignmentChoices)
        subjectCB = JComboBox(subjectChoices)
        subjectCB.isEnabled = false

        studentsTestSpecificPanel.add(subjectLabelText)

        studentsTestSpecificPanel.add(subjectCB)


//        studentsTestSpecificPanel.add(testGroupLabelText)
        studentsTestSpecificPanel.add(testGroupCB)

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

        // Create a Sign in button and add it to the panel
        val signInButton = JButton("Begin")

        val studentService = StudentStubService(ConfigFactory.createConfig())

        signInButton.addActionListener {

                var isFirstNameTheSame = remoteStudentObject2.firstName.equals(studentsFirstNameTF.getText(), true)
                var isLastNameTheSame = remoteStudentObject2.lastName.equals(studentsLastNameTF.getText(), true)

                if (isFirstNameTheSame && isLastNameTheSame) {
                    // START EVENT TRACKING HERE - as soon as student successfully logs in
                    eventTracker.startTracking(MyBundle.builtStudentId)

                    // Log the successful login
                    val loginData = hashMapOf<String, Any>(
                        "taskGroup" to (testGroupCB.selectedIndex + 1),
                        "classroom" to MyBundle.classroom,
                        "subject" to subjectCB.selectedItem.toString(),
                        "firstName" to studentsFirstNameTF.text,
                        "lastName" to studentsLastNameTF.text
                    )
                    eventTracker.logEvent("STUDENT_LOGIN_SUCCESS", MyBundle.builtStudentId, loginData)

                    val currentProject1 = project
                    val studentInfoFilePath = Paths.get(currentProject1?.basePath, "..")
                    val finalStudentInfoFilePath =
                        studentInfoFilePath.resolve("studentInfo1.txt").toAbsolutePath().normalize()
                    val finalStudentInfoFilePath2 =
                        studentInfoFilePath.resolve("studentInfo2.txt").toAbsolutePath().normalize()
                    val finalStudentTokenMessagePath =
                        studentInfoFilePath.resolve("studentTokenMessage.txt").toAbsolutePath().normalize()
                    val finalStudentTokenMessagePathForAfterReset =
                        studentInfoFilePath.resolve("studentTokenMessageAfterReset.txt").toAbsolutePath().normalize()
                    val finalStudentRepoAndForkMessagesPath =
                        studentInfoFilePath.resolve("studentRepoAndForkMessages.txt").toAbsolutePath().normalize()
                    Files.newBufferedWriter(finalStudentInfoFilePath).use { writer ->
                        writer.write(gson.toJson(remoteStudentObject2))
                    }

                    // Setting current project base path in MyBundle for future commits
                    MyBundle.currentProjectBasePath = currentProject1.basePath.toString()

//                Upisivanje dohvacenog studenta
                    Files.newBufferedWriter(finalStudentInfoFilePath2).use { writer ->
                        writer.write(MyBundle.returnedStudentString)
                        writer.newLine()
                        writer.write(gson.toJson(MyBundle.returnedStudent2))
                        writer.newLine()
                        writer.write(MyBundle.builtStudentId)
                        writer.newLine()
                        writer.write(MyBundle.currentProjectBasePath)
                        writer.newLine()
                        writer.write(MyBundle.computerName)
                        writer.newLine()
                        writer.write(MyBundle.currUsername)
                    }

                    var tempStudentTokenMessageReturned = RafApiClient.authorizeStudent(MyBundle.builtStudentId)
                    if (tempStudentTokenMessageReturned.contains("is already authorized")) {
                        MyBundle.strictStudentToken = File(finalStudentTokenMessagePath.toUri()).readText()
                    } else {
                        MyBundle.studentToken = tempStudentTokenMessageReturned
                        // Regular expression to extract the token value
                        val regex = Regex("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")
                        // Find the token value in the MyBundle.studentToken
                        val onlyTokenResult = regex.find(MyBundle.studentToken)
                        // If a match was found, get the value
                        val onlyTokenValue = onlyTokenResult?.value
                        if (onlyTokenValue != null) {
                            MyBundle.strictStudentToken = onlyTokenValue
                        }
                        Files.newBufferedWriter(finalStudentTokenMessagePath).use { writer ->
                            writer.write(MyBundle.strictStudentToken)
                        }
                    }
                    MyBundle.examString = "OopZadatak" + (testGroupCB.selectedIndex + 1)


                    // Log when exam/task is assigned
                    MyBundle.examString = "OopZadatak" + (testGroupCB.selectedIndex + 1)
                    val examData = hashMapOf<String, Any>(
                        "examString" to MyBundle.examString,
                        "taskGroup" to (testGroupCB.selectedIndex + 1)
                    )
                    eventTracker.logEvent("EXAM_ASSIGNED", MyBundle.builtStudentId, examData)


                    val testRepoMessageString = RafApiClient.getRepository(
                        MyBundle.builtStudentId,
                        MyBundle.strictStudentToken,
                        MyBundle.examString
                    )
                    print(testRepoMessageString)
                    val studentForkMessageString =
                        RafApiClient.getFork(MyBundle.builtStudentId, MyBundle.strictStudentToken)

                    // Log repository access
                    eventTracker.logEvent("REPOSITORY_ACCESS", MyBundle.builtStudentId, HashMap<String, Any>().apply {
                        put("repoPath", MyBundle.testRepoPath)
                        put("forkPath", MyBundle.studentForkPath)
                    })

                    MyBundle.testRepoMessage = testRepoMessageString
                    MyBundle.studentForkMessage = studentForkMessageString
//                    Test repo path parsing
                    val repoPathString = MyBundle.testRepoMessage
                    val decodedRepoPathString = repoPathString.replace("\\\"", "\"")
                    val repoPathValue = decodedRepoPathString.split("\"message\":\"\"")[1].dropLast(3)
                    MyBundle.testRepoPath = repoPathValue
//                    Test repo path parsing
                    val forkPathString = MyBundle.studentForkMessage
                    val decodedForkPathString = forkPathString.replace("\\\"", "\"")
                    val forkPathValue = decodedForkPathString.split("\"message\":\"\"")[1].dropLast(3)
                    MyBundle.studentForkPath = forkPathValue
                    Files.newBufferedWriter(finalStudentRepoAndForkMessagesPath).use { writer ->
                        writer.write(MyBundle.testRepoMessage)
                        writer.newLine()
                        writer.write(MyBundle.studentForkMessage)
                        writer.newLine()
                        writer.write(MyBundle.testRepoPath)
                        writer.newLine()
                        writer.write(MyBundle.studentForkPath)
                        writer.newLine()
                        writer.write("Exam string: " + MyBundle.examString)
                    }

                    // Run the clone operation in a worker thread to avoid blocking the UI.
                    ApplicationManager.getApplication().executeOnPooledThread {

                        eventTracker.logEvent("REPOSITORY_CLONE_START", MyBundle.builtStudentId)

                        isSuccess = studentService.startAssigment(
                            studentsIndexNumberTF.text.toInt(),
                            studentsStartYearTF.text,
                            studentsStudyProgramTF.text,
                            studentsTaskGroupCB.selectedItem?.toString(),
                            subjectCB.selectedItem?.toString(),
                            testGroupCB.selectedItem?.toString(),
                            studentsTermCB.selectedItem?.toString(),
                            MyBundle.downloadPath
                        )

                        // `invokeLater` schedules this task to run on the Event Dispatch Thread (EDT).
                        ApplicationManager.getApplication().invokeLater {
                            if (isSuccess) {
                                cloningReportArea.text = "Repository cloned successfully."
                                // Log successful clone and exam start
                                eventTracker.logEvent("REPOSITORY_CLONE_SUCCESS", MyBundle.builtStudentId)
                                eventTracker.logEvent("EXAM_START", MyBundle.builtStudentId, HashMap<String, Any>().apply {
                                    put("examType", MyBundle.examString)
                                    put("startTime", System.currentTimeMillis())
                                })

                                ApplicationManager.getApplication().invokeLater {
//                                    ProjectManager.getInstance().closeAndDispose(project)
//                                    ProjectManager.getInstance().loadAndOpenProject(
//                                        "/Users/lukamitrovic/Desktop/Doktorske/zadatak/IdejaZaKol1Prepravljeno/untitled"
//                                    )

                                    val projectPath = project.basePath ?: return@invokeLater
                                    val projectDir = File(projectPath)

                                    val base = File(MyBundle.downloadPath)

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
                                    }

                                    VirtualFileManager.getInstance().syncRefresh()
                                }


                                studentsFirstNameTF.isEnabled = false
                                studentsLastNameTF.isEnabled = false
                                studentsStudyProgramTF.isEnabled = false
                                studentsIndexNumberTF.isEnabled = false
                                studentsStartYearTF.isEnabled = false
                                classroomNameTF.isEnabled = false
                                testGroupCB.isEnabled = false
                                studentsTermCB.isEnabled = false
                                studentsTaskGroupCB.isEnabled = false

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
                } else {
                    // Log failed login attempt
                    eventTracker.logEvent("STUDENT_LOGIN_FAILED", MyBundle.builtStudentId, HashMap<String, Any>().apply {
                        put("attemptedFirstName", studentsFirstNameTF.text)
                        put("attemptedLastName", studentsLastNameTF.text)
                    })

                    val labelTryAgain = JLabel("Wrong information, plase try again.")
                    fieldsPanel.add(labelTryAgain)
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
                isPushSuccess = GitServerHttpService.pushToRepositoryN(
                    MyBundle.currentProjectBasePath,
                    MyBundle.builtStudentId,
                    MyBundle.username + " je predao rad."
                )

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

                studentService.loggedStudentRepoPath = "/home/user/raflms/projectsrootdir/OOP/testoop/grupa1/termin1/studentrepos/7c5d91dd-f398-41ba-b1b3-6cf6c0d4d0ba"
                studentService.setProjectRoot(currentProject.basePath)
                isPushSuccess = studentService.submitAssignment(true)

                if (isPushSuccess) {
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
        labelText.preferredSize = Dimension(70, labelText.preferredSize.height)
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
