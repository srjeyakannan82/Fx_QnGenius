package com.qngenius.service;

import com.qngenius.model.Question;
import com.qngenius.model.BlueprintCriteria;
import com.qngenius.util.DatabaseUtil;
import com.qngenius.exception.QuestionServiceException;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

public class QuestionService {
    
    private static final Logger LOGGER = Logger.getLogger(QuestionService.class.getName());
    private static QuestionService instance;
    
    private QuestionService() {}
    
    public static synchronized QuestionService getInstance() {
        if (instance == null) {
            instance = new QuestionService();
        }
        return instance;
    }

    /**
     * Saves a list of questions to the database with validation
     */
    public void saveQuestions(List<Question> questions, UUID userId) throws QuestionServiceException {
        if (questions == null || questions.isEmpty()) {
            throw new QuestionServiceException("Question list cannot be null or empty");
        }
        
        // Validate questions before saving
        validateQuestions(questions);
        
        try {
            DatabaseUtil.saveQuestions(questions);
            LOGGER.info(String.format("Successfully saved %d questions for user %s", questions.size(), userId));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to save questions to database", e);
            throw new QuestionServiceException("Failed to save questions: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves questions based on criteria with fallback options
     */
    public List<Question> getQuestionsByCriteria(UUID subjectId, BlueprintCriteria criteria) throws QuestionServiceException {
        try {
            List<Question> questions = DatabaseUtil.getQuestionsByCriteria(subjectId, criteria);
            
            // If not enough questions found, try with relaxed criteria
            if (questions.size() < criteria.getNumberOfQuestions()) {
                LOGGER.warning(String.format("Only found %d questions out of %d requested. Trying with relaxed criteria.", 
                              questions.size(), criteria.getNumberOfQuestions()));
                
                questions = getQuestionsWithRelaxedCriteria(subjectId, criteria);
            }
            
            return questions;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve questions by criteria", e);
            throw new QuestionServiceException("Failed to retrieve questions: " + e.getMessage(), e);
        }
    }

    /**
     * Search questions with multiple filters
     */
    public List<Question> searchQuestions(QuestionSearchCriteria searchCriteria) throws QuestionServiceException {
        try {
            List<Question> allQuestions = DatabaseUtil.getFullQuestions();
            
            return allQuestions.stream()
                .filter(q -> matchesSearchCriteria(q, searchCriteria))
                .collect(Collectors.toList());
                
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to search questions", e);
            throw new QuestionServiceException("Failed to search questions: " + e.getMessage(), e);
        }
    }

    /**
     * Get question statistics by subject
     */
    public QuestionStatistics getQuestionStatistics(UUID subjectId) throws QuestionServiceException {
        try {
            List<Question> questions = getQuestionsBySubject(subjectId);
            return calculateStatistics(questions);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get question statistics", e);
            throw new QuestionServiceException("Failed to get statistics: " + e.getMessage(), e);
        }
    }

    /**
     * Validate individual question for duplicates
     */
    public boolean isDuplicateQuestion(Question question, UUID subjectId) throws QuestionServiceException {
        try {
            List<Question> existingQuestions = getQuestionsBySubject(subjectId);
            
            return existingQuestions.stream()
                .anyMatch(existing -> isSimilarQuestion(existing, question));
                
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to check for duplicate questions", e);
            throw new QuestionServiceException("Failed to check duplicates: " + e.getMessage(), e);
        }
    }

    // Private helper methods
    private void validateQuestions(List<Question> questions) throws QuestionServiceException {
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            if (q.getQuestionText() == null || q.getQuestionText().trim().isEmpty()) {
                throw new QuestionServiceException("Question " + (i+1) + " has empty text");
            }
            if (q.getMarks() <= 0) {
                throw new QuestionServiceException("Question " + (i+1) + " has invalid marks: " + q.getMarks());
            }
        }
    }

    private List<Question> getQuestionsWithRelaxedCriteria(UUID subjectId, BlueprintCriteria criteria) throws SQLException {
        // Try with different difficulty levels if not enough questions
        String[] difficultyLevels = {"Easy", "Medium", "Hard"};
        
        for (String difficulty : difficultyLevels) {
            if (!difficulty.equals(criteria.getDifficultyLevel())) {
                BlueprintCriteria relaxedCriteria = new BlueprintCriteria(
                    criteria.getId(),
                    criteria.getBlueprintSectionId(),
                    criteria.getQuestionType(),
                    criteria.getNumberOfQuestions(),
                    criteria.getMarksPerQuestion(),
                    difficulty,
                    criteria.getBloomTaxonomyLevel()
                );
                
                List<Question> questions = DatabaseUtil.getQuestionsByCriteria(subjectId, relaxedCriteria);
                if (!questions.isEmpty()) {
                    return questions;
                }
            }
        }
        
        return DatabaseUtil.getQuestionsByCriteria(subjectId, criteria);
    }

    private boolean matchesSearchCriteria(Question question, QuestionSearchCriteria criteria) {
        if (criteria.getQuestionType() != null && !criteria.getQuestionType().equals(question.getQuestionType())) {
            return false;
        }
        if (criteria.getDifficultyLevel() != null && !criteria.getDifficultyLevel().equals(question.getDifficultyLevel())) {
            return false;
        }
        if (criteria.getKeywords() != null && !containsKeywords(question, criteria.getKeywords())) {
            return false;
        }
        if (criteria.getMinMarks() != null && question.getMarks() < criteria.getMinMarks()) {
            return false;
        }
        if (criteria.getMaxMarks() != null && question.getMarks() > criteria.getMaxMarks()) {
            return false;
        }
        return true;
    }

    private boolean containsKeywords(Question question, String keywords) {
        String questionText = question.getQuestionText().toLowerCase();
        String questionKeywords = question.getKeywords() != null ? question.getKeywords().toLowerCase() : "";
        String searchTerms = keywords.toLowerCase();
        
        String[] terms = searchTerms.split("\\s+");
        for (String term : terms) {
            if (!questionText.contains(term) && !questionKeywords.contains(term)) {
                return false;
            }
        }
        return true;
    }

    private List<Question> getQuestionsBySubject(UUID subjectId) throws SQLException {
        // This method would need to be implemented in DatabaseUtil
        // For now, returning all questions (you'll need to modify this)
        return DatabaseUtil.getFullQuestions().stream()
            .filter(q -> q.getUnitId() != null) // Basic filter, needs proper subject relationship
            .collect(Collectors.toList());
    }

    private boolean isSimilarQuestion(Question q1, Question q2) {
        // Simple similarity check - can be enhanced with more sophisticated algorithms
        String text1 = q1.getQuestionText().toLowerCase().trim();
        String text2 = q2.getQuestionText().toLowerCase().trim();
        
        // Check for exact match
        if (text1.equals(text2)) {
            return true;
        }
        
        // Check for high similarity (more than 80% similar)
        double similarity = calculateSimilarity(text1, text2);
        return similarity > 0.8;
    }

    private double calculateSimilarity(String s1, String s2) {
        // Simple Levenshtein distance-based similarity
        int distance = levenshteinDistance(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());
        return maxLength == 0 ? 1.0 : 1.0 - (double) distance / maxLength;
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                        dp[i-1][j-1] + (s1.charAt(i-1) == s2.charAt(j-1) ? 0 : 1),
                        Math.min(dp[i-1][j] + 1, dp[i][j-1] + 1)
                    );
                }
            }
        }
        return dp[s1.length()][s2.length()];
    }

    private QuestionStatistics calculateStatistics(List<Question> questions) {
        Map<String, Integer> typeCount = new HashMap<>();
        Map<String, Integer> difficultyCount = new HashMap<>();
        Map<String, Integer> bloomCount = new HashMap<>();
        
        int totalMarks = 0;
        
        for (Question q : questions) {
            typeCount.merge(q.getQuestionType(), 1, Integer::sum);
            difficultyCount.merge(q.getDifficultyLevel(), 1, Integer::sum);
            if (q.getBloomTaxonomyLevel() != null) {
                bloomCount.merge(q.getBloomTaxonomyLevel(), 1, Integer::sum);
            }
            totalMarks += q.getMarks();
        }
        
        return new QuestionStatistics(questions.size(), totalMarks, typeCount, difficultyCount, bloomCount);
    }

    // Inner classes for search criteria and statistics
    public static class QuestionSearchCriteria {
        private String questionType;
        private String difficultyLevel;
        private String keywords;
        private Integer minMarks;
        private Integer maxMarks;
        private UUID unitId;
        
        // Constructors and getters/setters
        public QuestionSearchCriteria() {}
        
        // Getters
        public String getQuestionType() { return questionType; }
        public String getDifficultyLevel() { return difficultyLevel; }
        public String getKeywords() { return keywords; }
        public Integer getMinMarks() { return minMarks; }
        public Integer getMaxMarks() { return maxMarks; }
        public UUID getUnitId() { return unitId; }
        
        // Setters
        public void setQuestionType(String questionType) { this.questionType = questionType; }
        public void setDifficultyLevel(String difficultyLevel) { this.difficultyLevel = difficultyLevel; }
        public void setKeywords(String keywords) { this.keywords = keywords; }
        public void setMinMarks(Integer minMarks) { this.minMarks = minMarks; }
        public void setMaxMarks(Integer maxMarks) { this.maxMarks = maxMarks; }
        public void setUnitId(UUID unitId) { this.unitId = unitId; }
    }

    public static class QuestionStatistics {
        private final int totalQuestions;
        private final int totalMarks;
        private final Map<String, Integer> questionTypeDistribution;
        private final Map<String, Integer> difficultyDistribution;
        private final Map<String, Integer> bloomTaxonomyDistribution;
        
        public QuestionStatistics(int totalQuestions, int totalMarks, 
                                Map<String, Integer> typeDistribution,
                                Map<String, Integer> difficultyDistribution,
                                Map<String, Integer> bloomDistribution) {
            this.totalQuestions = totalQuestions;
            this.totalMarks = totalMarks;
            this.questionTypeDistribution = new HashMap<>(typeDistribution);
            this.difficultyDistribution = new HashMap<>(difficultyDistribution);
            this.bloomTaxonomyDistribution = new HashMap<>(bloomDistribution);
        }
        
        // Getters
        public int getTotalQuestions() { return totalQuestions; }
        public int getTotalMarks() { return totalMarks; }
        public double getAverageMarks() { return totalQuestions > 0 ? (double) totalMarks / totalQuestions : 0; }
        public Map<String, Integer> getQuestionTypeDistribution() { return new HashMap<>(questionTypeDistribution); }
        public Map<String, Integer> getDifficultyDistribution() { return new HashMap<>(difficultyDistribution); }
        public Map<String, Integer> getBloomTaxonomyDistribution() { return new HashMap<>(bloomTaxonomyDistribution); }
    }
}

// Custom exception class
class QuestionServiceException extends Exception {
    public QuestionServiceException(String message) {
        super(message);
    }
    
    public QuestionServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}