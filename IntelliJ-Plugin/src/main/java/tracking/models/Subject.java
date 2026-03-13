package tracking.models;

public class Subject {
    private Long id;
    private String fullName;
    private String shortName;
    private String schoolYear;

    // Constructors, getters, setters...
    public Subject() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }

    public String getSchoolYear() { return schoolYear; }
    public void setSchoolYear(String schoolYear) { this.schoolYear = schoolYear; }

    @Override
    public String toString() {
        return fullName; // For display in combo boxes
    }
}
