package com.github.lmitrovic.studenttestingintellijplugin.toolWindow

import com.github.lmitrovic.studenttestingintellijplugin.MyBundle
import com.github.lmitrovic.studenttestingintellijplugin.assignment.AssignmentLoader
import com.github.lmitrovic.studenttestingintellijplugin.assignment.FormValidator
import com.github.lmitrovic.studenttestingintellijplugin.tracking.ActivityTracker
import com.github.lmitrovic.studenttestingintellijplugin.tracking.FeedbackDashboard
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import raflms.studentstub.api.StudentStubService
import raflms.trackingstub.api.TrackingStubService
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import javax.swing.*

class StudentFormPanel(
    private val project: Project,
    private val studentService: StudentStubService,
    private val trackingService: TrackingStubService
) {

    val root: JPanel

    private val studentsFirstNameTF = JTextField()
    private val studentsLastNameTF = JTextField()
    private val studentsStudyProgramTF = JBTextField().apply { emptyText.text = "SI, RI ili RN" }
    private val studentsIndexNumberTF = JTextField()
    private val studentsStartYearTF = JTextField()
    private val studentsTaskGroupTF = JTextField()
    private val classroomNameTF = JTextField()
    private val studentsTermCB = JComboBox<Any>()
    private val testGroupCB = JComboBox<Any>()
    private val subjectCB: JComboBox<Any>

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
        val allTestsData = studentService.allTestsWithAssigmentsData
        subjectCB = JComboBox(allTestsData.map { it.testName }.toTypedArray())

        fun updateGroupsAndTerms() {
            val selectedTestName = subjectCB.selectedItem as? String ?: return
            val selectedTest = allTestsData.find { it.testName == selectedTestName } ?: return
            val assignments = selectedTest.assigments ?: return
            testGroupCB.model = DefaultComboBoxModel(assignments.mapNotNull { it.group }.distinct().toTypedArray())
            studentsTermCB.model = DefaultComboBoxModel(assignments.mapNotNull { it.term }.distinct().toTypedArray())
        }

        subjectCB.addActionListener { updateGroupsAndTerms() }
        updateGroupsAndTerms()

        root = buildPanel()
        wireButtons()
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
            add(labeledRow("Studentska grupa:", studentsTaskGroupTF))
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
        val error = FormValidator.validate(
            studentsFirstNameTF.text,
            studentsLastNameTF.text,
            studentsStudyProgramTF.text,
            studentsIndexNumberTF.text,
            studentsStartYearTF.text,
            studentsTaskGroupTF.text,
            classroomNameTF.text
        )
        if (error != null) {
            JOptionPane.showMessageDialog(null, error, "Greška u unosu", JOptionPane.WARNING_MESSAGE)
            return
        }

        val downloadPath = Paths.get(System.getProperty("user.home"), MyBundle.downloadFolder)
        if (Files.exists(downloadPath)) {
            downloadPath.toFile().listFiles()?.forEach { it.deleteRecursively() }
        } else {
            Files.createDirectory(downloadPath)
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            val success = studentService.startAssigment(
                studentsIndexNumberTF.text.toInt(),
                studentsStartYearTF.text,
                studentsStudyProgramTF.text,
                studentsTaskGroupTF.text,
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

                    try {
                        trackingService.startTracking(studentId, taskId)
                    } catch (e: Throwable) {
                        println("=== TRACKING startTracking FAILED: ${e.message} ===")
                    }
                    println("=== TRACKING STARTED: $studentId, $taskId ===")
                    try {
                        ActivityTracker(project, trackingService, studentId).start()
                    } catch (e: Throwable) {
                        println("=== ActivityTracker FAILED: ${e.message} ===")
                    }
                    try {
                        FeedbackDashboard(project, studentId).start()
                    } catch (e: Throwable) {
                        println("=== FeedbackDashboard FAILED: ${e.message} ===")
                    }

                    AssignmentLoader(project).copyAndLoad()
                    disableFormFields()

                    signInButton.isEnabled = false
                    signInButton.isVisible = false
                    commitButton.isVisible = false
                    finalSubmissionButton.isVisible = true

                    ApplicationManager.getApplication().invokeLater {
                        LocalFileSystem.getInstance().refreshAndFindFileByPath(project.basePath!!)?.refresh(false, true)
                    }
                } else {
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

    private fun disableFormFields() {
        studentsFirstNameTF.isEnabled = false
        studentsLastNameTF.isEnabled = false
        studentsStudyProgramTF.isEnabled = false
        studentsIndexNumberTF.isEnabled = false
        studentsStartYearTF.isEnabled = false
        studentsTaskGroupTF.isEnabled = false
        classroomNameTF.isEnabled = false
        subjectCB.isEnabled = false
        testGroupCB.isEnabled = false
        studentsTermCB.isEnabled = false
    }

    private fun onCommitClicked() {
        try {
            trackingService.logEvent("SUBMISSION_ATTEMPT", currentStudentId, mapOf("type" to "commit"))
        } catch (e: Throwable) {
            println("Tracking logEvent failed: ${e.message}")
        }
        val currentProject = ProjectManager.getInstance().openProjects[0]
        FileDocumentManager.getInstance().saveAllDocuments()

        ApplicationManager.getApplication().executeOnPooledThread {
            studentService.setProjectRoot(currentProject.basePath)
            if (studentService.submitAssignment(false)) {
                JOptionPane.showMessageDialog(
                    null,
                    "Uspešno ste predali rad!",
                    "Uspešno",
                    JOptionPane.INFORMATION_MESSAGE
                )
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
            println("Tracking logEvent failed: ${e.message}")
        }
        val currentProject = ProjectManager.getInstance().openProjects[0]
        FileDocumentManager.getInstance().saveAllDocuments()

        ApplicationManager.getApplication().executeOnPooledThread {
            studentService.setProjectRoot(currentProject.basePath)
            val isPushSuccess = studentService.submitAssignment(true)
            try {
                trackingService.stopTracking(currentStudentId)
            } catch (e: Throwable) {
                println("Tracking stopTracking failed: ${e.message}")
            }

            if (isPushSuccess) {
                finalSubmissionButton.isEnabled = false

                val studentId =
                    "${studentsStudyProgramTF.text}-${studentsIndexNumberTF.text}-${studentsStartYearTF.text}"
                try {
                    FeedbackDashboard(project, studentId).finish()
                } catch (e: Throwable) {
                    println("=== FeedbackDashboard finish FAILED: ${e.message} ===")
                }

                JOptionPane.showMessageDialog(
                    null,
                    "Uspešno ste predali rad!",
                    "Uspešno",
                    JOptionPane.INFORMATION_MESSAGE
                )
            } else {
                println("Failed to push changes to new branch.")
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
