package com.qngenius.model;

import java.util.UUID;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ExamType {
    private final UUID id;
    private final StringProperty typeName;

    public ExamType(UUID id, String typeName) {
        this.id = id;
        this.typeName = new SimpleStringProperty(typeName);
    }

    public UUID getId() { return id; }
    public String getTypeName() { return typeName.get(); }
    public StringProperty typeNameProperty() { return typeName; }
}