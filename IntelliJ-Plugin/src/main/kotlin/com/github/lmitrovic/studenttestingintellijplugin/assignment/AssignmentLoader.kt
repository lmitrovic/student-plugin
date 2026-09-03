package com.github.lmitrovic.studenttestingintellijplugin.assignment

import com.github.lmitrovic.studenttestingintellijplugin.config.RafConfig
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.idea.maven.project.MavenProjectsManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import javax.swing.JOptionPane

class AssignmentLoader(private val project: Project) {

    private val log = thisLogger()

    /** Fajlovi/folderi koji se nikad ne diraju pri zameni sadržaja projekta. */
    private val preserved = setOf(".idea", ".git")

    fun copyAndLoad() {
        ApplicationManager.getApplication().invokeLater {
            val projectDir = File(project.basePath ?: return@invokeLater)
            val assignmentSource = File(System.getProperty("user.home"), RafConfig.DOWNLOAD_FOLDER_NAME)

            FileDocumentManager.getInstance().saveAllDocuments()
            VirtualFileManager.getInstance().syncRefresh()

            val sourceVf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(assignmentSource)
            val incoming = sourceVf?.children?.filter { it.name !in preserved }.orEmpty()

            // Zaštita: ako je preuzimanje zakazalo i folder je prazan, ne diramo projekat.
            if (incoming.isEmpty()) {
                log.warn("Preuzeti zadatak je prazan (${assignmentSource.path}) - sadržaj projekta ostaje netaknut.")
                JOptionPane.showMessageDialog(
                    null,
                    "Preuzimanje zadatka nije donelo nijedan fajl. Vaš postojeći sadržaj nije menjan.\n" +
                        "Pokušajte ponovo ili se obratite dežurnom nastavniku.",
                    "Preuzimanje neuspešno",
                    JOptionPane.ERROR_MESSAGE
                )
                return@invokeLater
            }

            backupCurrentProject(projectDir)

            WriteAction.run<Throwable> {
                val projectVf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(projectDir) ?: return@run
                projectVf.children
                    .filter { it.name !in preserved }
                    .forEach { it.delete(this) }

                incoming.forEach { child -> VfsUtil.copy(this, child, projectVf) }
            }

            assignmentSource.deleteRecursively()

            VirtualFileManager.getInstance().syncRefresh()
            loadProjectStructure()
        }
    }

    private val backupRoot: File
        get() = File(System.getProperty("user.home"), RafConfig.BACKUP_FOLDER_NAME)

    /**
     * Best-effort kopija trenutnog sadržaja projekta u `~/<BACKUP_FOLDER_NAME>/<projekat>-<vreme>/`
     * pre nego što se pregazi. Briše se posle uspešne predaje rada ([deleteBackups]). Ako ne
     * uspe, samo se loguje - izrada zadatka se svejedno nastavlja.
     */
    private fun backupCurrentProject(projectDir: File) {
        try {
            val entries = projectDir.listFiles()
                ?.filter { it.name !in preserved && it.name != ".gradle" && it.name != "build" && it.name != "out" }
                .orEmpty()
            if (entries.isEmpty()) return

            backupRoot.mkdirs()

            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss").format(Date())
            val target = File(backupRoot, "${projectDir.name}-$stamp")
            entries.forEach { it.copyRecursively(File(target, it.name), overwrite = true) }
            log.info("Backup postojećeg projekta: ${target.path}")
        } catch (e: Throwable) {
            log.warn("Backup projekta nije uspeo (nastavljam sa preuzimanjem zadatka)", e)
        }
    }

    /**
     * Briše sve backup-e ovog projekta iz `~/<BACKUP_FOLDER_NAME>/`. Poziva se posle uspešne
     * predaje rada - tada backup više nije potreban.
     */
    fun deleteBackups() {
        try {
            val name = File(project.basePath ?: return).name
            backupRoot.listFiles { f -> f.isDirectory && f.name.startsWith("$name-") }
                ?.forEach { it.deleteRecursively() }
        } catch (e: Throwable) {
            log.warn("Brisanje backup-a nije uspelo", e)
        }
    }

    private fun loadProjectStructure() {
        val basePath = project.basePath ?: return
        val hasPom = LocalFileSystem.getInstance().refreshAndFindFileByPath("$basePath/pom.xml") != null
        if (hasPom) {
            try {
                MavenProjectsManager.getInstance(project).forceUpdateAllProjectsOrFindAllAvailablePomFiles()
            } catch (e: Throwable) {
                log.warn("Maven import nije uspeo", e)
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
            log.warn("Reload Java modula nije uspeo", e)
        }
    }
}
