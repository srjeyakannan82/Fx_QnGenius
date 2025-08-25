package com.qngenius.model;

import java.util.UUID;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Subject {

    private final UUID id;
    private final UUID courseId;
    private final StringProperty subjectCode;
    private final StringProperty subjectName;

    public Subject(UUID id, UUID courseId, String subjectCode, String subjectName) {
        this.id = id;
        this.courseId = courseId;
        this.subjectCode = new SimpleStringProperty(subjectCode);
        this.subjectName = new SimpleStringProperty(subjectName);
    }

    public UUID getId() {
        return id;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public String getSubjectCode() {
        return subjectCode.get();
    }

    public StringProperty subjectCodeProperty() {
        return subjectCode;
    }

    public String getSubjectName() {
        return subjectName.get();
    }

    public StringProperty subjectNameProperty() {
        return subjectName;
    }
}