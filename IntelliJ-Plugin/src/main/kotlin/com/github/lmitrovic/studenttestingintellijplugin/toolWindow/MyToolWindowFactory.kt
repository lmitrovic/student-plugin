package com.github.lmitrovic.studenttestingintellijplugin.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import raflms.studentstub.api.StudentStubService
import raflms.studentstub.config.ConfigFactory
import raflms.trackingstub.api.TrackingStubService
import raflms.trackingstub.config.ConfigFactory as TrackingConfigFactory

class MyToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val studentService = StudentStubService(ConfigFactory.createConfig())
        val trackingService = TrackingStubService(TrackingConfigFactory.createConfig())
        val panel = StudentFormPanel(project, studentService, trackingService)
        val content = ContentFactory.getInstance().createContent(panel.root, "", false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true
}
