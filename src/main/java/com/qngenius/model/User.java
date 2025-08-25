package com.qngenius.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class User {
    private final UUID id;
    private final StringProperty username;
    private final StringProperty email;
    private final String passwordHash; // Don't expose via property for security
    private final StringProperty role;
    private final LocalDateTime createdAt;
    private final Set<String> permissions;
    private LocalDateTime lastLogin;
    private boolean isActive;

    // Constructor for creating new user
    public User(String username, String email, String passwordHash, String role) {
        this(UUID.randomUUID(), username, email, passwordHash, role, LocalDateTime.now());
    }

    // Constructor for database retrieval
    public User(UUID id, String username, String email, String passwordHash, String role, LocalDateTime createdAt) {
        this.id = id;
        this.username = new SimpleStringProperty(username);
        this.email = new SimpleStringProperty(email != null ? email : "");
        this.passwordHash = passwordHash;
        this.role = new SimpleStringProperty(role);
        this.createdAt = createdAt;
        this.permissions = new HashSet<>();
        this.isActive = true;
        initializePermissions();
    }

    // Simplified constructor for authentication
    public User(UUID id, String username, String role) {
        this(id, username, null, null, role, LocalDateTime.now());
    }

    private void initializePermissions() {
        switch (role.get().toLowerCase()) {
            case "admin":
                permissions.add("MANAGE_USERS");
                permissions.add("MANAGE_COURSES");
                permissions.add("MANAGE_SUBJECTS");
                permissions.add("MANAGE_BLUEPRINTS");
                permissions.add("VIEW_ANALYTICS");
                permissions.add("SYSTEM_CONFIG");
                break;
            case "coe":
            case "controller":
                permissions.add("GENERATE_PAPERS");
                permissions.add("VIEW_BLUEPRINTS");
                permissions.add("EXPORT_PAPERS");
                permissions.add("VIEW_ANALYTICS");
                break;
            case "teacher":
            case "faculty":
                permissions.add("CREATE_QUESTIONS");
                permissions.add("VIEW_QUESTIONS");
                permissions.add("EDIT_OWN_QUESTIONS");
                permissions.add("VIEW_BLUEPRINTS");
                break;
            case "student":
                permissions.add("VIEW_PAPERS");
                permissions.add("SUBMIT_ANSWERS");
                break;
            default:
                // No permissions for unknown roles
                break;
        }
    }

    // Getters
    public UUID getId() { return id; }
    
    public String getUsername() { return username.get(); }
    public StringProperty usernameProperty() { return username; }
    
    public String getEmail() { return email.get(); }
    public StringProperty emailProperty() { return email; }
    
    public String getRole() { return role.get(); }
    public StringProperty roleProperty() { return role; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastLogin() { return lastLogin; }
    public boolean isActive() { return isActive; }
    public Set<String> getPermissions() { return new HashSet<>(permissions); }
    
    // Setters
    public void setEmail(String email) { this.email.set(email); }
    public void setRole(String role) { 
        this.role.set(role); 
        initializePermissions(); // Reinitialize permissions when role changes
    }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    public void setActive(boolean active) { this.isActive = active; }

    // Permission checking methods
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role.get());
    }

    public boolean isCoe() {
        return "coe".equalsIgnoreCase(role.get()) || "controller".equalsIgnoreCase(role.get());
    }

    public boolean isTeacher() {
        return "teacher".equalsIgnoreCase(role.get()) || "faculty".equalsIgnoreCase(role.get());
    }

    public boolean isStudent() {
        return "student".equalsIgnoreCase(role.get());
    }

    // For display purposes
    public String getDisplayName() {
        return username.get();
    }

    public String getRoleDisplayName() {
        switch (role.get().toLowerCase()) {
            case "coe": return "Controller of Examinations";
            case "admin": return "Administrator";
            case "teacher": case "faculty": return "Faculty";
            case "student": return "Student";
            default: return role.get();
        }
    }

    @Override
    public String toString() {
        return String.format("User{id=%s, username='%s', role='%s', active=%s}", 
                           id, username.get(), role.get(), isActive);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}