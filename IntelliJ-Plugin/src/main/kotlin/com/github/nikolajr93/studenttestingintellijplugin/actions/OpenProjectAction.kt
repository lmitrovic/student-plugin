package com.github.nikolajr93.studenttestingintellijplugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile

class OpenProjectAction : AnAction() {

        override fun actionPerformed(event: AnActionEvent){
            val projectToOpen = "C:\\Users\\P53\\Documents\\RAF\\NVPTest\\restDemo"
            val projectManager = ProjectManager.getInstance()

            ApplicationManager.getApplication().invokeLater(Runnable {
                ApplicationManager.getApplication().runReadAction {
                    val openProjects = projectManager.openProjects

                    if (openProjects.none { it.projectFilePath == projectToOpen }) {
                        projectManager.loadAndOpenProject(projectToOpen)
                    }
                }
            })
        }
}