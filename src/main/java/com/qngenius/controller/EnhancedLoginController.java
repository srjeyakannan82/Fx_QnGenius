package com.qngenius.controller;

import com.qngenius.util.DatabaseUtil;
import com.qngenius.model.User;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class EnhancedLoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    @FXML
    private void handleLoginButtonAction(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Login Error", "Please enter both username and password.");
            return;
        }

        try {
            User user = authenticateUser(username, password);
            if (user != null) {
                navigateBasedOnRole(event, user);
            } else {
                showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid username or password.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Unable to connect to database. Please try again later.");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "System Error", "Unable to load application. Please contact support.");
        }
    }

    private User authenticateUser(String username, String password) throws SQLException {
        String sql = "SELECT id, username, password_hash, role FROM users WHERE username = ?";
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String storedPasswordHash = rs.getString("password_hash");
                if (passwordEncoder.matches(password, storedPasswordHash)) {
                    return new User(
                        rs.getObject("id", UUID.class),
                        rs.getString("username"),
                        rs.getString("role")
                    );
                }
            }
        }
        return null;
    }

    private void navigateBasedOnRole(ActionEvent event, User user) throws IOException {
        String fxmlPath;
        String windowTitle = "QnGenius - ";
        
        switch (user.getRole().toLowerCase()) {
            case "admin":
                fxmlPath = "/com/qngenius/view/admin.fxml";
                windowTitle += "Admin Dashboard";
                break;
            case "coe":
            case "controller":
                fxmlPath = "/com/qngenius/view/coe.fxml";
                windowTitle += "Controller of Examinations";
                break;
            case "teacher":
            case "faculty":
                fxmlPath = "/com/qngenius/view/mainapp.fxml";
                windowTitle += "Teacher Dashboard";
                break;
            case "student":
                fxmlPath = "/com/qngenius/view/student.fxml"; // Create this view
                windowTitle += "Student Portal";
                break;
            default:
                showAlert(Alert.AlertType.ERROR, "Access Error", "Your account role is not recognized. Please contact support.");
                return;
        }

        loadUserDashboard(event, fxmlPath, windowTitle, user);
    }

    private void loadUserDashboard(ActionEvent event, String fxmlPath, String windowTitle, User user) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent dashboardPage = loader.load();

        // Pass user information to the controller
        Object controller = loader.getController();
        if (controller instanceof UserAwareController) {
            ((UserAwareController) controller).setCurrentUser(user);
        }

        Scene dashboardScene = new Scene(dashboardPage);
        String cssPath = getClass().getResource("/css/style.css").toExternalForm();
        dashboardScene.getStylesheets().add(cssPath);

        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setTitle(windowTitle);
        window.setScene(dashboardScene);
        window.setMaximized(true);
        window.show();
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Interface for controllers that need user information
    public interface UserAwareController {
        void setCurrentUser(User user);
    }
}