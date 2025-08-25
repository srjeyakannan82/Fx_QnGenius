package com.qngenius.controller;

import com.qngenius.model.Blueprint;
import com.qngenius.model.Course;
import com.qngenius.model.ExamType;
import com.qngenius.model.Subject;
import com.qngenius.model.BlueprintCriteria;
import com.qngenius.model.Question;
import com.qngenius.util.DatabaseUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;
import java.util.List;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.FileNotFoundException;

public class CoEController {

    @FXML
    private Label usernameLabel;
    @FXML
    private ComboBox<String> subjectComboBox;
    @FXML
    private ComboBox<String> examTypeComboBox;
    @FXML
    private ComboBox<String> blueprintComboBox;
    @FXML
    private TextArea paperTextArea;

    private ObservableList<Subject> allSubjects = FXCollections.observableArrayList();
    private ObservableList<ExamType> allExamTypes = FXCollections.observableArrayList();
    private ObservableList<Blueprint> allBlueprints = FXCollections.observableArrayList();

    private UUID selectedSubjectId;
    private UUID selectedExamTypeId;
    private UUID selectedBlueprintId;

    public void setUsername(String username) {
        usernameLabel.setText("Welcome, " + username + "!");
    }

    public void setUserId(UUID userId) {
        // You would use this to track the user who generated the paper.
    }

    @FXML
    public void initialize() {
        loadSubjects();
        loadExamTypes();

        subjectComboBox.setOnAction(event -> loadBlueprints());
        examTypeComboBox.setOnAction(event -> loadBlueprints());
        blueprintComboBox.setOnAction(event -> {
            String selectedBlueprintTitle = blueprintComboBox.getSelectionModel().getSelectedItem();
            if (selectedBlueprintTitle != null) {
                for (Blueprint bp : allBlueprints) {
                    if (bp.getTitle().equals(selectedBlueprintTitle)) {
                        selectedBlueprintId = bp.getId();
                        break;
                    }
                }
            }
        });
    }

    private void loadSubjects() {
        try {
            allSubjects.clear();
            List<Course> allCourses = DatabaseUtil.getAllCourses();
            for (Course course : allCourses) {
                allSubjects.addAll(DatabaseUtil.getSubjectsByCourse(course.getId()));
            }
            subjectComboBox.getItems().clear();
            for (Subject s : allSubjects) {
                subjectComboBox.getItems().add(s.getSubjectCode());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadExamTypes() {
        try {
            allExamTypes.clear();
            allExamTypes.addAll(DatabaseUtil.getExamTypes());
            examTypeComboBox.getItems().clear();
            for (ExamType et : allExamTypes) {
                examTypeComboBox.getItems().add(et.getTypeName());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadBlueprints() {
        String selectedSubjectCode = subjectComboBox.getSelectionModel().getSelectedItem();
        String selectedExamTypeName = examTypeComboBox.getSelectionModel().getSelectedItem();

        if (selectedSubjectCode == null || selectedExamTypeName == null) {
            return;
        }

        UUID subjectId = null;
        for (Subject s : allSubjects) {
            if (s.getSubjectCode().equals(selectedSubjectCode)) {
                subjectId = s.getId();
                break;
            }
        }

        UUID examTypeId = null;
        for (ExamType et : allExamTypes) {
            if (et.getTypeName().equals(selectedExamTypeName)) {
                examTypeId = et.getId();
                break;
            }
        }

        if (subjectId != null && examTypeId != null) {
            try {
                allBlueprints.clear();
                allBlueprints.addAll(DatabaseUtil.getBlueprintsBySubjectAndExamType(subjectId, examTypeId));
                blueprintComboBox.getItems().clear();
                for (Blueprint bp : allBlueprints) {
                    blueprintComboBox.getItems().add(bp.getTitle());
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleGenerateButtonAction(ActionEvent event) {
        if (selectedBlueprintId == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Error", "Please select a blueprint first.");
            return;
        }

        try {
            List<BlueprintCriteria> allCriteria = DatabaseUtil.getBlueprintCriteria(selectedBlueprintId);
            StringBuilder paperContent = new StringBuilder();
            int questionNumber = 1;

            for (BlueprintCriteria criteria : allCriteria) {
                UUID subjectId = null;
                String selectedSubjectCode = subjectComboBox.getSelectionModel().getSelectedItem();
                for (Subject s : allSubjects) {
                    if (s.getSubjectCode().equals(selectedSubjectCode)) {
                        subjectId = s.getId();
                        break;
                    }
                }

                if (subjectId != null) {
                    List<Question> questions = DatabaseUtil.getQuestionsByCriteria(subjectId, criteria);

                    for (Question q : questions) {
                        paperContent.append("Q").append(questionNumber++).append(". ").append(q.getQuestionText())
                                .append(" (Marks: ").append(q.getMarks()).append(")\n");
                    }
                }
            }

            paperTextArea.setText(paperContent.toString());
            showAlert(Alert.AlertType.INFORMATION, "Success", "Question paper generated successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Generation Failed", "An error occurred while generating the paper.");
        }
    }

    @FXML
    private void handleExportButtonAction(ActionEvent event) {
        String paperContent = paperTextArea.getText();
        if (paperContent.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Export Error", "No question paper to export.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Question Paper");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        // Show save dialog
        File file = fileChooser.showSaveDialog(paperTextArea.getScene().getWindow());

        if (file != null) {
            try {
                // Create PDF writer and document
                PdfWriter writer = new PdfWriter(file.getAbsolutePath());
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf);

                // Add content from the TextArea
                document.add(new Paragraph(paperContent));

                // Close the document
                document.close();

                showAlert(Alert.AlertType.INFORMATION, "Export Successful",
                        "Question paper exported to " + file.getAbsolutePath());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Export Failed", "Error saving the file.");
            }
        }
    }

    @FXML
    private void handleLogoutButtonAction(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/qngenius/view/login.fxml"));
        Parent loginPage = loader.load();
        Scene loginScene = new Scene(loginPage);
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(loginScene);
        window.setMaximized(true);
        window.show();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}