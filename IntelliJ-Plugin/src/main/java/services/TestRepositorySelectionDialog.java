//package services;
//
//import com.github.nikolajr93.studenttestingintellijplugin.api.RafApiClient;
//import tracking.models.Subject;
//import tracking.models.TestGroupInfo;
//
//import javax.swing.*;
//import java.awt.*;
//import java.util.List;
//
//public class TestRepositorySelectionDialog extends DialogWrapper {
//    private JComboBox<Subject> subjectCombo;
//    private JComboBox<String> yearCombo;
//    private JComboBox<String> testTypeCombo;
//    private JComboBox<TestGroupInfo> groupCombo;
//    private JLabel statusLabel;
//
//    public TestRepositorySelectionDialog(Project project) {
//        super(project);
//        setTitle("Select Test Repository");
//        init();
//        loadSubjects();
//    }
//
//    @Override
//    protected JComponent createCenterPanel() {
//        JPanel panel = new JPanel(new GridBagLayout());
//        GridBagConstraints gbc = new GridBagConstraints();
//
//        // Subject selection
//        gbc.gridx = 0; gbc.gridy = 0;
//        panel.add(new JLabel("Subject:"), gbc);
//        gbc.gridx = 1;
//        subjectCombo = new JComboBox<>();
//        subjectCombo.setPreferredSize(new Dimension(200, 25));
//        panel.add(subjectCombo, gbc);
//
//        // Year selection
//        gbc.gridx = 0; gbc.gridy = 1;
//        panel.add(new JLabel("Academic Year:"), gbc);
//        gbc.gridx = 1;
//        yearCombo = new JComboBox<>();
//        yearCombo.setEnabled(false);
//        panel.add(yearCombo, gbc);
//
//        // Test Type selection
//        gbc.gridx = 0; gbc.gridy = 2;
//        panel.add(new JLabel("Test Type:"), gbc);
//        gbc.gridx = 1;
//        testTypeCombo = new JComboBox<>();
//        testTypeCombo.setEnabled(false);
//        panel.add(testTypeCombo, gbc);
//
//        // Group selection
//        gbc.gridx = 0; gbc.gridy = 3;
//        panel.add(new JLabel("Group:"), gbc);
//        gbc.gridx = 1;
//        groupCombo = new JComboBox<>();
//        groupCombo.setEnabled(false);
//        panel.add(groupCombo, gbc);
//
//        // Status label
//        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
//        statusLabel = new JLabel("Loading subjects...");
//        panel.add(statusLabel, gbc);
//
//        setupListeners();
//        return panel;
//    }
//
//    private void setupListeners() {
//        subjectCombo.addActionListener(e -> onSubjectSelected());
//        yearCombo.addActionListener(e -> onYearSelected());
//        testTypeCombo.addActionListener(e -> onTestTypeSelected());
//    }
//
//    private void loadSubjects() {
//        statusLabel.setText("Loading subjects...");
//
//        RafApiClient.getSubjects().thenAccept(subjects -> {
//            SwingUtilities.invokeLater(() -> {
//                subjectCombo.removeAllItems();
//                subjects.forEach(subjectCombo::addItem);
//                statusLabel.setText("Select a subject");
//            });
//        }).exceptionally(throwable -> {
//            SwingUtilities.invokeLater(() -> {
//                statusLabel.setText("Error loading subjects: " + throwable.getMessage());
//            });
//            throwable.printStackTrace();
//            return null;
//        });
//    }
//
//    private void onSubjectSelected() {
//        Subject selected = (Subject) subjectCombo.getSelectedItem();
//        if (selected == null) return;
//
//        // Clear dependent combos
//        clearCombo(yearCombo);
//        clearCombo(testTypeCombo);
//        clearCombo(groupCombo);
//
//        statusLabel.setText("Loading years for " + selected.getShortName() + "...");
//
//        RafApiClient.getYears(selected.getShortName()).thenAccept(years -> {
//            SwingUtilities.invokeLater(() -> {
//                populateCombo(yearCombo, years);
//                statusLabel.setText("Select an academic year");
//            });
//        }).exceptionally(throwable -> {
//            SwingUtilities.invokeLater(() -> {
//                statusLabel.setText("Error loading years: " + throwable.getMessage());
//            });
//            return null;
//        });
//    }
//
//    private void onYearSelected() {
//        Subject subject = (Subject) subjectCombo.getSelectedItem();
//        String year = (String) yearCombo.getSelectedItem();
//        if (subject == null || year == null) return;
//
//        // Clear dependent combos
//        clearCombo(testTypeCombo);
//        clearCombo(groupCombo);
//
//        statusLabel.setText("Loading test types...");
//
//        RafApiClient.getTestTypes(subject.getShortName(), year).thenAccept(testTypes -> {
//            SwingUtilities.invokeLater(() -> {
//                populateCombo(testTypeCombo, testTypes);
//                statusLabel.setText("Select a test type");
//            });
//        }).exceptionally(throwable -> {
//            SwingUtilities.invokeLater(() -> {
//                statusLabel.setText("Error loading test types: " + throwable.getMessage());
//            });
//            return null;
//        });
//    }
//
//    private void onTestTypeSelected() {
//        Subject subject = (Subject) subjectCombo.getSelectedItem();
//        String year = (String) yearCombo.getSelectedItem();
//        String testType = (String) testTypeCombo.getSelectedItem();
//        if (subject == null || year == null || testType == null) return;
//
//        clearCombo(groupCombo);
//        statusLabel.setText("Loading groups...");
//
//        RafApiClient.getGroups(subject.getShortName(), year, testType).thenAccept(groups -> {
//            SwingUtilities.invokeLater(() -> {
//                groupCombo.removeAllItems();
//                groups.forEach(groupCombo::addItem);
//                groupCombo.setEnabled(true);
//                statusLabel.setText("Select a group");
//            });
//        }).exceptionally(throwable -> {
//            SwingUtilities.invokeLater(() -> {
//                statusLabel.setText("Error loading groups: " + throwable.getMessage());
//            });
//            return null;
//        });
//    }
//
//    private <T> void clearCombo(JComboBox<T> combo) {
//        combo.removeAllItems();
//        combo.setEnabled(false);
//    }
//
//    private <T> void populateCombo(JComboBox<T> combo, List<T> items) {
//        combo.removeAllItems();
//        items.forEach(combo::addItem);
//        combo.setEnabled(true);
//    }
//
//    public TestGroupInfo getSelectedGroup() {
//        return (TestGroupInfo) groupCombo.getSelectedItem();
//    }
//
//    @Override
//    protected boolean isOKActionEnabled() {
//        return groupCombo.getSelectedItem() != null;
//    }
//}
