package com.qngenius.model;

import java.util.UUID;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Course {

    private final UUID id;
    private final StringProperty courseCode;
    private final StringProperty courseName;

    public Course(UUID id, String courseCode, String courseName) {
        this.id = id;
        this.courseCode = new SimpleStringProperty(courseCode);
        this.courseName = new SimpleStringProperty(courseName);
    }

    public UUID getId() {
        return id;
    }

    public String getCourseCode() {
        return courseCode.get();
    }

    public StringProperty courseCodeProperty() {
        return courseCode;
    }

    public String getCourseName() {
        return courseName.get();
    }

    public StringProperty courseNameProperty() {
        return courseName;
    }
}