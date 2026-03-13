package tracking.listeners;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import tracking.EventTracker;

import java.util.HashMap;
import java.util.Map;

public class CodeChangeTracker {
    private final EventTracker eventTracker;
    private final String studentId;
    private final Project project;

    public CodeChangeTracker(EventTracker eventTracker, String studentId, Project project) {
        this.eventTracker = eventTracker;
        this.studentId = studentId;
        this.project = project;
    }

    public void initialize() {
        EditorFactory editorFactory = EditorFactory.getInstance();
        editorFactory.addEditorFactoryListener(new EditorFactoryListener() {
            @Override
            public void editorCreated(EditorFactoryEvent event) {
                Editor editor = event.getEditor();
                editor.getDocument().addDocumentListener(new DocumentListener() {
                    @Override
                    public void documentChanged(DocumentEvent docEvent) {
                        Map<String, Object> data = new HashMap<>();

                        // Get the virtual file through FileDocumentManager
                        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
                        String fileName = virtualFile != null ? virtualFile.getName() : "unknown";

                        data.put("file", fileName);
                        data.put("line", editor.getCaretModel().getLogicalPosition().line);
                        data.put("changeLength", docEvent.getNewLength() - docEvent.getOldLength());

                        eventTracker.logEvent("CODE_CHANGE", studentId, data);
                    }
                });
            }
        }, project);
    }
}