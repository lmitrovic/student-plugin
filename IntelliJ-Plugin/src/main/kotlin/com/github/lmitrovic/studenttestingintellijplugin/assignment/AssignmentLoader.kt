package com.github.lmitrovic.studenttestingintellijplugin.assignment

import com.github.lmitrovic.studenttestingintellijplugin.MyBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.idea.maven.project.MavenProjectsManager
import java.io.File

class AssignmentLoader(private val project: Project) {

    fun copyAndLoad() {
        ApplicationManager.getApplication().invokeLater {
            val projectDir = File(project.basePath ?: return@invokeLater)
            val assignmentSource = File(System.getProperty("user.home"), MyBundle.downloadFolder)

            FileDocumentManager.getInstance().saveAllDocuments()
            VirtualFileManager.getInstance().syncRefresh()

            WriteAction.run<Throwable> {
                val projectVf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(projectDir) ?: return@run
                projectVf.children
                    .filter { it.name != ".idea" && it.name != ".git" }
                    .forEach { it.delete(this) }

                val sourceVf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(assignmentSource) ?: return@run
                sourceVf.children
                    .filter { it.name != ".idea" && it.name != ".git" }
                    .forEach { child -> VfsUtil.copy(this, child, projectVf) }

                File(System.getProperty("user.home"), MyBundle.downloadFolder).deleteRecursively()
            }

            VirtualFileManager.getInstance().syncRefresh()
            loadProjectStructure()
        }
    }

    private fun loadProjectStructure() {
        val basePath = project.basePath ?: return
        val hasPom = LocalFileSystem.getInstance().refreshAndFindFileByPath("$basePath/pom.xml") != null
        if (hasPom) {
            try {
                MavenProjectsManager.getInstance(project).forceUpdateAllProjectsOrFindAllAvailablePomFiles()
            } catch (e: Throwable) {
                println("Maven import failed: ${e.message}")
            }
        } else {
            reloadJavaModules()
        }
    }

    private fun reloadJavaModules() {
        try {
            val basePath = project.basePath ?: return
            val imlFiles = File(basePath).listFiles { f -> f.extension == "iml" } ?: return
            if (imlFiles.isEmpty()) return

            val entries = imlFiles.joinToString("\n      ") { iml ->
                "<module fileurl=\"file://\$PROJECT_DIR\$/${iml.name}\" filepath=\"\$PROJECT_DIR\$/${iml.name}\" />"
            }
            val modulesXml = """<?xml version="1.0" encoding="UTF-8"?>
<project version="4">
  <component name="ProjectModuleManager">
    <modules>
      $entries
    </modules>
  </component>
</project>"""

            File(basePath, ".idea").mkdirs()
            File(basePath, ".idea/modules.xml").writeText(modulesXml)
            LocalFileSystem.getInstance().refreshAndFindFileByPath("$basePath/.idea")?.refresh(false, true)

            WriteAction.run<Throwable> {
                val moduleManager = ModuleManager.getInstance(project)
                val model = moduleManager.getModifiableModel()
                model.modules.forEach { model.disposeModule(it) }
                imlFiles.forEach { iml ->
                    val vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(iml) ?: return@forEach
                    model.loadModule(vFile.path)
                }
                model.commit()
            }
        } catch (e: Throwable) {
            println("Java module reload failed: ${e.message}")
        }
    }
}
