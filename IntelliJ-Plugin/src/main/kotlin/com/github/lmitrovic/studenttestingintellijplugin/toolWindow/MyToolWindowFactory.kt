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
import com.intellij.ui.components.Label
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

    private var isSuccess by Delegates.notNull<Boolean>()


    init {
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
        isSuccess = false
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {

        val studentService = StudentStubService(ConfigFactory.createConfig())

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
                ApplicationManager.getApplication().invokeLater (label@{
                    if (isSuccess) {

                        ApplicationManager.getApplication().invokeLater {

                            val projectPath = project.basePath ?: return@invokeLater
                            val projectDir = File(projectPath)

                            val base = File(System.getProperty("user.home"), MyBundle.downloadFolder)

                            val first = base.listFiles { obj: File? -> obj!!.isDirectory() }[0]
                            //val second = first.listFiles { obj: File? -> obj!!.isDirectory() }[0]

                            val assignmentSource = File(first.absolutePath)

                            FileDocumentManager.getInstance().saveAllDocuments()
                            VirtualFileManager.getInstance().syncRefresh()

                            WriteAction.run<Throwable> {

                                val projectVf = LocalFileSystem.getInstance()
                                    .refreshAndFindFileByIoFile(projectDir) ?: return@run

                                // obriši stare fajlove
                                projectVf.children
                                    //.filter { it.name != ".idea" && it.name != ".git" }
                                    .forEach { it.delete(this) }

                                val sourceVf = LocalFileSystem.getInstance()
                                    .refreshAndFindFileByIoFile(assignmentSource) ?: return@run

                                // kopiraj nove fajlove iz preuzetog projekta (bez .idea)
                                sourceVf.children
                                    //.filter { it.name != ".idea" && it.name != ".git" }
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
            val currentProject = ProjectManager.getInstance().openProjects[0]
            FileDocumentManager.getInstance().saveAllDocuments()

            // Start a background thread for the blocking operations
            ApplicationManager.getApplication().executeOnPooledThread {
                // Use the project base path as the repository path. Replace "newBranch" with the desired branch name.
                studentService.setProjectRoot(currentProject.basePath)
                val isPushSuccess = studentService.submitAssignment(false)

                // If the push operation is successful, close and dispose of the project
                if(isPushSuccess){
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

                val currentProject = ProjectManager.getInstance().openProjects[0]
                FileDocumentManager.getInstance().saveAllDocuments()

                // Start a background thread for the blocking operations
                ApplicationManager.getApplication().executeOnPooledThread {

                    studentService.setProjectRoot(currentProject.basePath)
                    val isPushSuccess = studentService.submitAssignment(true)

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

    override fun shouldBeAvailable(project: Project) = true
}
