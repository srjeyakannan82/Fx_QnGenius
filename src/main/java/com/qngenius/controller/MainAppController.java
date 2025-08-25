package com.qngenius.controller;

import com.qngenius.model.Question;
import com.qngenius.util.DatabaseUtil;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MainAppController {

    @FXML
    private Label usernameLabel;

    @FXML
    private ComboBox<String> questionTypeComboBox;

    @FXML
    private ComboBox<Integer> marksComboBox;

    @FXML
    private ComboBox<String> difficultyLevelComboBox;

    @FXML
    private ComboBox<String> bloomTaxonomyComboBox;

    @FXML
    private TextArea questionsArea;

    // This method will be called by LoginController to pass the username
    public void setUsername(String username) {
        if (username != null && !username.trim().isEmpty()) {
            usernameLabel.setText("Welcome, " + username + "!");
        } else {
            usernameLabel.setText("Welcome!");
        }
    }

    @FXML
    public void initialize() {
        // Populate the ComboBoxes with predefined data
        questionTypeComboBox.setItems(FXCollections.observableArrayList(
                "Short Answer", "Case Study", "Long Answer", "Multiple Choice"));

        marksComboBox.setItems(FXCollections.observableArrayList(5, 10, 15, 20));

        difficultyLevelComboBox.setItems(FXCollections.observableArrayList(
                "Easy", "Medium", "Hard"));

        bloomTaxonomyComboBox.setItems(FXCollections.observableArrayList(
                "Remember", "Understand", "Apply", "Analyze", "Evaluate", "Create"));
    }

    @FXML
    private void handleGenerateButtonAction(ActionEvent event) {
        String questionType = questionTypeComboBox.getValue();
        Integer marks = marksComboBox.getValue();
        String difficultyLevel = difficultyLevelComboBox.getValue();
        String bloomTaxonomy = bloomTaxonomyComboBox.getValue();

        if (questionType == null || marks == null || difficultyLevel == null || bloomTaxonomy == null) {
            questionsArea.setText("Please select values for all fields.");
            return;
        }

        try {
            List<Question> newQuestions = generateDummyQuestions(questionType, marks, difficultyLevel, bloomTaxonomy);
            DatabaseUtil.saveQuestions(newQuestions);

            List<Question> retrievedQuestions = DatabaseUtil.getQuestions(questionType, difficultyLevel);
            displayQuestions(retrievedQuestions);

        } catch (SQLException e) {
            e.printStackTrace();
            questionsArea.setText(
                    "An error occurred while generating or saving questions. Please check the database connection and permissions.");
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

    private List<Question> generateDummyQuestions(String questionType, int marks, String difficultyLevel,
            String bloomTaxonomy) {
        List<Question> questions = new ArrayList<>();

        // Pass a 'null' for the 'createdBy' UUID since this is dummy data
        questions.add(new Question("Explain the concept of SWOT analysis.", questionType, marks, difficultyLevel,
                bloomTaxonomy, null));
        questions.add(new Question("How can a business use PESTLE analysis?", questionType, marks, difficultyLevel,
                bloomTaxonomy, null));
        questions.add(new Question("Analyze the competitive forces in the smartphone industry.", questionType, marks,
                difficultyLevel, bloomTaxonomy, null));

        return questions;
    }

    // This method needs to be updated to match the new constructor.
    private void displayQuestions(List<Question> questions) {
        StringBuilder sb = new StringBuilder();
        if (questions.isEmpty()) {
            sb.append("No questions found for the given criteria.");
        } else {
            for (Question q : questions) {
                sb.append("Question ID: ").append(q.getQuestionId()).append("\n");
                sb.append("Question: ").append(q.getQuestionText()).append("\n");
                sb.append("Type: ").append(q.getQuestionType()).append("\n");
                sb.append("Marks: ").append(q.getMarks()).append("\n");
                sb.append("Difficulty: ").append(q.getDifficultyLevel()).append("\n");
                sb.append("Bloom Level: ").append(q.getBloomTaxonomyLevel()).append("\n\n");
            }
        }
        questionsArea.setText(sb.toString());
    }
}