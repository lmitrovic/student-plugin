package com.github.lmitrovic.studenttestingintellijplugin.assignment

object FormValidator {

    fun validate(
        firstName: String,
        lastName: String,
        studyProgram: String,
        indexNumber: String,
        startYear: String,
        classroom: String
    ): String? {
        if (firstName.isBlank())    return "Ime je obavezno."
        if (lastName.isBlank())     return "Prezime je obavezno."
        if (studyProgram.isBlank()) return "Studijski program je obavezan."
        if (indexNumber.isBlank())  return "Broj indeksa je obavezan."
        if (indexNumber.toIntOrNull() == null) return "Broj indeksa mora biti broj."
        if (startYear.isBlank())    return "Godina upisa je obavezna."
        if (startYear.toIntOrNull() == null)   return "Godina upisa mora biti broj."
        if (classroom.isBlank())    return "Učionica je obavezna."
        return null
    }
}
