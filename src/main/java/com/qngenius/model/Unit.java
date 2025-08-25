package com.qngenius.model;

import java.util.UUID;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Unit {

    private final UUID id;
    private final UUID subjectId;
    private final StringProperty unitName;

    public Unit(UUID id, UUID subjectId, String unitName) {
        this.id = id;
        this.subjectId = subjectId;
        this.unitName = new SimpleStringProperty(unitName);
    }

    public UUID getId() {
        return id;
    }

    public UUID getSubjectId() {
        return subjectId;
    }

    public String getUnitName() {
        return unitName.get();
    }

    public StringProperty unitNameProperty() {
        return unitName;
    }
}