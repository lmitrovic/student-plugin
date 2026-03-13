package tracking;

import java.util.HashMap;
import java.util.Map;

public class StudentEvent {
    private final String eventType;
    private final long timestamp;
    private final String studentId;
    private final String sessionId;
    private final String taskId;
    private final Map<String, Object> eventData;

    public StudentEvent(String eventType, long timestamp, String studentId,
                        String sessionId, String taskId, Map<String, Object> eventData) {
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.studentId = studentId;
        this.sessionId = sessionId;
        this.taskId = taskId;
        this.eventData = eventData != null ? new HashMap<>(eventData) : new HashMap<>();
    }

    // Constructor with default empty eventData
    public StudentEvent(String eventType, long timestamp, String studentId,
                        String sessionId, String taskId) {
        this(eventType, timestamp, studentId, sessionId, taskId, new HashMap<>());
    }

    // Getters
    public String getEventType() { return eventType; }
    public long getTimestamp() { return timestamp; }
    public String getStudentId() { return studentId; }
    public String getSessionId() { return sessionId; }
    public String getTaskId() { return taskId; }
    public Map<String, Object> getEventData() { return new HashMap<>(eventData); }

    @Override
    public String toString() {
        return "StudentEvent{" +
                "eventType='" + eventType + '\'' +
                ", timestamp=" + timestamp +
                ", studentId='" + studentId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", taskId='" + taskId + '\'' +
                ", eventData=" + eventData +
                '}';
    }
}