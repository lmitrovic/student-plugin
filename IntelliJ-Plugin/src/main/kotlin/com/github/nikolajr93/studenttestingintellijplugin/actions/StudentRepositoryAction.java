package com.github.nikolajr93.studenttestingintellijplugin.actions;

import com.github.nikolajr93.studenttestingintellijplugin.GitServerHttpService;
import services.TestRepositorySelectionDialog;
import tracking.models.TestGroupInfo;

import javax.swing.*;

public class StudentRepositoryAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        TestRepositorySelectionDialog dialog = new TestRepositorySelectionDialog(project);
        if (dialog.showAndGet()) {
            TestGroupInfo selectedGroup = dialog.getSelectedGroup();
            cloneSelectedRepository(selectedGroup, project);
        }
    }

    private void cloneSelectedRepository(TestGroupInfo groupInfo, Project project) {
        // Get current student ID (however you determine this)
        String studentId = getCurrentStudentId(); // Implement this method

        // Construct source URL
        String sourceUrl = groupInfo.constructSourceUrl(Config.HTTP_REPO_BASE_URL);

        // Determine local path
        String localPath = getLocalWorkspacePath(groupInfo, studentId);

        // Show progress dialog
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Cloning Repository", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("Cloning " + groupInfo.getDisplayName() + "...");

                try {
                    // Use existing forkAndCloneRepository method
                    GitServerHttpService.forkAndCloneRepository(sourceUrl, studentId, localPath);

                    SwingUtilities.invokeLater(() -> {
                        Messages.showInfoMessage(
                                project,
                                "Repository cloned successfully to: " + localPath,
                                "Success"
                        );
                    });

                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        Messages.showErrorDialog(
                                project,
                                "Failed to clone repository: " + ex.getMessage(),
                                "Error"
                        );
                    });
                }
            }
        });
    }

    private String getCurrentStudentId() {
        // Implement logic to get current student ID
        // Could be from settings, user input, etc.
        return "Petar_Petrovic_M_41_23"; // Example
    }

    private String getLocalWorkspacePath(TestGroupInfo groupInfo, String studentId) {
        // Create local path based on group info
        return System.getProperty("user.home") + "/StudentProjects/" +
                groupInfo.getGitPath().replace("/srv/git/", "").replace("/", "_") +
                "_" + studentId;
    }
}