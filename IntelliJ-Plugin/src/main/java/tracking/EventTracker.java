package tracking;


import com.github.nikolajr93.studenttestingintellijplugin.MyBundle;
import com.intellij.openapi.project.Project;
import tracking.listeners.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EventTracker {
    private final Project project;
    private final EventQueueManager eventQueue;
    private final String sessionId;

    // Keep references to trackers for cleanup
    private FocusEventTracker focusTracker;
    private CodeChangeTracker codeChangeTracker;
    private CompilationTracker compilationTracker;
    private TestExecutionTracker testExecutionTracker;
    private ActivityTracker activityTracker;

    private StruggleDetectionTracker struggleTracker;
    private ErrorPatternTracker errorTracker;

    public EventTracker(Project project) {
        this.project = project;
        this.eventQueue = new EventQueueManager();
        this.sessionId = UUID.randomUUID().toString();
    }

    public void startTracking(String studentId) {
        initializeListeners(studentId);
        logEvent("SESSION_START", studentId);
    }

    public void logEvent(String eventType, String studentId) {
        logEvent(eventType, studentId, new HashMap<>());
    }

    public void logEvent(String eventType, String studentId, Map<String, Object> data) {
        StudentEvent event = new StudentEvent(
                eventType,
                System.currentTimeMillis(),
                studentId,
                sessionId,
                MyBundle.INSTANCE.getExamString(),
                data
        );
        eventQueue.addEvent(event);
    }

    private void initializeListeners(String studentId) {
        // Initialize all listeners
        focusTracker = new FocusEventTracker(this, studentId);
        focusTracker.initialize();

        codeChangeTracker = new CodeChangeTracker(this, studentId, project);
        codeChangeTracker.initialize();

        compilationTracker = new CompilationTracker(this, studentId, project);
        compilationTracker.initialize();

        testExecutionTracker = new TestExecutionTracker(this, studentId, project);
        testExecutionTracker.initialize();

        activityTracker = new ActivityTracker(this, studentId);
        activityTracker.initialize();

        struggleTracker = new StruggleDetectionTracker(this, studentId);
        errorTracker = new ErrorPatternTracker(this, studentId);
    }

    public StruggleDetectionTracker getStruggleTracker() {
        return struggleTracker;
    }

    public ErrorPatternTracker getErrorTracker() {
        return errorTracker;
    }

    public void stopTracking(String studentId) {
        logEvent("SESSION_END", studentId);

        // Clean up trackers
        if (activityTracker != null) {
            activityTracker.cleanup();
        }

        eventQueue.shutdown();
    }
}
