package com.github.lmitrovic.studenttestingintellijplugin.toolWindow

import com.github.lmitrovic.studenttestingintellijplugin.assignment.AssignmentLoader
import com.github.lmitrovic.studenttestingintellijplugin.assignment.FormValidator
import com.github.lmitrovic.studenttestingintellijplugin.config.RafConfig
import com.github.lmitrovic.studenttestingintellijplugin.session.RafStubServices
import com.github.lmitrovic.studenttestingintellijplugin.session.StudentSessionService
import com.github.lmitrovic.studenttestingintellijplugin.session.StudentTrackingSession
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import raflms.studentstub.api.datamodel.TestWithAssignments
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import javax.swing.*

class StudentFormPanel(private val project: Project) {

    private val log = thisLogger()

    private val services = RafStubServices.getInstance(project)
    private val studentService get() = services.studentService
    private val trackingService get() = services.trackingService
    private val session = StudentSessionService.getInstance(project)

    val root: JPanel

    private val studentsFirstNameTF = JTextField()
    private val studentsLastNameTF = JTextField()
    private val studentsStudyProgramTF = JBTextField().apply { emptyText.text = "SI, RI ili RN" }
    private val studentsIndexNumberTF = JTextField()
    private val studentsStartYearTF = JTextField()
    private val classroomNameTF = JTextField()
    private val studentsTermCB = JComboBox<Any>()
    private val testGroupCB = JComboBox<Any>()
    private val subjectCB = JComboBox<Any>()

    private var allTestsData: List<TestWithAssignments> = emptyList()

    private val signInButton = JButton("Počni").apply {
        font = font.deriveFont(Font.BOLD, 13f)
        maximumSize = Dimension(Int.MAX_VALUE, JBUI.scale(36))
        alignmentX = Component.CENTER_ALIGNMENT
    }
    private val commitButton = JButton("Commit").apply {
        maximumSize = Dimension(Int.MAX_VALUE, JBUI.scale(30))
        alignmentX = Component.CENTER_ALIGNMENT
        isVisible = false
    }
    private val finalSubmissionButton = JButton("Predaj rad").apply {
        font = font.deriveFont(Font.BOLD, 13f)
        maximumSize = Dimension(Int.MAX_VALUE, JBUI.scale(36))
        alignmentX = Component.CENTER_ALIGNMENT
        isVisible = false
    }

    private var currentStudentId = ""

    init {
        subjectCB.addActionListener { updateGroupsAndTerms() }
        root = buildPanel()
        wireButtons()

        if (session.isActive) {
            restoreRunningSession()
        } else {
            loadTestsAsync()
        }
    }

    /** Lista testova/termina se povlači sa servera - u pozadini, da ne blokira EDT pri otvaranju panela. */
    private fun loadTestsAsync() {
        subjectCB.model = DefaultComboBoxModel(arrayOf<Any>("Učitavanje..."))
        subjectCB.isEnabled = false
        ApplicationManager.getApplication().executeOnPooledThread {
            val data = try {
                studentService.allTestsWithAssigmentsData
            } catch (e: Throwable) {
                log.warn("Neuspešno učitavanje liste testova", e)
                emptyList()
            }
            ApplicationManager.getApplication().invokeLater {
                allTestsData = data
                subjectCB.model = DefaultComboBoxModel(data.map { it.testName as Any }.toTypedArray())
                subjectCB.isEnabled = true
                updateGroupsAndTerms()
            }
        }
    }

    private fun updateGroupsAndTerms() {
        val selectedTestName = subjectCB.selectedItem as? String ?: return
        val selectedTest = allTestsData.find { it.testName == selectedTestName } ?: return
        val assignments = selectedTest.assigments ?: return
        testGroupCB.model = DefaultComboBoxModel(assignments.mapNotNull { it.group }.distinct().toTypedArray())
        studentsTermCB.model = DefaultComboBoxModel(assignments.mapNotNull { it.term }.distinct().toTypedArray())
    }

