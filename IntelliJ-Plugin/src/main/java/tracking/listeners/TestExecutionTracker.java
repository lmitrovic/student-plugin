package tracking.listeners;

import com.intellij.execution.ExecutionListener;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import tracking.EventTracker;

import java.util.HashMap;
import java.util.Map;

public class TestExecutionTracker implements ExecutionListener {
    private final EventTracker eventTracker;
    private final String studentId;
    private final Project project;

    public TestExecutionTracker(EventTracker eventTracker, String studentId, Project project) {
        this.eventTracker = eventTracker;
        this.studentId = studentId;
        this.project = project;
    }

    public void initialize() {
        project.getMessageBus().connect().subscribe(ExecutionManager.EXECUTION_TOPIC, this);
    }

    @Override
    public void processStarted(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler) {
        Map<String, Object> data = new HashMap<>();
        data.put("executorId", executorId);
        data.put("configurationName", env.getRunProfile().getName());

        eventTracker.logEvent("TEST_EXECUTION_STARTED", studentId, data);
    }

    @Override
    public void processTerminated(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler, int exitCode) {
        Map<String, Object> data = new HashMap<>();
        data.put("executorId", executorId);
        data.put("configurationName", env.getRunProfile().getName());
        data.put("exitCode", exitCode);
        data.put("success", exitCode == 0);

        eventTracker.logEvent("TEST_EXECUTION_FINISHED", studentId, data);
    }
}
