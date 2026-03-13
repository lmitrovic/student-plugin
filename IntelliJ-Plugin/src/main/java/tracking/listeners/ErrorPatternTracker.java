package tracking.listeners;

import tracking.EventTracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ErrorPatternTracker {
    private final EventTracker eventTracker;
    private final String studentId;
    private final Map<String, Integer> errorTypes = new HashMap<>();
    private final List<String> recentErrors = new ArrayList<>();

    // Constructor that receives eventTracker
    public ErrorPatternTracker(EventTracker eventTracker, String studentId) {
        this.eventTracker = eventTracker;
        this.studentId = studentId;
    }

    public void trackError(String errorMessage, String errorType) {
        errorTypes.put(errorType, errorTypes.getOrDefault(errorType, 0) + 1);
        recentErrors.add(errorMessage);

        // Keep only recent errors (last 10)
        if (recentErrors.size() > 10) {
            recentErrors.remove(0);
        }

        // Detect if same error keeps recurring
        long occurrences = recentErrors.stream().filter(error -> error.equals(errorMessage)).count();
        if (occurrences >= 3) {
            Map<String, Object> data = new HashMap<>();
            data.put("errorMessage", errorMessage);
            data.put("errorType", errorType);
            data.put("occurrences", occurrences);

            eventTracker.logEvent("PERSISTENT_ERROR_DETECTED", studentId, data);
        }

        // Log general error pattern
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("errorMessage", errorMessage);
        errorData.put("errorType", errorType);
        errorData.put("totalErrorsOfThisType", errorTypes.get(errorType));

        eventTracker.logEvent("ERROR_OCCURRED", studentId, errorData);
    }

    public Map<String, Integer> getErrorTypeCounts() {
        return new HashMap<>(errorTypes);
    }

    public void clearRecentErrors() {
        recentErrors.clear();
    }
}
