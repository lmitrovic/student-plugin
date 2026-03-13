package tracking.listeners;


import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.AnActionResult;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import tracking.EventTracker;

import java.util.HashMap;
import java.util.Map;

public class CompilationTracker implements AnActionListener {
    private final EventTracker eventTracker;
    private final String studentId;
    private final Project project;

    public CompilationTracker(EventTracker eventTracker, String studentId, Project project) {
        this.eventTracker = eventTracker;
        this.studentId = studentId;
        this.project = project;
    }

    public void initialize() {
        ApplicationManager.getApplication().getMessageBus()
                .connect(project)
                .subscribe(AnActionListener.TOPIC, this);
    }

    @Override
    public void beforeActionPerformed(AnAction action, AnActionEvent event) {
        String actionId = ActionManager.getInstance().getId(action);
        if (actionId != null && isCompilationAction(actionId)) {
            Map<String, Object> data = new HashMap<>();
            data.put("actionId", actionId);
            data.put("actionType", "compilation_started");

            eventTracker.logEvent("COMPILATION_STARTED", studentId, data);
        }
    }

    @Override
    public void afterActionPerformed(AnAction action, AnActionEvent event, AnActionResult result) {
        String actionId = ActionManager.getInstance().getId(action);
        if (actionId != null && isCompilationAction(actionId)) {
            Map<String, Object> data = new HashMap<>();
            data.put("actionId", actionId);
            data.put("actionType", "compilation_finished");

            // AnActionResult doesn't have direct success/error methods
            // We'll track that the action was performed
            data.put("performed", result.isPerformed());

            eventTracker.logEvent("COMPILATION_FINISHED", studentId, data);
        }
    }

    private boolean isCompilationAction(String actionId) {
        return actionId.contains("Compile") ||
                actionId.contains("Build") ||
                actionId.equals("CompileDirty") ||
                actionId.equals("Compile") ||
                actionId.equals("BuildProject") ||
                actionId.equals("RebuildProject") ||
                actionId.equals("CompileProject");
    }
}