    private fun restoreRunningSession() {
        val s = session.current
        studentService.loggedStudentRepoPath = s.studentRepoPath
        currentStudentId = s.studentId

        studentsFirstNameTF.text = s.firstName
        studentsLastNameTF.text = s.lastName
        studentsStudyProgramTF.text = s.studyProgram
        studentsIndexNumberTF.text = s.indexNumber
        studentsStartYearTF.text = s.startYear
        classroomNameTF.text = s.classroom
        subjectCB.model = DefaultComboBoxModel(arrayOf<Any>(s.testName.ifBlank { "-" }))
        testGroupCB.model = DefaultComboBoxModel(arrayOf<Any>(s.groupLabel.ifBlank { "-" }))
        studentsTermCB.model = DefaultComboBoxModel(arrayOf<Any>(s.term.ifBlank { "-" }))

        disableFormFields()
        signInButton.isEnabled = false
        signInButton.isVisible = false
        commitButton.isVisible = false
        finalSubmissionButton.isVisible = true

        StudentTrackingSession.getInstance(project).start(trackingService, s.studentId, s.taskId)
        log.info("Sesija obnovljena za ${s.studentId} / ${s.taskId}")
    }

    private fun buildPanel(): JPanel {
        val gap = JBUI.scale(5)

        val studentSection = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = IdeBorderFactory.createTitledBorder("Podaci o studentu")
            add(labeledRow("Ime:", studentsFirstNameTF))
            add(Box.createRigidArea(Dimension(0, gap)))
            add(labeledRow("Prezime:", studentsLastNameTF))
            add(Box.createRigidArea(Dimension(0, gap)))
            add(labeledRow("Studijski program:", studentsStudyProgramTF))
            add(Box.createRigidArea(Dimension(0, gap)))
            add(labeledRow("Broj indeksa:", studentsIndexNumberTF))
            add(Box.createRigidArea(Dimension(0, gap)))
            add(labeledRow("Godina upisa:", studentsStartYearTF))
            add(Box.createRigidArea(Dimension(0, gap)))
            add(labeledRow("Učionica:", classroomNameTF))
        }

