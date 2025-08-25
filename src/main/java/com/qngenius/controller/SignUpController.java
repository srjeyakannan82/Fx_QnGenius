package com.qngenius.controller;

import com.qngenius.util.DatabaseUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // This is the new import

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SignUpController {

    @FXML
    private TextField usernameField;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;

    @FXML
private void handleSignUpButtonAction(ActionEvent event) throws IOException {
    String username = usernameField.getText();
    String email = emailField.getText();
    String password = passwordField.getText();

    if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
        showAlert(Alert.AlertType.ERROR, "Form Error!", "Please enter all fields.");
        return;
    }

    // Hash the password
    BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    String hashedPassword = passwordEncoder.encode(password);

    // Save user to the database
    String sql = "INSERT INTO users (username, password_hash, email) VALUES (?, ?, ?)";
    try (Connection conn = DatabaseUtil.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setString(1, username);
        pstmt.setString(2, hashedPassword);
        pstmt.setString(3, email);

        pstmt.executeUpdate();

        showAlert(Alert.AlertType.INFORMATION, "Success!", "Account created successfully!");
        navigateToLogin(event);

    } catch (SQLException e) {
        showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to create account. Username or email might already exist.");
        e.printStackTrace();
    }
}
    
    @FXML
    private void handleLoginButtonAction(ActionEvent event) throws IOException {
        navigateToLogin(event);
    }

    private void navigateToLogin(ActionEvent event) throws IOException {
        // Parent loginPage = FXMLLoader.load(getClass().getResource("/com/qngenius/view/login.fxml"));
Parent loginPage = FXMLLoader.load(getClass().getResource("/com/qngenius/view/login.fxml"));
        Scene loginScene = new Scene(loginPage);
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(loginScene);
        window.show();
    }


    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}