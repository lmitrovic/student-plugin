package tracking.listeners;

import tracking.EventTracker;

import java.util.HashMap;
import java.util.Map;

public class StruggleDetectionTracker {
    private final EventTracker eventTracker;
    private final String studentId;
    private long lastActivityTime = System.currentTimeMillis();
    private int consecutiveCompileFailures = 0;
    private int deleteCount = 0;
    private int writeCount = 0;

    public StruggleDetectionTracker(EventTracker eventTracker, String studentId) {
        this.eventTracker = eventTracker;
        this.studentId = studentId;
    }

    public void updateActivity() {
        this.lastActivityTime = System.currentTimeMillis();
    }

    // Track when students get stuck
    public void detectInactivityPattern() {
        long currentTime = System.currentTimeMillis();
        long inactivityDuration = currentTime - lastActivityTime;

        if (inactivityDuration > 120000) { // 2 minutes
            Map<String, Object> data = new HashMap<>();
            data.put("inactivityDuration", inactivityDuration);

            this.eventTracker.logEvent("PROLONGED_INACTIVITY", this.studentId, data);
        }
    }

    // Track compilation struggles
    public void trackCompilationPattern(boolean success) {
        if (!success) {
            consecutiveCompileFailures++;
            if (consecutiveCompileFailures >= 3) {
                Map<String, Object> data = new HashMap<>();
                data.put("consecutiveFailures", consecutiveCompileFailures);

                this.eventTracker.logEvent("COMPILATION_STRUGGLE_DETECTED", this.studentId, data);
            }
        } else {
            consecutiveCompileFailures = 0;
        }
    }

    // Track code thrashing (lots of deleting/rewriting)
    public void trackCodeChange(boolean isDelete) {
        if (isDelete) {
            deleteCount++;
        } else {
            writeCount++;
        }

        // Check ratio every 20 actions
        if ((deleteCount + writeCount) % 20 == 0 && writeCount > 0) {
            float deleteToWriteRatio = (float) deleteCount / writeCount;

            if (deleteToWriteRatio > 0.8) { // More than 80% deletes
                Map<String, Object> data = new HashMap<>();
                data.put("deleteToWriteRatio", deleteToWriteRatio);
                data.put("deleteCount", deleteCount);
                data.put("writeCount", writeCount);

                this.eventTracker.logEvent("CODE_THRASHING_DETECTED", this.studentId, data);
            }
        }
    }

    public void resetCounters() {
        deleteCount = 0;
        writeCount = 0;
        consecutiveCompileFailures = 0;
    }
}