        val taskSection = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = IdeBorderFactory.createTitledBorder("Zadatak")
            add(Box.createRigidArea(Dimension(0, gap)))
            add(labeledRow("Test:", subjectCB))
            add(Box.createRigidArea(Dimension(0, gap)))
            add(labeledRow("Grupa zadatka:", testGroupCB))
            add(Box.createRigidArea(Dimension(0, gap)))
            add(labeledRow("Termin:", studentsTermCB))
            add(Box.createRigidArea(Dimension(0, gap)))
        }

        val buttonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(10, 0, 0, 0)
            add(signInButton)
            add(Box.createRigidArea(Dimension(0, JBUI.scale(6))))
            add(commitButton)
            add(Box.createRigidArea(Dimension(0, JBUI.scale(4))))
            add(finalSubmissionButton)
        }

        val content = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(8)
            add(studentSection)
            add(Box.createRigidArea(Dimension(0, JBUI.scale(8))))
            add(taskSection)
            add(buttonPanel)
        }

        return JPanel(BorderLayout()).apply {
            add(content, BorderLayout.NORTH)
        }
    }

    private fun labeledRow(labelText: String, component: JComponent): JPanel {
        val labelW = JBUI.scale(120)
        val label = JLabel(labelText).apply {
            preferredSize = Dimension(labelW, preferredSize.height)
            minimumSize = Dimension(labelW, minimumSize.height)
        }
        return JPanel(BorderLayout(JBUI.scale(6), 0)).apply {
            maximumSize = Dimension(Int.MAX_VALUE, component.preferredSize.height + JBUI.scale(2))
            add(label, BorderLayout.WEST)
            add(component, BorderLayout.CENTER)
        }
    }

    private fun wireButtons() {
        signInButton.addActionListener { onBeginClicked() }
        commitButton.addActionListener { onCommitClicked() }
        finalSubmissionButton.addActionListener { onFinalSubmitClicked() }
    }

    private fun onBeginClicked() {
        if (session.isActive) {
            JOptionPane.showMessageDialog(
                null,
                "Izrada zadatka je već započeta. Nastavite rad i predajte ga dugmetom \"Predaj rad\".",
                "Zadatak je već započet",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        val error = FormValidator.validate(
            studentsFirstNameTF.text,
            studentsLastNameTF.text,
            studentsStudyProgramTF.text,
            studentsIndexNumberTF.text,
            studentsStartYearTF.text,
            classroomNameTF.text
        )
        if (error != null) {
            JOptionPane.showMessageDialog(null, error, "Greška u unosu", JOptionPane.WARNING_MESSAGE)
            return
        }

        // Preuzimanje zadatka briše sadržaj otvorenog projekta - traži potvrdu pre bilo čega
        // nepovratnog (pre serverskog `startAssigment`, pre `session.begin`).
        if (!confirmProjectOverwrite()) {
            return
        }

        val downloadPath = Paths.get(System.getProperty("user.home"), RafConfig.DOWNLOAD_FOLDER_NAME)
        if (Files.exists(downloadPath)) {
            downloadPath.toFile().listFiles()?.forEach { it.deleteRecursively() }
        } else {
            Files.createDirectory(downloadPath)
        }

        signInButton.isEnabled = false
        ApplicationManager.getApplication().executeOnPooledThread {
            val success = studentService.startAssigment(
                studentsIndexNumberTF.text.toInt(),
                studentsStartYearTF.text,
                studentsStudyProgramTF.text,
                "",
                studentsFirstNameTF.text,
                studentsLastNameTF.text,
                subjectCB.selectedItem?.toString(),
                testGroupCB.selectedItem?.toString(),
                studentsTermCB.selectedItem?.toString(),
                classroomNameTF.text,
                downloadPath.toString()
            )

            ApplicationManager.getApplication().invokeLater {
                if (success) {
                    val studentId =
                        "${studentsStudyProgramTF.text}-${studentsIndexNumberTF.text}-${studentsStartYearTF.text}"
                    val taskId = "${subjectCB.selectedItem}-${testGroupCB.selectedItem}-${studentsTermCB.selectedItem}"
                    currentStudentId = studentId

                    session.begin(StudentSessionService.State().apply {
                        studentRepoPath = studentService.loggedStudentRepoPath ?: ""
                        this.studentId = studentId
                        this.taskId = taskId
                        firstName = studentsFirstNameTF.text
                        lastName = studentsLastNameTF.text
                        studyProgram = studentsStudyProgramTF.text
                        indexNumber = studentsIndexNumberTF.text
                        startYear = studentsStartYearTF.text
                        classroom = classroomNameTF.text
                        testName = subjectCB.selectedItem?.toString().orEmpty()
                        groupLabel = testGroupCB.selectedItem?.toString().orEmpty()
                        term = studentsTermCB.selectedItem?.toString().orEmpty()
                    })

                    StudentTrackingSession.getInstance(project).start(trackingService, studentId, taskId)

                    AssignmentLoader(project).copyAndLoad()
                    disableFormFields()

                    signInButton.isVisible = false
                    commitButton.isVisible = false
                    finalSubmissionButton.isVisible = true

                    ApplicationManager.getApplication().invokeLater {
                        project.basePath?.let {
                            LocalFileSystem.getInstance().refreshAndFindFileByPath(it)?.refresh(false, true)
                        }
                    }
                } else {
                    signInButton.isEnabled = true
                    JOptionPane.showMessageDialog(
                        null,
                        "Nije uspelo preuzimanje zadatka.",
                        "Greška",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }
    }

    /** Folderi/fajlovi u korenu projekta koje `AssignmentLoader` ionako ne dira. */
    private val overwriteIgnored = setOf(".idea", ".git", ".gradle", "build", "out", ".DS_Store")

    /**
     * Potvrda pre nego što `AssignmentLoader` obriše sadržaj otvorenog projekta i ubaci zadatak.
     * Prazan projekat ne traži potvrdu. Ako projekat ne liči na prazan ispitni (Git sa istorijom
     * ili puno fajlova), dijalog se pojačava. Vraća true samo uz eksplicitnu potvrdu.
     */
    private fun confirmProjectOverwrite(): Boolean {
        val basePath = project.basePath ?: return true
        val projectDir = File(basePath)

        val toDelete = projectDir.listFiles()
            ?.filter { it.name !in overwriteIgnored }
            .orEmpty()
            .sortedBy { it.name.lowercase() }
        if (toDelete.isEmpty()) return true

        val gitHasHistory = File(projectDir, ".git/logs/HEAD").let { it.isFile && it.length() > 0 }
        val risky = gitHasHistory || toDelete.size > 25

        val listing = toDelete.take(15).joinToString("\n") { "   • ${it.name}${if (it.isDirectory) "/" else ""}" }
        val more = (toDelete.size - 15).let { if (it > 0) "\n   … i još $it" else "" }

        val message = buildString {
            if (risky) {
                append("UPOZORENJE: ovo ne liči na prazan ispitni projekat")
                if (gitHasHistory) append(" (Git repozitorijum sa istorijom)")
                append(".\n\n")
            }
            append("Preuzimanje zadatka će OBRISATI sledeći sadržaj projekta\n")
            append(basePath).append(":\n\n")
            append(listing).append(more)
            append("\n\nKopija trenutnog sadržaja se pravi u ~/${RafConfig.BACKUP_FOLDER_NAME}/ i briše se nakon predaje rada.")
            append("\n\nNastaviti?")
        }

        val options = arrayOf<Any>("Obriši i preuzmi zadatak", "Odustani")
        val choice = JOptionPane.showOptionDialog(
            null,
            message,
            "Potvrda preuzimanja zadatka",
            JOptionPane.YES_NO_OPTION,
            if (risky) JOptionPane.ERROR_MESSAGE else JOptionPane.WARNING_MESSAGE,
            null,
            options,
            options[1],
        )
        return choice == JOptionPane.YES_OPTION
    }

    private fun disableFormFields() {
        studentsFirstNameTF.isEnabled = false
        studentsLastNameTF.isEnabled = false
        studentsStudyProgramTF.isEnabled = false
        studentsIndexNumberTF.isEnabled = false
        studentsStartYearTF.isEnabled = false
        classroomNameTF.isEnabled = false
        subjectCB.isEnabled = false
        testGroupCB.isEnabled = false
        studentsTermCB.isEnabled = false
    }

    private fun onCommitClicked() {
        try {
            trackingService.logEvent("SUBMISSION_ATTEMPT", currentStudentId, mapOf("type" to "commit"))
        } catch (e: Throwable) {
            log.warn("Tracking logEvent nije uspeo: ${e.message}")
        }
        ApplicationManager.getApplication().runWriteAction {
            FileDocumentManager.getInstance().saveAllDocuments()
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            studentService.setProjectRoot(project.basePath)
            if (studentService.submitAssignment(false)) {
                ApplicationManager.getApplication().invokeLater {
                    JOptionPane.showMessageDialog(
                        null,
                        "Uspešno ste predali rad!",
                        "Uspešno",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                }
            }
        }
    }

    private fun onFinalSubmitClicked() {
        val confirmed = JOptionPane.showConfirmDialog(
            null,
            "Da li ste sigurni da želite da predate rad?",
            "Potvrda o predaji rada",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        ) == JOptionPane.YES_OPTION

        if (!confirmed) return

        try {
            trackingService.logEvent("SUBMISSION_ATTEMPT", currentStudentId, mapOf("type" to "final"))
        } catch (e: Throwable) {
            log.warn("Tracking logEvent nije uspeo: ${e.message}")
        }
        ApplicationManager.getApplication().runWriteAction {
            FileDocumentManager.getInstance().saveAllDocuments()
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            studentService.setProjectRoot(project.basePath)
            val isPushSuccess = studentService.submitAssignment(true)

            if (isPushSuccess) {
                StudentTrackingSession.getInstance(project).finishAndStop()
                try {
                    trackingService.stopTracking(currentStudentId)
                } catch (e: Throwable) {
                    log.warn("Tracking stopTracking nije uspeo: ${e.message}")
                }
                session.clear()
                AssignmentLoader(project).deleteBackups()
                ApplicationManager.getApplication().invokeLater {
                    finalSubmissionButton.isEnabled = false
                    JOptionPane.showMessageDialog(
                        null,
                        "Uspešno ste predali rad!",
                        "Uspešno",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                }
            } else {
                log.warn("Predaja rada (push) nije uspela.")
                ApplicationManager.getApplication().invokeLater {
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
}
