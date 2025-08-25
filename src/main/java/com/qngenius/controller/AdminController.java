package com.qngenius.controller;

import com.qngenius.model.Blueprint;
import com.qngenius.model.Course;
import com.qngenius.model.ExamType;
import com.qngenius.model.Subject;
import com.qngenius.model.Unit;
import com.qngenius.util.DatabaseUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class AdminController {

    @FXML private Label usernameLabel;

    // Academic Data FXML Fields
    @FXML private TextField courseCodeField;
    @FXML private TextField courseNameField;
    @FXML private TableView<Course> coursesTable;
    @FXML private TableColumn<Course, String> courseCodeColumn;
    @FXML private TableColumn<Course, String> courseNameColumn;
    @FXML private ComboBox<String> courseSelectForSubject;
    @FXML private TextField subjectCodeField;
    @FXML private TextField subjectNameField;
    @FXML private TableView<Subject> subjectsTable;
    @FXML private TableColumn<Subject, String> subjectCodeColumn;
    @FXML private TableColumn<Subject, String> subjectNameColumn;
    @FXML private ComboBox<String> subjectSelectForUnit;
    @FXML private TextField unitNameField;
    @FXML private TableView<Unit> unitsTable;
    @FXML private TableColumn<Unit, String> unitNameColumn;

    // Blueprint FXML Fields
    @FXML private ComboBox<String> subjectSelectForBlueprint;
    @FXML private ComboBox<String> examTypeSelectForBlueprint;
    @FXML private TextField blueprintTitleField;
    @FXML private TextField totalMarksField;
    @FXML private TextField durationField;
    @FXML private TableView<Blueprint> blueprintsTable;
    @FXML private TableColumn<Blueprint, String> blueprintTitleColumn;
    @FXML private TableColumn<Blueprint, String> blueprintTotalMarksColumn;
    @FXML private TableColumn<Blueprint, String> blueprintDurationColumn;

    // Data lists
    private ObservableList<Course> allCourses = FXCollections.observableArrayList();
    private ObservableList<Subject> allSubjects = FXCollections.observableArrayList();
    private ObservableList<Unit> allUnits = FXCollections.observableArrayList();
    private ObservableList<ExamType> allExamTypes = FXCollections.observableArrayList();
    private ObservableList<Blueprint> allBlueprints = FXCollections.observableArrayList();

    // To hold UUIDs of selected items
    private UUID selectedCourseId;
    private UUID selectedSubjectIdForUnit;
    private UUID selectedSubjectIdForBlueprint;
    private UUID selectedExamTypeId;

    public void setUsername(String username) {
        usernameLabel.setText("Welcome, " + username + "!");
    }

    public void setUserId(UUID userId) {
        // ...
    }
    
    @FXML
    public void initialize() {
        // Initialize Course Table
        courseCodeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCourseCode()));
        courseNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCourseName()));
        
        // Initialize Subject Table
        subjectCodeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSubjectCode()));
        subjectNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSubjectName()));
        
        // Initialize Unit Table
        unitNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUnitName()));
        
        // Initialize Blueprint Table
        blueprintTitleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        blueprintTotalMarksColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getTotalMarks())));
        blueprintDurationColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getDurationMinutes())));
        
        // Load initial data for combo boxes and tables
        loadCourses();
        loadExamTypes();
        
        // Add listeners to enable dynamic loading
        courseSelectForSubject.setOnAction(event -> {
            String selectedCourseCode = courseSelectForSubject.getSelectionModel().getSelectedItem();
            if (selectedCourseCode != null) {
                for (Course c : allCourses) {
                    if (c.getCourseCode().equals(selectedCourseCode)) {
                        selectedCourseId = c.getId();
                        break;
                    }
                }
                loadSubjects();
            }
        });

        subjectSelectForUnit.setOnAction(event -> {
            String selectedSubjectCode = subjectSelectForUnit.getSelectionModel().getSelectedItem();
            if (selectedSubjectCode != null) {
                for (Subject s : allSubjects) {
                    if (s.getSubjectCode().equals(selectedSubjectCode)) {
                        selectedSubjectIdForUnit = s.getId();
                        break;
                    }
                }
                loadUnits();
            }
        });
        
        subjectSelectForBlueprint.setOnAction(event -> {
            String selectedSubjectCode = subjectSelectForBlueprint.getSelectionModel().getSelectedItem();
            if (selectedSubjectCode != null) {
                for (Subject s : allSubjects) {
                    if (s.getSubjectCode().equals(selectedSubjectCode)) {
                        selectedSubjectIdForBlueprint = s.getId();
                        break;
                    }
                }
                loadBlueprints();
            }
        });
        
        examTypeSelectForBlueprint.setOnAction(event -> {
            String selectedExamTypeName = examTypeSelectForBlueprint.getSelectionModel().getSelectedItem();
            if (selectedExamTypeName != null) {
                for (ExamType et : allExamTypes) {
                    if (et.getTypeName().equals(selectedExamTypeName)) {
                        selectedExamTypeId = et.getId();
                        break;
                    }
                }
            }
        });
    }

    @FXML
    private void handleLogoutButtonAction(javafx.event.ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/qngenius/view/login.fxml"));
        Parent loginPage = loader.load();
        Scene loginScene = new Scene(loginPage);
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(loginScene);
        window.setMaximized(true);
        window.show();
    }
    
    // --- Course Management ---
    @FXML
    private void handleAddCourse() {
        String courseCode = courseCodeField.getText();
        String courseName = courseNameField.getText();
        if (courseCode.isEmpty() || courseName.isEmpty()) return;
        try {
            DatabaseUtil.addCourse(courseCode, courseName);
            loadCourses();
            courseCodeField.clear();
            courseNameField.clear();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to add course. It may already exist.");
        }
    }
    private void loadCourses() {
        try {
            allCourses.clear();
            allCourses.addAll(DatabaseUtil.getAllCourses());
            coursesTable.setItems(allCourses);
            courseSelectForSubject.getItems().clear();
            subjectSelectForBlueprint.getItems().clear();
            for (Course c : allCourses) {
                courseSelectForSubject.getItems().add(c.getCourseCode());
                // Add subjects for blueprint selection
                for (Subject s : DatabaseUtil.getSubjectsByCourse(c.getId())) {
                    subjectSelectForBlueprint.getItems().add(s.getSubjectCode());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- Subject Management ---
    @FXML
    private void handleAddSubject() {
        String subjectCode = subjectCodeField.getText();
        String subjectName = subjectNameField.getText();
        if (selectedCourseId == null || subjectCode.isEmpty() || subjectName.isEmpty()) return;
        try {
            DatabaseUtil.addSubject(selectedCourseId, subjectCode, subjectName);
            loadSubjects();
            subjectCodeField.clear();
            subjectNameField.clear();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to add subject. It may already exist.");
        }
    }
    private void loadSubjects() {
        if (selectedCourseId == null) return;
        try {
            allSubjects.clear();
            allSubjects.addAll(DatabaseUtil.getSubjectsByCourse(selectedCourseId));
            subjectsTable.setItems(allSubjects);
            
            subjectSelectForUnit.getItems().clear();
            for (Subject s : allSubjects) {
                subjectSelectForUnit.getItems().add(s.getSubjectCode());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- Unit Management ---
    @FXML
    private void handleAddUnit() {
        String unitName = unitNameField.getText();
        if (selectedSubjectIdForUnit == null || unitName.isEmpty()) return;
        try {
            DatabaseUtil.addUnit(selectedSubjectIdForUnit, unitName);
            loadUnits();
            unitNameField.clear();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to add unit. It may already exist.");
        }
    }
    private void loadUnits() {
        if (selectedSubjectIdForUnit == null) return;
        try {
            allUnits.clear();
            allUnits.addAll(DatabaseUtil.getUnitsBySubject(selectedSubjectIdForUnit));
            unitsTable.setItems(allUnits);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // --- Blueprint Management ---
    @FXML
    private void handleAddBlueprint() {
        if (selectedSubjectIdForBlueprint == null || selectedExamTypeId == null || blueprintTitleField.getText().isEmpty() || totalMarksField.getText().isEmpty() || durationField.getText().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Please fill in all fields.");
            return;
        }
        try {
            int totalMarks = Integer.parseInt(totalMarksField.getText());
            int duration = Integer.parseInt(durationField.getText());
            DatabaseUtil.addBlueprint(selectedSubjectIdForBlueprint, selectedExamTypeId, blueprintTitleField.getText(), totalMarks, duration);
            loadBlueprints();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Marks and Duration must be numbers.");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to add blueprint.");
        }
    }
    private void loadBlueprints() {
        if (selectedSubjectIdForBlueprint == null) return;
        try {
            allBlueprints.clear();
            allBlueprints.addAll(DatabaseUtil.getBlueprintsBySubject(selectedSubjectIdForBlueprint));
            blueprintsTable.setItems(allBlueprints);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void loadExamTypes() {
        try {
            allExamTypes.clear();
            allExamTypes.addAll(DatabaseUtil.getExamTypes());
            examTypeSelectForBlueprint.getItems().clear();
            for (ExamType et : allExamTypes) {
                examTypeSelectForBlueprint.getItems().add(et.getTypeName());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}