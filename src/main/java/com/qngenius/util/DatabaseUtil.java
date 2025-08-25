package com.qngenius.util;

import com.qngenius.model.Blueprint;
import com.qngenius.model.Course;
import com.qngenius.model.ExamType;
import com.qngenius.model.Subject;
import com.qngenius.model.Unit;
import com.qngenius.model.BlueprintCriteria;
import com.qngenius.model.Question;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public class DatabaseUtil {

    private static final Properties properties = new Properties();

    static {
        String propertiesFilePath = "C:/db.properties";
        try (FileInputStream input = new FileInputStream(propertiesFilePath)) {
            if (input == null) {
                System.out.println("sorry, unable to find db.properties at absolute path.");
            }
            properties.load(input);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        String url = properties.getProperty("db.url");
        String user = properties.getProperty("db.user");
        String password = properties.getProperty("db.password");
        return DriverManager.getConnection(url, user, password);
    }

    // --- Course Methods ---
    public static void addCourse(String courseCode, String courseName) throws SQLException {
        String sql = "INSERT INTO courses (course_code, course_name) VALUES (?, ?)";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseCode);
            pstmt.setString(2, courseName);
            pstmt.executeUpdate();
        }
    }

    public static List<Course> getAllCourses() throws SQLException {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT id, course_code, course_name FROM courses";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                courses.add(new Course(rs.getObject("id", UUID.class), rs.getString("course_code"),
                        rs.getString("course_name")));
            }
        }
        return courses;
    }

    // --- Subject Methods ---
    public static void addSubject(UUID courseId, String subjectCode, String subjectName) throws SQLException {
        String sql = "INSERT INTO subjects (course_id, subject_code, subject_name) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, courseId);
            pstmt.setString(2, subjectCode);
            pstmt.setString(3, subjectName);
            pstmt.executeUpdate();
        }
    }

    public static List<Subject> getSubjectsByCourse(UUID courseId) throws SQLException {
        List<Subject> subjects = new ArrayList<>();
        String sql = "SELECT id, course_id, subject_code, subject_name FROM subjects WHERE course_id = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, courseId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    subjects.add(new Subject(rs.getObject("id", UUID.class), rs.getObject("course_id", UUID.class),
                            rs.getString("subject_code"), rs.getString("subject_name")));
                }
            }
        }
        return subjects;
    }

    // --- Unit Methods ---
    public static void addUnit(UUID subjectId, String unitName) throws SQLException {
        String sql = "INSERT INTO units (subject_id, unit_name) VALUES (?, ?)";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, subjectId);
            pstmt.setString(2, unitName);
            pstmt.executeUpdate();
        }
    }

    public static List<Unit> getUnitsBySubject(UUID subjectId) throws SQLException {
        List<Unit> units = new ArrayList<>();
        String sql = "SELECT id, subject_id, unit_name FROM units WHERE subject_id = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, subjectId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    units.add(new Unit(rs.getObject("id", UUID.class), rs.getObject("subject_id", UUID.class),
                            rs.getString("unit_name")));
                }
            }
        }
        return units;
    }

    // --- Blueprint Methods ---
    public static List<ExamType> getExamTypes() throws SQLException {
        List<ExamType> examTypes = new ArrayList<>();
        String sql = "SELECT id, type_name FROM exam_types";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                examTypes.add(new ExamType(rs.getObject("id", UUID.class), rs.getString("type_name")));
            }
        }
        return examTypes;
    }

    public static void addBlueprint(UUID subjectId, UUID examTypeId, String title, int totalMarks, int duration)
            throws SQLException {
        String sql = "INSERT INTO blueprints (subject_id, exam_type_id, title, total_marks, duration_minutes) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, subjectId);
            pstmt.setObject(2, examTypeId);
            pstmt.setString(3, title);
            pstmt.setInt(4, totalMarks);
            pstmt.setInt(5, duration);
            pstmt.executeUpdate();
        }
    }

    public static List<Blueprint> getBlueprintsBySubject(UUID subjectId) throws SQLException {
        List<Blueprint> blueprints = new ArrayList<>();
        String sql = "SELECT id, subject_id, exam_type_id, title, total_marks, duration_minutes, is_custom FROM blueprints WHERE subject_id = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, subjectId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    blueprints.add(new Blueprint(
                            rs.getObject("id", UUID.class),
                            rs.getObject("subject_id", UUID.class),
                            rs.getObject("exam_type_id", UUID.class),
                            rs.getString("title"),
                            rs.getInt("total_marks"),
                            rs.getInt("duration_minutes"),
                            rs.getBoolean("is_custom")));
                }
            }
        }
        return blueprints;
    }

    // Method to get all blueprints for a specific subject AND exam type
    public static List<Blueprint> getBlueprintsBySubjectAndExamType(UUID subjectId, UUID examTypeId)
            throws SQLException {
        List<Blueprint> blueprints = new ArrayList<>();
        String sql = "SELECT id, subject_id, exam_type_id, title, total_marks, duration_minutes, is_custom FROM blueprints WHERE subject_id = ? AND exam_type_id = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, subjectId);
            pstmt.setObject(2, examTypeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    blueprints.add(new Blueprint(
                            rs.getObject("id", UUID.class),
                            rs.getObject("subject_id", UUID.class),
                            rs.getObject("exam_type_id", UUID.class),
                            rs.getString("title"),
                            rs.getInt("total_marks"),
                            rs.getInt("duration_minutes"),
                            rs.getBoolean("is_custom")));
                }
            }
        }
        return blueprints;
    }

    // Method to get all criteria for a given blueprint
    public static List<BlueprintCriteria> getBlueprintCriteria(UUID blueprintId) throws SQLException {
        List<BlueprintCriteria> criteriaList = new ArrayList<>();
        String sql = "SELECT bc.id, bc.blueprint_section_id, bc.question_type, bc.number_of_questions, bc.marks_per_question, bc.difficulty_level, bc.bloom_taxonomy_level FROM blueprint_criteria bc JOIN blueprint_sections bs ON bc.blueprint_section_id = bs.id WHERE bs.blueprint_id = ?";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, blueprintId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    criteriaList.add(new BlueprintCriteria(
                            rs.getObject("id", UUID.class),
                            rs.getObject("blueprint_section_id", UUID.class),
                            rs.getString("question_type"),
                            rs.getInt("number_of_questions"),
                            rs.getInt("marks_per_question"),
                            rs.getString("difficulty_level"),
                            rs.getString("bloom_taxonomy_level")));
                }
            }
        }
        return criteriaList;
    }

    public static List<Question> getFullQuestions() throws SQLException {
        List<Question> questions = new ArrayList<>();
        String sql = "SELECT * FROM questions";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                questions.add(new Question(
                        rs.getObject("question_id", UUID.class),
                        rs.getObject("unit_id", UUID.class),
                        rs.getObject("co_id", UUID.class),
                        rs.getString("question_text"),
                        rs.getString("question_type"),
                        rs.getInt("marks"),
                        rs.getString("difficulty_level"),
                        rs.getString("importance_level"),
                        rs.getString("application_level"),
                        rs.getString("bloom_taxonomy_level"),
                        rs.getString("course_outcome"),
                        rs.getString("keywords"),
                        rs.getObject("created_by", UUID.class),
                        rs.getString("raw_content")));
            }
        }
        return questions;
    }

    // Method to get questions that match specific criteria
    public static List<Question> getQuestionsByCriteria(UUID subjectId, BlueprintCriteria criteria)
            throws SQLException {
        List<Question> questions = new ArrayList<>();
        String sql = "SELECT * FROM questions " +
                "WHERE subject_id = ? AND marks = ? AND difficulty_level = ? AND bloom_taxonomy_level = ? " +
                "ORDER BY RANDOM() LIMIT ?";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, subjectId);
            pstmt.setInt(2, criteria.getMarksPerQuestion());
            pstmt.setString(3, criteria.getDifficultyLevel());
            pstmt.setString(4, criteria.getBloomTaxonomyLevel());
            pstmt.setInt(5, criteria.getNumberOfQuestions());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    questions.add(new Question(
                            rs.getObject("question_id", UUID.class),
                            rs.getObject("unit_id", UUID.class),
                            rs.getString("question_text"),
                            rs.getString("question_type"),
                            rs.getInt("marks"),
                            rs.getString("difficulty_level"),
                            rs.getString("importance_level"),
                            rs.getString("application_level"),
                            rs.getString("bloom_taxonomy_level"),
                            rs.getString("course_outcome"),
                            rs.getString("keywords"),
                            rs.getObject("created_by", UUID.class),
                            rs.getString("raw_content")));
                }
            }
        }
        return questions;
    }

    /**
     * Saves a list of questions to the database.
     * 
     * @param questions The list of Question objects to save.
     * @throws SQLException If a database access error occurs.
     */
    public static void saveQuestions(List<Question> questions) throws SQLException {
        String sql = "INSERT INTO questions (question_id, question_text, question_type, marks, difficulty_level, bloom_taxonomy_level) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Question q : questions) {
                pstmt.setObject(1, UUID.randomUUID());
                pstmt.setString(2, q.getQuestionText());
                pstmt.setString(3, q.getQuestionType());
                pstmt.setInt(4, q.getMarks());
                pstmt.setString(5, q.getDifficultyLevel());
                pstmt.setString(6, q.getBloomTaxonomyLevel());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    /**
     * Retrieves questions from the database based on criteria.
     * 
     * @param questionType    The type of question (e.g., Short Answer).
     * @param difficultyLevel The difficulty level (e.g., Easy, Medium, Hard).
     * @return A list of matching Question objects.
     * @throws SQLException If a database access error occurs.
     */
    public static List<Question> getQuestions(String questionType, String difficultyLevel) throws SQLException {
        List<Question> questions = new ArrayList<>();
        String sql = "SELECT * FROM questions WHERE question_type = ? AND difficulty_level = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, questionType);
            pstmt.setString(2, difficultyLevel);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    questions.add(new Question(
                            rs.getObject("question_id", UUID.class),
                            rs.getString("question_text"),
                            rs.getString("question_type"),
                            rs.getInt("marks"),
                            rs.getString("difficulty_level"),
                            rs.getString("bloom_taxonomy_level")));
                }
            }
        }
        return questions;
    }
}