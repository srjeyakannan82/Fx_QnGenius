package com.qngenius.model;

import java.util.UUID;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Blueprint {
    private final UUID id;
    private final UUID subjectId;
    private final UUID examTypeId;
    private final StringProperty title;
    private final SimpleIntegerProperty totalMarks;
    private final SimpleIntegerProperty durationMinutes;
    private final boolean isCustom;

    public Blueprint(UUID id, UUID subjectId, UUID examTypeId, String title, int totalMarks, int durationMinutes, boolean isCustom) {
        this.id = id;
        this.subjectId = subjectId;
        this.examTypeId = examTypeId;
        this.title = new SimpleStringProperty(title);
        this.totalMarks = new SimpleIntegerProperty(totalMarks);
        this.durationMinutes = new SimpleIntegerProperty(durationMinutes);
        this.isCustom = isCustom;
    }

    public UUID getId() { return id; }
    public UUID getSubjectId() { return subjectId; }
    public UUID getExamTypeId() { return examTypeId; }
    public String getTitle() { return title.get(); }
    public StringProperty titleProperty() { return title; }
    public int getTotalMarks() { return totalMarks.get(); }
    public SimpleIntegerProperty totalMarksProperty() { return totalMarks; }
    public int getDurationMinutes() { return durationMinutes.get(); }
    public SimpleIntegerProperty durationMinutesProperty() { return durationMinutes; }
    public boolean isCustom() { return isCustom; }
}