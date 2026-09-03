package com.github.lmitrovic.studenttestingintellijplugin.startup

import com.github.lmitrovic.studenttestingintellijplugin.config.RafConfig
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.util.io.HttpRequests
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PluginStartupActivity : ProjectActivity {

    private val log = thisLogger()

    private val pluginId = PluginId.getId("com.github.lmitrovic.studenttestingintellijplugin")
    private val versionRegex = "version=\"(.*?)\"".toRegex()

    override suspend fun execute(project: Project) {
        try {
            val currentVersion = PluginManagerCore.getPlugin(pluginId)?.version ?: "0.0.0"

            val xmlContent = withContext(Dispatchers.IO) {
                HttpRequests.request(RafConfig.pluginUpdateXmlUrl)
                    .connectTimeout(RafConfig.HTTP_CONNECT_TIMEOUT_MS)
                    .readTimeout(RafConfig.HTTP_READ_TIMEOUT_MS)
                    .readString()
            }

            val latestVersion = versionRegex.findAll(xmlContent).lastOrNull()?.groupValues?.get(1).orEmpty()

            if (latestVersion.isNotEmpty() && latestVersion != currentVersion) {
                ApplicationManager.getApplication().invokeLater {
                    showUpdateNotification(project, currentVersion, latestVersion)
                }
            }
        } catch (e: Exception) {
            log.info("Provera nove verzije plugina nije uspela: ${e.message}")
        }
    }

    private fun showUpdateNotification(project: Project, oldVersion: String, newVersion: String) {
        log.info("Nova verzija plugina dostupna: $newVersion (trenutna: $oldVersion)")

        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup("RAF LMS Updates")
            .createNotification(
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
