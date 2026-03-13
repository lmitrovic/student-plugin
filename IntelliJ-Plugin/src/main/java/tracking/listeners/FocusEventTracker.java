package tracking.listeners;

import com.intellij.openapi.wm.WindowManager;
import tracking.EventTracker;

import javax.swing.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

public class FocusEventTracker implements WindowFocusListener {
    private final EventTracker eventTracker;
    private final String studentId;

    public FocusEventTracker(EventTracker eventTracker, String studentId) {
        this.eventTracker = eventTracker;
        this.studentId = studentId;
    }

    public void initialize() {
        JFrame ideFrame = WindowManager.getInstance().getFrame(null);
        if (ideFrame != null) {
            ideFrame.addWindowFocusListener(this);
        }
    }

    @Override
    public void windowGainedFocus(WindowEvent e) {
        eventTracker.logEvent("WINDOW_FOCUS_GAINED", studentId);
    }

    @Override
    public void windowLostFocus(WindowEvent e) {
        eventTracker.logEvent("WINDOW_FOCUS_LOST", studentId);
    }
}