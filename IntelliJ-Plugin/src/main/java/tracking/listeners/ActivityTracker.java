package tracking.listeners;

import tracking.EventTracker;

import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

public class ActivityTracker implements AWTEventListener {
    private final EventTracker eventTracker;
    private final String studentId;
    private long lastKeyboardActivity = 0;
    private long lastMouseActivity = 0;
    private static final long ACTIVITY_THRESHOLD = 1000; // 1 second threshold

    public ActivityTracker(EventTracker eventTracker, String studentId) {
        this.eventTracker = eventTracker;
        this.studentId = studentId;
    }

    public void initialize() {
        Toolkit.getDefaultToolkit().addAWTEventListener(
                this,
                AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK
        );
    }

    @Override
    public void eventDispatched(AWTEvent event) {
        long currentTime = System.currentTimeMillis();

        if (event instanceof MouseEvent) {
            MouseEvent mouseEvent = (MouseEvent) event;
            if (mouseEvent.getID() == MouseEvent.MOUSE_CLICKED) {
                // Only log if enough time has passed since last mouse activity
                if (currentTime - lastMouseActivity > ACTIVITY_THRESHOLD) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("button", mouseEvent.getButton());
                    data.put("clickCount", mouseEvent.getClickCount());

                    eventTracker.logEvent("MOUSE_ACTIVITY", studentId, data);
                    lastMouseActivity = currentTime;
                }
            }
        } else if (event instanceof KeyEvent) {
            KeyEvent keyEvent = (KeyEvent) event;
            if (keyEvent.getID() == KeyEvent.KEY_PRESSED) {
                // Only log if enough time has passed since last keyboard activity
                if (currentTime - lastKeyboardActivity > ACTIVITY_THRESHOLD) {
                    // Don't log actual keystrokes for privacy, just activity
                    eventTracker.logEvent("KEYBOARD_ACTIVITY", studentId);
                    lastKeyboardActivity = currentTime;
                }
            }
        }
    }

    public void cleanup() {
        Toolkit.getDefaultToolkit().removeAWTEventListener(this);
    }
}