package tracking.models;

import java.time.LocalDateTime;
import java.util.Map;

public class StudentEventDto {
    private String studentId;
    private String sessionId;
    private String eventType;
    private LocalDateTime timestamp;
    private Map<String, Object> eventData;
    private String taskId;

    public StudentEventDto() {}

    public StudentEventDto(String studentId, String sessionId, String eventType,
                           LocalDateTime timestamp, Map<String, Object> eventData, String taskId) {
        this.studentId = studentId;
        this.sessionId = sessionId;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.eventData = eventData;
        this.taskId = taskId;
    }

    // Getters and Setters
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Map<String, Object> getEventData() { return eventData; }
    public void setEventData(Map<String, Object> eventData) { this.eventData = eventData; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
}