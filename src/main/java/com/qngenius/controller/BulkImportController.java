package com.qngenius.controller;

import com.qngenius.model.Question;
import com.qngenius.model.Subject;
import com.qngenius.model.Unit;
import com.qngenius.service.QuestionService;
import com.qngenius.util.DatabaseUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

public class BulkImportController {
    
    private static final Logger LOGGER = Logger.getLogger(BulkImportController.class.getName());
    
    @FXML private ComboBox<String> subjectComboBox;
    @FXML private ComboBox<String> unitComboBox;
    @FXML private Button selectFileButton;
    @FXML private Label selectedFileLabel;
    @FXML private TableView<QuestionImportRow> previewTable;
    @FXML private TableColumn<QuestionImportRow, String> questionTextColumn;
    @FXML private TableColumn<QuestionImportRow, String> questionTypeColumn;
    @FXML private TableColumn<QuestionImportRow, Integer> marksColumn;
    @FXML private TableColumn<QuestionImportRow, String> difficultyColumn;
    @FXML private TableColumn<QuestionImportRow, String> statusColumn;
    @FXML private Button importButton;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;
    @FXML private TextArea logTextArea;
    @FXML private CheckBox validateDuplicatesCheckbox;
    @FXML private CheckBox skipInvalidCheckbox;
    
    private File selectedFile;
    private UUID currentUserId;
    private UUID selectedSubjectId;
    private UUID selectedUnitId;
    private List<QuestionImportRow> importRows = new ArrayList<>();
    private QuestionService questionService = QuestionService.getInstance();
    
    public void setCurrentUser(UUID userId) {
        this.currentUserId = userId;
    }
    
    @FXML
    public void initialize() {
        initializeTableColumns();
        loadSubjects();
        setupEventHandlers();
        
        // Initial state
        importButton.setDisable(true);
        progressBar.setVisible(false);
        statusLabel.setText("Ready to import questions");
        
        // Set default options
        validateDuplicatesCheckbox.setSelected(true);
        skipInvalidCheckbox.setSelected(true);
    }
    
