package com.qngenius.model;

import java.util.UUID;

public class BlueprintCriteria {

    private final UUID id;
    private final UUID blueprintSectionId;
    private final String questionType;
    private final int numberOfQuestions;
    private final int marksPerQuestion;
    private final String difficultyLevel;
    private final String bloomTaxonomyLevel;

    public BlueprintCriteria(UUID id, UUID blueprintSectionId, String questionType, int numberOfQuestions, int marksPerQuestion, String difficultyLevel, String bloomTaxonomyLevel) {
        this.id = id;
        this.blueprintSectionId = blueprintSectionId;
        this.questionType = questionType;
        this.numberOfQuestions = numberOfQuestions;
        this.marksPerQuestion = marksPerQuestion;
        this.difficultyLevel = difficultyLevel;
        this.bloomTaxonomyLevel = bloomTaxonomyLevel;
    }

    // Getters for all fields...
    public UUID getId() { return id; }
    public UUID getBlueprintSectionId() { return blueprintSectionId; }
    public String getQuestionType() { return questionType; }
    public int getNumberOfQuestions() { return numberOfQuestions; }
    public int getMarksPerQuestion() { return marksPerQuestion; }
    public String getDifficultyLevel() { return difficultyLevel; }
    public String getBloomTaxonomyLevel() { return bloomTaxonomyLevel; }
}