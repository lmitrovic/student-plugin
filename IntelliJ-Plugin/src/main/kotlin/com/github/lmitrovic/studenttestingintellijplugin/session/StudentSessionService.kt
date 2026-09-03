package com.github.lmitrovic.studenttestingintellijplugin.session

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * Čuva stanje aktivne izrade zadatka po projektu, tako da student može da nastavi rad
 * i preda ga i nakon restarta IntelliJ-a. Stanje ide u `.idea/workspace.xml`.
 */
@Service(Service.Level.PROJECT)
@State(
    name = "RafStudentSession",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
class StudentSessionService : PersistentStateComponent<StudentSessionService.State> {

    class State {
        /** Da li je izrada zadatka u toku. */
        var active: Boolean = false

        /** `studentFolderPath` iz odgovora na `startAssigment` - jedino što je potrebno za predaju. */
        var studentRepoPath: String = ""

        var studentId: String = ""
        var taskId: String = ""
        var startedAtMillis: Long = 0L

        // Vrednosti forme - za ponovni prikaz koji se zadatak radi.
        var firstName: String = ""
        var lastName: String = ""
        var studyProgram: String = ""
        var indexNumber: String = ""
        var startYear: String = ""
        var classroom: String = ""
        var testName: String = ""
        var groupLabel: String = ""
        var term: String = ""
    }

    private var state = State()

    override fun getState(): State = state

    override fun loadState(loaded: State) {
        state = loaded
    }

    /** Trenutno stanje sesije (za čitanje). */
    val current: State
        get() = state

    val isActive: Boolean
        get() = state.active && state.studentRepoPath.isNotBlank()

    fun begin(newState: State) {
        newState.active = true
        if (newState.startedAtMillis == 0L) {
            newState.startedAtMillis = System.currentTimeMillis()
        }
        state = newState
    }

    fun clear() {
        state = State()
    }

    companion object {
        fun getInstance(project: Project): StudentSessionService = project.service()
    }
}