    private void initializeTableColumns() {
        questionTextColumn.setCellValueFactory(data -> data.getValue().questionTextProperty());
        questionTypeColumn.setCellValueFactory(data -> data.getValue().questionTypeProperty());
        marksColumn.setCellValueFactory(data -> data.getValue().marksProperty().asObject());
        difficultyColumn.setCellValueFactory(data -> data.getValue().difficultyProperty());
        statusColumn.setCellValueFactory(data -> data.getValue().statusProperty());
        
        // Add custom cell factory for status column to show colors
        statusColumn.setCellFactory(column -> new TableCell<QuestionImportRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item.toLowerCase()) {
                        case "valid":
                            setStyle("-fx-background-color: lightgreen;");
                            break;
                        case "invalid":
                        case "duplicate":
                            setStyle("-fx-background-color: lightcoral;");
                            break;
                        case "warning":
                            setStyle("-fx-background-color: lightyellow;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
    }
    
    private void setupEventHandlers() {
        subjectComboBox.setOnAction(e -> {
            String selectedSubjectCode = subjectComboBox.getValue();
            if (selectedSubjectCode != null) {
                loadUnitsForSubject(selectedSubjectCode);
            }
        });
        
        unitComboBox.setOnAction(e -> {
            validateImportReadiness();
        });
    }
    
    private void loadSubjects() {
        try {
            List<Subject> allSubjects = new ArrayList<>();
            DatabaseUtil.getAllCourses().forEach(course -> {
                try {
                    allSubjects.addAll(DatabaseUtil.getSubjectsByCourse(course.getId()));
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Failed to load subjects for course: " + course.getCourseCode(), e);
                }
            });
            
            subjectComboBox.setItems(FXCollections.observableArrayList(
                allSubjects.stream().map(Subject::getSubjectCode).toArray(String[]::new)
            ));
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load subjects", e);
            showError("Failed to load subjects: " + e.getMessage());
        }
    }
    
    private void loadUnitsForSubject(String subjectCode) {
        try {
            // Find subject ID by code
            List<Subject> allSubjects = new ArrayList<>();
            DatabaseUtil.getAllCourses().forEach(course -> {
                try {
                    allSubjects.addAll(DatabaseUtil.getSubjectsByCourse(course.getId()));
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error loading subjects", e);
                }
            });
            
            Optional<Subject> selectedSubject = allSubjects.stream()
                .filter(s -> s.getSubjectCode().equals(subjectCode))
                .findFirst();
                
            if (selectedSubject.isPresent()) {
                selectedSubjectId = selectedSubject.get().getId();
                List<Unit> units = DatabaseUtil.getUnitsBySubject(selectedSubjectId);
                
                unitComboBox.setItems(FXCollections.observableArrayList(
                    units.stream().map(Unit::getUnitName).toArray(String[]::new)
                ));
                
                if (!units.isEmpty()) {
                    selectedUnitId = units.get(0).getId();
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load units", e);
            showError("Failed to load units: " + e.getMessage());
        }
    }
    
    @FXML
    private void selectFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Excel File with Questions");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        File file = fileChooser.showOpenDialog(selectFileButton.getScene().getWindow());
        if (file != null) {
            selectedFile = file;
            selectedFileLabel.setText("Selected: " + file.getName());
            
            // Load and preview the file
            loadFilePreview();
        }
    }
    
    private void loadFilePreview() {
        Task<List<QuestionImportRow>> loadTask = new Task<List<QuestionImportRow>>() {
            @Override
            protected List<QuestionImportRow> call() throws Exception {
                updateMessage("Loading file preview...");
                return parseExcelFile(selectedFile);
            }
            
            @Override
            protected void succeeded() {
                importRows = getValue();
                previewTable.setItems(FXCollections.observableArrayList(importRows));
                statusLabel.setText("Loaded " + importRows.size() + " questions from file");
                validateImportReadiness();
                
                // Show validation summary
                long validCount = importRows.stream().mapToLong(row -> "valid".equals(row.getStatus()) ? 1 : 0).sum();
                long invalidCount = importRows.size() - validCount;
                
                appendLog(String.format("File loaded: %d total questions, %d valid, %d invalid/warnings", 
                         importRows.size(), validCount, invalidCount));
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                LOGGER.log(Level.SEVERE, "Failed to load file preview", exception);
                showError("Failed to load file: " + exception.getMessage());
                statusLabel.setText("Failed to load file");
            }
        };
        
        statusLabel.textProperty().bind(loadTask.messageProperty());
        new Thread(loadTask).start();
    }
    
    private List<QuestionImportRow> parseExcelFile(File file) throws IOException {
        List<QuestionImportRow> rows = new ArrayList<>();
        
        try (FileInputStream fis = new FileInputStream(file)) {
            Workbook workbook;
            
            if (file.getName().toLowerCase().endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(fis);
            } else {
                workbook = new HSSFWorkbook(fis);
            }
            
            Sheet sheet = workbook.getSheetAt(0);
            boolean isFirstRow = true;
            
            for (Row row : sheet) {
                if (isFirstRow) {
                    isFirstRow = false;
                    continue; // Skip header row
                }
                
                if (isEmptyRow(row)) {
                    continue;
                }
                
                try {
                    QuestionImportRow importRow = parseRowToQuestion(row);
                    if (validateDuplicatesCheckbox.isSelected()) {
                        checkForDuplicate(importRow);
                    }
                    rows.add(importRow);
                } catch (Exception e) {
                    // Create an invalid row to show the error
                    QuestionImportRow errorRow = new QuestionImportRow();
                    errorRow.setQuestionText("Error parsing row " + (row.getRowNum() + 1));
                    errorRow.setStatus("Invalid: " + e.getMessage());
                    rows.add(errorRow);
                }
            }
            
            workbook.close();
        }
        
        return rows;
    }
    
    private boolean isEmptyRow(Row row) {
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellValueAsString(cell);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private QuestionImportRow parseRowToQuestion(Row row) {
        QuestionImportRow importRow = new QuestionImportRow();
        
        // Expected columns: Question Text, Type, Marks, Difficulty, Bloom Level, Keywords
        String questionText = getCellValueAsString(row.getCell(0));
        String questionType = getCellValueAsString(row.getCell(1));
        String marksStr = getCellValueAsString(row.getCell(2));
        String difficulty = getCellValueAsString(row.getCell(3));
        String bloomLevel = getCellValueAsString(row.getCell(4));
        String keywords = getCellValueAsString(row.getCell(5));
        
        importRow.setQuestionText(questionText);
        importRow.setQuestionType(questionType);
        importRow.setDifficulty(difficulty);
        importRow.setBloomLevel(bloomLevel);
        importRow.setKeywords(keywords);
        
        // Parse marks
        try {
            if (marksStr != null && !marksStr.trim().isEmpty()) {
                importRow.setMarks(Integer.parseInt(marksStr.trim()));
            } else {
                importRow.setMarks(0);
            }
        } catch (NumberFormatException e) {
            importRow.setMarks(0);
            importRow.setStatus("Warning: Invalid marks value");
        }
        
        // Validate the row
        validateImportRow(importRow);
        
        return importRow;
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }
    
    private void validateImportRow(QuestionImportRow row) {
        List<String> errors = new ArrayList<>();
        
        // Required field validation
        if (row.getQuestionText() == null || row.getQuestionText().trim().isEmpty()) {
            errors.add("Question text is required");
        } else if (row.getQuestionText().length() < 10) {
            errors.add("Question text too short");
        }
        
        if (row.getQuestionType() == null || row.getQuestionType().trim().isEmpty()) {
            errors.add("Question type is required");
        } else if (!isValidQuestionType(row.getQuestionType())) {
            errors.add("Invalid question type");
        }
        
        if (row.getMarks() <= 0) {
            errors.add("Marks must be greater than 0");
        } else if (row.getMarks() > 100) {
            errors.add("Marks seem too high (>100)");
        }
        
        if (row.getDifficulty() == null || row.getDifficulty().trim().isEmpty()) {
            errors.add("Difficulty level is required");
        } else if (!isValidDifficultyLevel(row.getDifficulty())) {
            errors.add("Invalid difficulty level");
        }
        
        if (row.getBloomLevel() != null && !row.getBloomLevel().trim().isEmpty() 
            && !isValidBloomLevel(row.getBloomLevel())) {
            errors.add("Invalid Bloom taxonomy level");
        }
        
        // Set status based on validation
        if (errors.isEmpty()) {
            row.setStatus("Valid");
        } else {
            row.setStatus("Invalid: " + String.join(", ", errors));
        }
    }
    
    private void checkForDuplicate(QuestionImportRow row) {
        try {
            if (questionService.isDuplicateQuestion(row.toQuestion(selectedUnitId, currentUserId), selectedSubjectId)) {
                row.setStatus("Duplicate");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to check for duplicate question", e);
            // Don't fail the import for this
        }
    }
    
    private boolean isValidQuestionType(String type) {
        return Arrays.asList("Short Answer", "Long Answer", "Multiple Choice", "Case Study", "Essay", "Numerical").contains(type);
    }
    
    private boolean isValidDifficultyLevel(String level) {
        return Arrays.asList("Easy", "Medium", "Hard").contains(level);
    }
    
    private boolean isValidBloomLevel(String level) {
        return Arrays.asList("Remember", "Understand", "Apply", "Analyze", "Evaluate", "Create").contains(level);
    }
    
    private void validateImportReadiness() {
        boolean canImport = selectedFile != null 
                         && selectedSubjectId != null 
                         && selectedUnitId != null 
                         && !importRows.isEmpty();
        
        importButton.setDisable(!canImport);
    }
    
    @FXML
    private void startImport() {
        if (importRows.isEmpty()) {
            showWarning("No questions to import");
            return;
        }
        
        // Filter questions based on settings
        List<QuestionImportRow> questionsToImport = importRows.stream()
            .filter(row -> {
                if ("valid".equalsIgnoreCase(row.getStatus())) {
                    return true;
                }
                if (skipInvalidCheckbox.isSelected()) {
                    return false;
                }
                return !"duplicate".equalsIgnoreCase(row.getStatus());
            })
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        
        if (questionsToImport.isEmpty()) {
            showWarning("No valid questions to import after filtering");
            return;
        }
        
        // Confirm import
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Import");
        confirmation.setHeaderText("Ready to import questions");
        confirmation.setContentText(String.format("Import %d questions into %s?", 
                                   questionsToImport.size(), subjectComboBox.getValue()));
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            performImport(questionsToImport);
        }
    }
    
    private void performImport(List<QuestionImportRow> questionsToImport) {
        Task<Void> importTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Importing questions...");
                updateProgress(0, questionsToImport.size());
                
                List<Question> questions = new ArrayList<>();
                int processed = 0;
                
                for (QuestionImportRow row : questionsToImport) {
                    questions.add(row.toQuestion(selectedUnitId, currentUserId));
                    processed++;
                    updateProgress(processed, questionsToImport.size());
                    
                    // Batch insert every 50 questions
                    if (questions.size() >= 50) {
                        questionService.saveQuestions(questions, currentUserId);
                        questions.clear();
                        
                        Platform.runLater(() -> appendLog("Imported batch of 50 questions..."));
                    }
                }
                
                // Import remaining questions
                if (!questions.isEmpty()) {
                    questionService.saveQuestions(questions, currentUserId);
                }
                
                return null;
            }
            
            @Override
            protected void succeeded() {
                statusLabel.setText("Import completed successfully!");
                progressBar.setVisible(false);
                appendLog("Import completed: " + questionsToImport.size() + " questions imported");
                
                // Reset UI
                importButton.setDisable(true);
                previewTable.getItems().clear();
                selectedFileLabel.setText("No file selected");
                selectedFile = null;
                
                showInfo("Successfully imported " + questionsToImport.size() + " questions!");
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                LOGGER.log(Level.SEVERE, "Import failed", exception);
                statusLabel.setText("Import failed");
                progressBar.setVisible(false);
                showError("Import failed: " + exception.getMessage());
            }
        };
        
        statusLabel.textProperty().bind(importTask.messageProperty());
        progressBar.progressProperty().bind(importTask.progressProperty());
        progressBar.setVisible(true);
        importButton.setDisable(true);
        
        new Thread(importTask).start();
    }
    
    private void appendLog(String message) {
        Platform.runLater(() -> {
            logTextArea.appendText(new Date() + ": " + message + "\n");
            logTextArea.setScrollTop(Double.MAX_VALUE);
        });
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Inner class for table row representation
    public static class QuestionImportRow {
        private String questionText = "";
        private String questionType = "";
        private int marks = 0;
        private String difficulty = "";
        private String bloomLevel = "";
        private String keywords = "";
        private String status = "Valid";
        
        public Question toQuestion(UUID unitId, UUID createdBy) {
            return new Question(
                null, // ID will be generated
                unitId,
                null, // coId
                questionText,
                questionType,
                marks,
                difficulty,
                null, // importance level
                null, // application level
                bloomLevel,
                null, // course outcome
                keywords,
                createdBy,
                null // raw content
            );
        }
        
        // JavaFX Property methods
        public javafx.beans.property.StringProperty questionTextProperty() {
            return new javafx.beans.property.SimpleStringProperty(questionText);
        }
        
        public javafx.beans.property.StringProperty questionTypeProperty() {
            return new javafx.beans.property.SimpleStringProperty(questionType);
        }
        
        public javafx.beans.property.IntegerProperty marksProperty() {
            return new javafx.beans.property.SimpleIntegerProperty(marks);
        }
        
        public javafx.beans.property.StringProperty difficultyProperty() {
            return new javafx.beans.property.SimpleStringProperty(difficulty);
        }
        
        public javafx.beans.property.StringProperty statusProperty() {
            return new javafx.beans.property.SimpleStringProperty(status);
        }
        
        // Getters and setters
        public String getQuestionText() { return questionText; }
        public void setQuestionText(String questionText) { this.questionText = questionText != null ? questionText : ""; }
        
        public String getQuestionType() { return questionType; }
        public void setQuestionType(String questionType) { this.questionType = questionType != null ? questionType : ""; }
        
        public int getMarks() { return marks; }
        public void setMarks(int marks) { this.marks = marks; }
        
        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty != null ? difficulty : ""; }
        
        public String getBloomLevel() { return bloomLevel; }
        public void setBloomLevel(String bloomLevel) { this.bloomLevel = bloomLevel != null ? bloomLevel : ""; }
        
        public String getKeywords() { return keywords; }
        public void setKeywords(String keywords) { this.keywords = keywords != null ? keywords : ""; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status != null ? status : "Valid"; }
    }
}