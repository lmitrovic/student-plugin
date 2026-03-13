package com.github.nikolajr93.studenttestingintellijplugin.api;

public class TaskSubmissionInfo {
    private String forkName;

    public TaskSubmissionInfo() {}

    public TaskSubmissionInfo(String forkName) {
        this.forkName = forkName;
    }

    public String getForkName() {
        return forkName;
    }

    public void setForkName(String forkName) {
        this.forkName = forkName;
    }
}
