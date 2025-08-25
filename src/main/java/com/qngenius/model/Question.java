package com.qngenius.model;

import java.util.UUID;

public class Question {
    private final UUID questionId;
    private final UUID unitId;
    private final UUID coId; // Added coId to match schema
    private final String questionText;
    private final String questionType;
    private final int marks;
    private final String difficultyLevel;
    private final String importanceLevel;
    private final String applicationLevel;
    private final String bloomTaxonomyLevel;
    private final String courseOutcome;
    private final String keywords;
    private final UUID createdBy;
    private final String rawContent;

    /**
     * Constructor for creating a NEW question object before saving to the database.
     * questionId, unitId, coId will be null initially as they are set by the DB or
     * later linked.
     */
    public Question(String questionText, String questionType, int marks, String difficultyLevel,
            String bloomTaxonomyLevel, UUID createdBy) {
        this(null, null, null, questionText, questionType, marks, difficultyLevel,
                null, null, bloomTaxonomyLevel, null, null, createdBy, null);
    }

    /**
     * Simplified constructor for retrieving questions from the
     * DatabaseUtil.getQuestions method where only specific fields are fetched.
     */
    public Question(UUID questionId, String questionText, String questionType, int marks, String difficultyLevel,
            String bloomTaxonomyLevel) {
        this(questionId, null, null, questionText, questionType, marks, difficultyLevel,
                null, null, bloomTaxonomyLevel, null, null, null, null);
    }

    /**
     * Full constructor for retrieving questions from the database, matching all
     * columns.
     */
    public Question(UUID questionId, UUID unitId, UUID coId, String questionText, String questionType, int marks,
            String difficultyLevel,
            String importanceLevel, String applicationLevel, String bloomTaxonomyLevel,
            String courseOutcome, String keywords, UUID createdBy, String rawContent) {
        this.questionId = questionId;
        this.unitId = unitId;
        this.coId = coId;
        this.questionText = questionText;
        this.questionType = questionType;
        this.marks = marks;
        this.difficultyLevel = difficultyLevel;
        this.importanceLevel = importanceLevel;
        this.applicationLevel = applicationLevel;
        this.bloomTaxonomyLevel = bloomTaxonomyLevel;
        this.courseOutcome = courseOutcome;
        this.keywords = keywords;
        this.createdBy = createdBy;
        this.rawContent = rawContent;
    }

    /**
     * Constructor for retrieving questions from a query that doesn't include the
     * coId.
     * This is the constructor that matches the call in the getQuestionsByCriteria
     * method.
     */
    public Question(UUID questionId, UUID unitId, String questionText, String questionType, int marks,
            String difficultyLevel, String importanceLevel, String applicationLevel, String bloomTaxonomyLevel,
            String courseOutcome, String keywords, UUID createdBy, String rawContent) {
        this.questionId = questionId;
        this.unitId = unitId;
        this.coId = null; // Set coId to null as it's not provided in this specific query
        this.questionText = questionText;
        this.questionType = questionType;
        this.marks = marks;
        this.difficultyLevel = difficultyLevel;
        this.importanceLevel = importanceLevel;
        this.applicationLevel = applicationLevel;
        this.bloomTaxonomyLevel = bloomTaxonomyLevel;
        this.courseOutcome = courseOutcome;
        this.keywords = keywords;
        this.createdBy = createdBy;
        this.rawContent = rawContent;
    }

    // --- Getters for all fields ---
    public UUID getQuestionId() {
        return questionId;
    }

    public UUID getUnitId() {
        return unitId;
    }

    public UUID getCoId() {
        return coId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getQuestionType() {
        return questionType;
    }

    public int getMarks() {
        return marks;
    }

    public String getDifficultyLevel() {
        return difficultyLevel;
    }

    public String getImportanceLevel() {
        return importanceLevel;
    }

    public String getApplicationLevel() {
        return applicationLevel;
    }

    public String getBloomTaxonomyLevel() {
        return bloomTaxonomyLevel;
    }

    public String getCourseOutcome() {
        return courseOutcome;
    }

    public String getKeywords() {
        return keywords;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public String getRawContent() {
        return rawContent;
    }
}