package com.qngenius.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.qngenius.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.logging.Level;

public class EnhancedDatabaseUtil {
    private static final Logger LOGGER = Logger.getLogger(EnhancedDatabaseUtil.class.getName());
    private static HikariDataSource dataSource;
    private static final ConfigManager config = ConfigManager.getInstance();
    
    static {
        initializeConnectionPool();
    }
    
    private static void initializeConnectionPool() {
        try {
            HikariConfig hikariConfig = new HikariConfig();
            
            // Database connection settings
            hikariConfig.setJdbcUrl(config.getDatabaseUrl());
            hikariConfig.setUsername(config.getDatabaseUser());
            hikariConfig.setPassword(config.getDatabasePassword());
            hikariConfig.setDriverClassName(config.getDatabaseDriver());
            
            // Pool settings
            hikariConfig.setInitialPoolSize(config.getConnectionPoolInitialSize());
            hikariConfig.setMaximumPoolSize(config.getConnectionPoolMaxSize());
            hikariConfig.setConnectionTimeout(config.getConnectionPoolMaxWaitTime());
            hikariConfig.setIdleTimeout(600000); // 10 minutes
            hikariConfig.setMaxLifetime(1800000); // 30 minutes
            hikariConfig.setLeakDetectionThreshold(60000); // 1 minute
            
            // Connection validation
            hikariConfig.setConnectionTestQuery("SELECT 1");
            hikariConfig.setValidationTimeout(5000);
            
            // Pool name for monitoring
            hikariConfig.setPoolName("QnGenius-DB-Pool");
            
            dataSource = new HikariDataSource(hikariConfig);
            LOGGER.info("Database connection pool initialized successfully");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize database connection pool", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }
    
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Database connection pool not initialized");
        }
        return dataSource.getConnection();
    }
    
    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOGGER.info("Database connection pool closed");
        }
    }
    
    // Enhanced Course Methods with better error handling
    public static void addCourse(String courseCode, String courseName) throws SQLException {
        String sql = "INSERT INTO courses (course_code, course_name) VALUES (?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, courseCode.trim().toUpperCase());
            pstmt.setString(2, courseName.trim());
            pstmt.executeUpdate();
            
            LOGGER.info("Course added successfully: " + courseCode);
            
        } catch (SQLException e) {
            if (e.getSQLState().equals("23505")) { // Unique violation
                throw new SQLException("Course with code '" + courseCode + "' already exists");
            }
            throw e;
        }
    }
    
    public static List<Course> getAllCourses() throws SQLException {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT id, course_code, course_name, created_at FROM courses ORDER BY course_name";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                courses.add(new Course(
                    rs.getObject("id", UUID.class),
                    rs.getString("course_code"),
                    rs.getString("course_name")
                ));
            }
            
            LOGGER.info("Retrieved " + courses.size() + " courses");
        }
        
        return courses;
    }
    
    public static boolean deleteCourse(UUID courseId) throws SQLException {
        // Check if course has subjects
        if (hasDependentSubjects(courseId)) {
            throw new SQLException("Cannot delete course: it has associated subjects");
        }
        
        String sql = "DELETE FROM courses WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setObject(1, courseId);
            int affected = pstmt.executeUpdate();
            
            LOGGER.info("Course deletion result: " + affected + " rows affected");
            return affected > 0;
        }
    }
    
    private static boolean hasDependentSubjects(UUID courseId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM subjects WHERE course_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setObject(1, courseId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
    
    // Enhanced Subject Methods
    public static void addSubject(UUID courseId, String subjectCode, String subjectName) throws SQLException {
        String sql = "INSERT INTO subjects (course_id, subject_code, subject_name) VALUES (?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setObject(1, courseId);
            pstmt.setString(2, subjectCode.trim().toUpperCase());
            pstmt.setString(3, subjectName.trim());
            pstmt.executeUpdate();
            
            LOGGER.info("Subject added successfully: " + subjectCode);
            
        } catch (SQLException e) {
            if (e.getSQLState().equals("23505")) {
                throw new SQLException("Subject with code '" + subjectCode + "' already exists");
            } else if (e.getSQLState().equals("23503")) {
                throw new SQLException("Invalid course ID provided");
            }
            throw e;
        }
    }
    
    public static List<Subject> getSubjectsByCourse(UUID courseId) throws SQLException {
        List<Subject> subjects = new ArrayList<>();
        String sql = "SELECT id, course_id, subject_code, subject_name FROM subjects WHERE course_id = ? ORDER BY subject_name";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setObject(1, courseId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    subjects.add(new Subject(
                        rs.getObject("id", UUID.class),
                        rs.getObject("course_id", UUID.class),
                        rs.getString("subject_code"),
                        rs.getString("subject_name")
                    ));
                }
            }
            
            LOGGER.info("Retrieved " + subjects.size() + " subjects for course ID: " + courseId);
        }
        
        return subjects;
    }
    
    // Enhanced User Management
    public static User getUserByUsername(String username) throws SQLException {
        String sql = "SELECT id, username, password_hash, role, created_at FROM users WHERE username = ? AND role IS NOT NULL";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username.trim().toLowerCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                        rs.getObject("id", UUID.class),
                        rs.getString("username"),
                        null, // Don't return email for security
                        rs.getString("password_hash"),
                        rs.getString("role"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                    );
                }
            }
        }
        
        return null;
    }
    
    public static void logUserLogin(UUID userId, String ipAddress, boolean success) throws SQLException {
        String sql = "INSERT INTO login_logs (user_id, ip_address, success) VALUES (?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setObject(1, userId);
            pstmt.setString(2, ipAddress);
            pstmt.setBoolean(3, success);
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            // Don't fail the login process if logging fails
            LOGGER.log(Level.WARNING, "Failed to log user login attempt", e);
        }
    }
    
    public static void updateUserLastLogin(UUID userId) throws SQLException {
        String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setObject(1, userId);
            pstmt.executeUpdate();
        }
    }
    
    // Enhanced Question Methods with better error handling
    public static void saveQuestions(List<Question> questions) throws SQLException {
        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("Questions list cannot be null or empty");
        }
        
        String sql = "INSERT INTO questions (question_text, question_type, marks, difficulty_level, bloom_taxonomy_level, created_by) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Question q : questions) {
                    pstmt.setString(1, q.getQuestionText());
                    pstmt.setString(2, q.getQuestionType());
                    pstmt.setInt(3, q.getMarks());
                    pstmt.setString(4, q.getDifficultyLevel());
                    pstmt.setString(5, q.getBloomTaxonomyLevel());
                    pstmt.setObject(6, q.getCreatedBy());
                    pstmt.addBatch();
                }
                
                int[] results = pstmt.executeBatch();
                conn.commit();
                
                LOGGER.info("Successfully saved " + results.length + " questions");
                
            } catch (SQLException e) {
                conn.rollback();
                throw new SQLException("Failed to save questions batch: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
    
    // Database health check methods
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Database connection test failed", e);
            return false;
        }
    }
    
    public static DatabaseHealth getConnectionPoolStatus() {
        if (dataSource == null) {
            return new DatabaseHealth(false, "Connection pool not initialized", 0, 0, 0);
        }
        
        return new DatabaseHealth(
            !dataSource.isClosed(),
            dataSource.isClosed() ? "Pool is closed" : "Pool is active",
            dataSource.getHikariPoolMXBean().getActiveConnections(),
            dataSource.getHikariPoolMXBean().getIdleConnections(),
            dataSource.getHikariPoolMXBean().getTotalConnections()
        );
    }
    
    // Keep existing methods for backward compatibility but add logging
    public static List<ExamType> getExamTypes() throws SQLException {
        List<ExamType> examTypes = new ArrayList<>();
        String sql = "SELECT id, type_name FROM exam_types ORDER BY type_name";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                examTypes.add(new ExamType(
                    rs.getObject("id", UUID.class),
                    rs.getString("type_name")
                ));
            }
            
            LOGGER.info("Retrieved " + examTypes.size() + " exam types");
        }
        
        return examTypes;
    }
    
    // Add all other existing methods from original DatabaseUtil...
    // (I'll include the key ones, but you should migrate all existing methods)
    
    public static void addBlueprint(UUID subjectId, UUID examTypeId, String title, int totalMarks, int duration) throws SQLException {
        String sql = "INSERT INTO blueprints (subject_id, exam_type_id, title, total_marks, duration_minutes) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setObject(1, subjectId);
            pstmt.setObject(2, examTypeId);
            pstmt.setString(3, title.trim());
            pstmt.setInt(4, totalMarks);
            pstmt.setInt(5, duration);
            pstmt.executeUpdate();
            
            LOGGER.info("Blueprint added successfully: " + title);
        }
    }
    
    // ... continue with other existing methods
    
    // Inner class for database health monitoring
    public static class DatabaseHealth {
        private final boolean isHealthy;
        private final String status;
        private final int activeConnections;
        private final int idleConnections;
        private final int totalConnections;
        
        public DatabaseHealth(boolean isHealthy, String status, int activeConnections, int idleConnections, int totalConnections) {
            this.isHealthy = isHealthy;
            this.status = status;
            this.activeConnections = activeConnections;
            this.idleConnections = idleConnections;
            this.totalConnections = totalConnections;
        }
        
        // Getters
        public boolean isHealthy() { return isHealthy; }
        public String getStatus() { return status; }
        public int getActiveConnections() { return activeConnections; }
        public int getIdleConnections() { return idleConnections; }
        public int getTotalConnections() { return totalConnections; }
    }
}