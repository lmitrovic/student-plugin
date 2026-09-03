package com.github.lmitrovic.studenttestingintellijplugin.session

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import raflms.studentstub.api.StudentStubService
import raflms.studentstub.config.ConfigFactory
import raflms.trackingstub.api.TrackingStubService
import raflms.trackingstub.config.ConfigFactory as TrackingConfigFactory

/**
 * Deljene instance stub servisa po projektu. Ranije su se pravile pri svakom otvaranju
 * tool window-a - što je značilo mrežni poziv na EDT-u svaki put i gubitak stanja
 * (`loggedStudentRepoPath`) između otvaranja.
 */
@Service(Service.Level.PROJECT)
class RafStubServices {

    val studentService: StudentStubService by lazy { StudentStubService(ConfigFactory.createConfig()) }

    val trackingService: TrackingStubService by lazy { TrackingStubService(TrackingConfigFactory.createConfig()) }

    companion object {
        fun getInstance(project: Project): RafStubServices = project.service()
    }
}
