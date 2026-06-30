package com.github.lmitrovic.studenttestingintellijplugin.startup

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.notification.*
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ShowSettingsUtil
import java.net.URI

class PluginStartupActivity : StartupActivity {

    override fun runActivity(project: Project) {
        ApplicationManager.getApplication().executeOnPooledThread {
            checkPluginUpdate(project)
        }
    }

    private fun checkPluginUpdate(project: Project) {
        try {
            val pluginId = PluginId.getId("com.github.lmitrovic.studenttestingintellijplugin")
            val currentVersion = PluginManagerCore.getPlugin(pluginId)?.version ?: "0.0.0"

            val xmlContent = URI("http://157.180.37.247/updatePluginsStudent.xml").toURL().readText()

            val regex = "version=\"(.*?)\"".toRegex()
            val latestVersion = regex.findAll(xmlContent).lastOrNull()?.groupValues?.get(1) ?: ""

            if (latestVersion.isNotEmpty() && latestVersion != currentVersion) {
                ApplicationManager.getApplication().invokeLater {
                    showUpdateNotification(project, currentVersion, latestVersion)
                }
            }
        } catch (e: Exception) {
            println(e)
        }
    }

    private fun showUpdateNotification(project: Project, oldVersion: String, newVersion: String) {
        println("Nova verzija dostupna: ($newVersion)")
        val notificationGroup = NotificationGroupManager.getInstance()
            .getNotificationGroup("RAF LMS Updates")

        val notification = notificationGroup.createNotification(
            "🚀 Nova verzija je dostupna!",
            "Vaša verzija: $oldVersion -> Nova: $newVersion.\nAžurirajte plugin za nove funkcije.",
            NotificationType.IDE_UPDATE
        )

        notification.addAction(NotificationAction.createSimple("Otvori Plugins") {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, "Plugins")
        })

        Notifications.Bus.notify(notification, project)
    }
}
