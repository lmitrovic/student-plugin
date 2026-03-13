package com.github.nikolajr93.studenttestingintellijplugin.api;

public class StudentInfoDto {
    private String firstName;
    private String lastName;
    private Integer indexNumber;
    private String startYear;
    private String studyProgramShort; // RN, RI - iz indeksa

    public StudentInfoDto(String firstName,
                          String lastName,
                          Integer indexNumber,
                          String startYear,
                          String studyProgramShort
    ) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.indexNumber = indexNumber;
        this.startYear = startYear;
        this.studyProgramShort = studyProgramShort;
    }

    public StudentInfoDto(){

    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Integer getIndexNumber() {
        return indexNumber;
    }

    public void setIndexNumber(Integer indexNumber) {
        this.indexNumber = indexNumber;
    }

    public String getStartYear() {
        return startYear;
    }

    public void setStartYear(String startYear) {
        this.startYear = startYear;
    }

    public String getStudyProgramShort() {
        return studyProgramShort;
    }

    public void setStudyProgramShort(String studyProgramShort) {
        this.studyProgramShort = studyProgramShort;
    }
}
