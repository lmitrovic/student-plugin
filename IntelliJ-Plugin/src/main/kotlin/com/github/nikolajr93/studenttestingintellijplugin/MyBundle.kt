package com.github.nikolajr93.studenttestingintellijplugin

import com.github.nikolajr93.studenttestingintellijplugin.api.Student
import com.github.nikolajr93.studenttestingintellijplugin.api.StudentInfoDto
import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.MyBundle"

object MyBundle : DynamicBundle(BUNDLE) {

    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
        getMessage(key, *params)

    @Suppress("unused")
    @JvmStatic
    fun messagePointer(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
        getLazyMessage(key, *params)

    var repoCloned = false
    var returnedStudent = Student()
    var returnedStudent2 = StudentInfoDto()
    var returnedStudentString = ""
    var studentToken = ""
    var strictStudentToken = ""
    var strictStudentTokenAlternative = ""
    var examString = ""
    //    Get the value from environment variables
    var studentId = "DN62025"
    var testRepoMessage = ""
    var studentForkMessage = ""
    var testRepoPath = "zadatak-1-92a07e95-c5d5-4895-97d0-0bef6ae2e780.git"
    var studentForkPath = ""

    var username = "lmitrovic625dn"
    //    var username = ""
    var builtStudentId = ""

    var computerName = ""
    var currUsername = "lmitrovic625dn"
    var classroom = "Raf7"

    var currentProjectBasePath = ""

    var downloadFolder = "student-plugin-temp"
}
