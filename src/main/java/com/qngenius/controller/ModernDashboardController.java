package com.qngenius.controller;

import com.qngenius.model.User;
import com.qngenius.service.QuestionService;
import com.qngenius.util.DatabaseUtil;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ModernDashboardController {
    
    private static final Logger LOGGER = Logger.getLogger(ModernDashboardController.class.getName());
    
    // User Info
    @FXML private Label welcomeLabel;
    @FXML private Label lastLoginLabel;
    @FXML private Label userRoleLabel;
    
    // Quick Stats Cards
    @FXML private Label totalQuestionsLabel;
    @FXML private Label totalSubjectsLabel;
    @FXML private Label totalBlueprintsLabel;
    @FXML private Label recentActivityLabel;
    
    // Charts
    @FXML private PieChart questionTypeChart;
    @FXML private BarChart<String, Number> difficultyChart;
    @FXML private LineChart<String, Number> activityChart;
    @FXML private AreaChart<String, Number> subjectDistributionChart;
    
    // Recent Activity
    @FXML private TableView<ActivityItem> recentActivityTable;
    @FXML private TableColumn<ActivityItem, String> activityTimeColumn;
    @FXML private TableColumn<ActivityItem, String> activityTypeColumn;
    @FXML private TableColumn<ActivityItem, String> activityDescriptionColumn;
    
    // Quick Actions
    @FXML private VBox quickActionsPane;
    @FXML private GridPane statsGrid;
    
    // Progress indicators
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label loadingStatusLabel;
    
    private User currentUser;
    private QuestionService questionService = QuestionService.getInstance();
    private Timer refreshTimer;
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        updateUserInfo();
        loadDashboardData();
        setupAutoRefresh();
    }
    
    @FXML
    public void initialize() {
        setupCharts();
        setupActivityTable();
        setupQuickActions();
        
        // Initially hide loading indicator
        loadingIndicator.setVisible(false);
        loadingStatusLabel.setVisible(false);
    }
    
    private void setupCharts() {
        // Configure Question Type Pie Chart
        questionTypeChart.setTitle("Question Distribution by Type");
        questionTypeChart.setLegendVisible(true);
        questionTypeChart.setLabelsVisible(true);
        
        // Configure Difficulty Bar Chart
        difficultyChart.setTitle("Questions by Difficulty Level");
        difficultyChart.setCategoryGap(10);
        difficultyChart.setBarGap(5);
        
        CategoryAxis difficultyXAxis = (CategoryAxis) difficultyChart.getXAxis();
        difficultyXAxis.setLabel("Difficulty Level");
        
        NumberAxis difficultyYAxis = (NumberAxis) difficultyChart.getYAxis();
        difficultyYAxis.setLabel("Number of Questions");
        
        // Configure Activity Line Chart
        activityChart.setTitle("Question Creation Activity (Last 7 Days)");
        activityChart.setCreateSymbols(true);
        activityChart.setLegendVisible(false);
        
        CategoryAxis activityXAxis = (CategoryAxis) activityChart.getXAxis();
        activityXAxis.setLabel("Date");
        
        NumberAxis activityYAxis = (NumberAxis) activityChart.getYAxis();
        activityYAxis.setLabel("Questions Created");
        
        // Configure Subject Distribution Area Chart
        subjectDistributionChart.setTitle("Questions by Subject");
        subjectDistributionChart.setCreateSymbols(false);
        subjectDistributionChart.setLegendVisible(true);
    }
    
    private void setupActivityTable() {
        activityTimeColumn.setCellValueFactory(data -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> data.getValue().getTimestamp().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"))
            )
        );
        
        activityTypeColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getType())
        );
        
        activityDescriptionColumn.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getDescription())
        );
        
        // Custom cell factory for activity type with icons/colors
        activityTypeColumn.setCellFactory(column -> new TableCell<ActivityItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item.toLowerCase()) {
                        case "question created":
                            setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                            break;
                        case "blueprint created":
                            setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
                            break;
                        case "paper generated":
                            setStyle("-fx-text-fill: purple; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("-fx-text-fill: black;");
                    }
                }
            }
        });
    }
    
    private void setupQuickActions() {
        if (quickActionsPane == null) return;
        
        quickActionsPane.getChildren().clear();
        
        if (currentUser != null) {
            // Add role-specific quick actions
            if (currentUser.hasPermission("CREATE_QUESTIONS")) {
                Button createQuestionBtn = new Button("Create Question");
                createQuestionBtn.getStyleClass().add("quick-action-button");
                createQuestionBtn.setOnAction(e -> navigateToQuestionCreation());
                quickActionsPane.getChildren().add(createQuestionBtn);
            }
            
            if (currentUser.hasPermission("GENERATE_PAPERS")) {
                Button generatePaperBtn = new Button("Generate Paper");
                generatePaperBtn.getStyleClass().add("quick-action-button");
                generatePaperBtn.setOnAction(e -> navigateToPaperGeneration());
                quickActionsPane.getChildren().add(generatePaperBtn);
            }
            
            if (currentUser.hasPermission("MANAGE_BLUEPRINTS")) {
                Button manageBlueprintsBtn = new Button("Manage Blueprints");
                manageBlueprintsBtn.getStyleClass().add("quick-action-button");
                manageBlueprintsBtn.setOnAction(e -> navigateToBlueprints());
                quickActionsPane.getChildren().add(manageBlueprintsBtn);
            }
            
            // Import questions action
            Button importQuestionsBtn = new Button("Import Questions");
            importQuestionsBtn.getStyleClass().add("quick-action-button");
            importQuestionsBtn.setOnAction(e -> navigateToImport());
            quickActionsPane.getChildren().add(importQuestionsBtn);
        }
    }
    
    private void updateUserInfo() {
        if (currentUser == null) return;
        
        Platform.runLater(() -> {
            welcomeLabel.setText("Welcome back, " + currentUser.getDisplayName());
            userRoleLabel.setText("Role: " + currentUser.getRoleDisplayName());
            
            if (currentUser.getLastLogin() != null) {
                lastLoginLabel.setText("Last login: " + 
                    currentUser.getLastLogin().format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm")));
            } else {
                lastLoginLabel.setText("First time login");
            }
        });
    }
    
    private void loadDashboardData() {
        showLoading("Loading dashboard data...");
        
        Task<DashboardData> loadTask = new Task<DashboardData>() {
            @Override
            protected DashboardData call() throws Exception {
                DashboardData data = new DashboardData();
                
                updateMessage("Loading question statistics...");
                data.questionStats = loadQuestionStatistics();
                
                updateMessage("Loading subject data...");
                data.subjectCount = DatabaseUtil.getAllCourses().stream()
                    .mapToInt(course -> {
                        try {
                            return DatabaseUtil.getSubjectsByCourse(course.getId()).size();
                        } catch (SQLException e) {
                            return 0;
                        }
                    }).sum();
                
                updateMessage("Loading recent activity...");
                data.recentActivity = loadRecentActivity();
                
                updateMessage("Preparing charts...");
                data.chartData = prepareChartData(data.questionStats);
                
                return data;
            }
            
            @Override
            protected void succeeded() {
                DashboardData data = getValue();
                updateDashboardUI(data);
                hideLoading();
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                LOGGER.log(Level.SEVERE, "Failed to load dashboard data", exception);
                hideLoading();
                showErrorMessage("Failed to load dashboard data: " + exception.getMessage());
            }
        };
        
        loadingStatusLabel.textProperty().bind(loadTask.messageProperty());
        new Thread(loadTask).start();
    }
    
    private QuestionService.QuestionStatistics loadQuestionStatistics() throws Exception {
        // This would need to be implemented to get statistics across all subjects
        // For now, return a mock statistics object
        Map<String, Integer> typeStats = new HashMap<>();
        typeStats.put("Short Answer", 45);
        typeStats.put("Long Answer", 30);
        typeStats.put("Multiple Choice", 25);
        typeStats.put("Case Study", 15);
        
        Map<String, Integer> difficultyStats = new HashMap<>();
        difficultyStats.put("Easy", 40);
        difficultyStats.put("Medium", 50);
        difficultyStats.put("Hard", 25);
        
        Map<String, Integer> bloomStats = new HashMap<>();
        bloomStats.put("Remember", 20);
        bloomStats.put("Understand", 35);
        bloomStats.put("Apply", 30);
        bloomStats.put("Analyze", 15);
        bloomStats.put("Evaluate", 10);
        bloomStats.put("Create", 5);
        
        return new QuestionService.QuestionStatistics(115, 1150, typeStats, difficultyStats, bloomStats);
    }
    
    private List<ActivityItem> loadRecentActivity() {
        List<ActivityItem> activities = new ArrayList<>();
        
        // Mock recent activity data - in real implementation, query from database
        activities.add(new ActivityItem(
            LocalDateTime.now().minusHours(2),
            "Question Created",
            "Created 5 new questions for Computer Networks"
        ));
        
        activities.add(new ActivityItem(
            LocalDateTime.now().minusHours(4),
            "Blueprint Created",
            "Created blueprint for Internal Exam 1 - Database Systems"
        ));
        
        activities.add(new ActivityItem(
            LocalDateTime.now().minusDays(1),
            "Paper Generated",
            "Generated question paper for Software Engineering"
        ));
        
        activities.add(new ActivityItem(
            LocalDateTime.now().minusDays(1).minusHours(3),
            "Questions Imported",
            "Imported 25 questions from Excel file"
        ));
        
        activities.add(new ActivityItem(
            LocalDateTime.now().minusDays(2),
            "Subject Added",
            "Added new subject: Data Mining"
        ));
        
        return activities;
    }
    
    private ChartData prepareChartData(QuestionService.QuestionStatistics stats) {
        ChartData chartData = new ChartData();
        
        // Prepare pie chart data
        chartData.questionTypeData = FXCollections.observableArrayList();
        stats.getQuestionTypeDistribution().forEach((type, count) -> 
            chartData.questionTypeData.add(new PieChart.Data(type, count))
        );
        
        // Prepare bar chart data
        chartData.difficultySeriesData = new XYChart.Series<>();
        chartData.difficultySeriesData.setName("Questions by Difficulty");
        stats.getDifficultyDistribution().forEach((difficulty, count) ->
            chartData.difficultySeriesData.getData().add(new XYChart.Data<>(difficulty, count))
        );
        
        // Prepare activity chart data (mock data for last 7 days)
        chartData.activitySeriesData = new XYChart.Series<>();
        chartData.activitySeriesData.setName("Questions Created");
        
        LocalDateTime now = LocalDateTime.now();
        for (int i = 6; i >= 0; i--) {
            LocalDateTime date = now.minusDays(i);
            String dateStr = date.format(DateTimeFormatter.ofPattern("MM/dd"));
            int count = (int) (Math.random() * 10) + 1; // Mock data
            chartData.activitySeriesData.getData().add(new XYChart.Data<>(dateStr, count));
        }
        
        return chartData;
    }
    
    private void updateDashboardUI(DashboardData data) {
        Platform.runLater(() -> {
            // Update stat cards
            totalQuestionsLabel.setText(String.valueOf(data.questionStats.getTotalQuestions()));
            totalSubjectsLabel.setText(String.valueOf(data.subjectCount));
            totalBlueprintsLabel.setText("12"); // Mock data
            recentActivityLabel.setText(String.valueOf(data.recentActivity.size()));
            
            // Update charts
            questionTypeChart.setData(data.chartData.questionTypeData);
            
            difficultyChart.getData().clear();
            difficultyChart.getData().add(data.chartData.difficultySeriesData);
            
            activityChart.getData().clear();
            activityChart.getData().add(data.chartData.activitySeriesData);
            
            // Update activity table
            recentActivityTable.setItems(FXCollections.observableArrayList(data.recentActivity));
            
            // Update stats cards with animations
            animateStatCards();
        });
    }
    
    private void animateStatCards() {
        // Simple animation for stat cards - you can enhance this with JavaFX animations
        totalQuestionsLabel.setStyle("-fx-background-color: rgba(76, 175, 80, 0.1); -fx-background-radius: 5;");
        totalSubjectsLabel.setStyle("-fx-background-color: rgba(33, 150, 243, 0.1); -fx-background-radius: 5;");
        totalBlueprintsLabel.setStyle("-fx-background-color: rgba(156, 39, 176, 0.1); -fx-background-radius: 5;");
        recentActivityLabel.setStyle("-fx-background-color: rgba(255, 152, 0, 0.1); -fx-background-radius: 5;");
    }
    
    private void setupAutoRefresh() {
        // Refresh dashboard every 5 minutes
        refreshTimer = new Timer(true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (Platform.isFxApplicationThread()) {
                    loadDashboardData();
                } else {
                    Platform.runLater(() -> loadDashboardData());
                }
            }
        }, 300000, 300000); // 5 minutes
    }
    
    @FXML
    private void refreshDashboard() {
        loadDashboardData();
    }
    
    private void showLoading(String message) {
        Platform.runLater(() -> {
            loadingIndicator.setVisible(true);
            loadingStatusLabel.setVisible(true);
            loadingStatusLabel.setText(message);
        });
    }
    
    private void hideLoading() {
        Platform.runLater(() -> {
            loadingIndicator.setVisible(false);
            loadingStatusLabel.setVisible(false);
        });
    }
    
    private void showErrorMessage(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Dashboard Error");
            alert.setHeaderText("Failed to load dashboard");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    // Navigation methods (these would need to be connected to your existing controllers)
    private void navigateToQuestionCreation() {
        // Implementation would load the question creation view
        LOGGER.info("Navigating to question creation");
    }
    
    private void navigateToPaperGeneration() {
        // Implementation would load the paper generation view
        LOGGER.info("Navigating to paper generation");
    }
    
    private void navigateToBlueprints() {
        // Implementation would load the blueprint management view
        LOGGER.info("Navigating to blueprint management");
    }
    
    private void navigateToImport() {
        // Implementation would load the bulk import view
        LOGGER.info("Navigating to bulk import");
    }
    
    public void shutdown() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }
    }
    
    // Data classes
    private static class DashboardData {
        QuestionService.QuestionStatistics questionStats;
        int subjectCount;
        int blueprintCount;
        List<ActivityItem> recentActivity;
        ChartData chartData;
    }
    
    private static class ChartData {
        javafx.collections.ObservableList<PieChart.Data> questionTypeData;
        XYChart.Series<String, Number> difficultySeriesData;
        XYChart.Series<String, Number> activitySeriesData;
        XYChart.Series<String, Number> subjectSeriesData;
    }
    
    public static class ActivityItem {
        private final LocalDateTime timestamp;
        private final String type;
        private final String description;
        
        public ActivityItem(LocalDateTime timestamp, String type, String description) {
            this.timestamp = timestamp;
            this.type = type;
            this.description = description;
        }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getType() { return type; }
        public String getDescription() { return description; }
    }
}