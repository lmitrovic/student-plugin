package tracking.models;

public class TestGroupInfo {
    private Long testGroupId;
    private String groupNumber;
    private String gitPath;

    public TestGroupInfo() {}

    public TestGroupInfo(Long testGroupId, String groupNumber, String gitPath) {
        this.testGroupId = testGroupId;
        this.groupNumber = groupNumber;
        this.gitPath = gitPath;
    }

    public String getDisplayName() {
        return "Group " + groupNumber;
    }

    public String constructSourceUrl(String baseUrl) {
        return baseUrl + gitPath.replace("/srv/git/", "");
    }

    // Getters, setters...
    public Long getTestGroupId() { return testGroupId; }
    public void setTestGroupId(Long testGroupId) { this.testGroupId = testGroupId; }

    public String getGroupNumber() { return groupNumber; }
    public void setGroupNumber(String groupNumber) { this.groupNumber = groupNumber; }

    public String getGitPath() { return gitPath; }
    public void setGitPath(String gitPath) { this.gitPath = gitPath; }

    @Override
    public String toString() {
        return getDisplayName(); // For display in combo boxes
    }
}
