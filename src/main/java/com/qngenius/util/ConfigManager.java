package com.qngenius.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ConfigManager {
    private static final Logger LOGGER = Logger.getLogger(ConfigManager.class.getName());
    private static ConfigManager instance;
    private Properties dbProperties;
    private Properties appProperties;
    
    private ConfigManager() {
        loadConfigurations();
    }
    
    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
    
    private void loadConfigurations() {
        loadDatabaseConfig();
        loadApplicationConfig();
    }
    
    private void loadDatabaseConfig() {
        dbProperties = new Properties();
        
        // Try multiple locations for database config
        String[] configPaths = {
            "/config/db.properties",
            "/db.properties",
            "config/db.properties"
        };
        
        boolean loaded = false;
        for (String path : configPaths) {
            try (InputStream input = getClass().getResourceAsStream(path)) {
                if (input != null) {
                    dbProperties.load(input);
                    LOGGER.info("Database configuration loaded from: " + path);
                    loaded = true;
                    break;
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to load database config from: " + path, e);
            }
        }
        
        if (!loaded) {
            LOGGER.warning("No database configuration found, using defaults");
            setDefaultDatabaseConfig();
        }
    }
    
    private void loadApplicationConfig() {
        appProperties = new Properties();
        
        try (InputStream input = getClass().getResourceAsStream("/config/app.properties")) {
            if (input != null) {
                appProperties.load(input);
                LOGGER.info("Application configuration loaded successfully");
            } else {
                setDefaultAppConfig();
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load application config, using defaults", e);
            setDefaultAppConfig();
        }
    }
    
    private void setDefaultDatabaseConfig() {
        dbProperties.setProperty("db.url", "jdbc:postgresql://localhost:5432/qngenius");
        dbProperties.setProperty("db.user", "postgres");
        dbProperties.setProperty("db.password", "password");
        dbProperties.setProperty("db.driver", "org.postgresql.Driver");
        dbProperties.setProperty("db.pool.initialSize", "5");
        dbProperties.setProperty("db.pool.maxSize", "20");
        dbProperties.setProperty("db.pool.maxWaitTime", "30000");
    }
    
    private void setDefaultAppConfig() {
        appProperties.setProperty("app.name", "QnGenius - Question Bank Generator");
        appProperties.setProperty("app.version", "1.0.0");
        appProperties.setProperty("app.theme", "default");
        appProperties.setProperty("app.session.timeout", "3600");
        appProperties.setProperty("app.max.upload.size", "10485760"); // 10MB
        appProperties.setProperty("app.backup.enabled", "true");
        appProperties.setProperty("app.backup.interval", "86400"); // 24 hours
    }
    
    // Database configuration getters
    public String getDatabaseUrl() {
        return dbProperties.getProperty("db.url");
    }
    
    public String getDatabaseUser() {
        return dbProperties.getProperty("db.user");
    }
    
    public String getDatabasePassword() {
        return dbProperties.getProperty("db.password");
    }
    
    public String getDatabaseDriver() {
        return dbProperties.getProperty("db.driver", "org.postgresql.Driver");
    }
    
    public int getConnectionPoolInitialSize() {
        return Integer.parseInt(dbProperties.getProperty("db.pool.initialSize", "5"));
    }
    
    public int getConnectionPoolMaxSize() {
        return Integer.parseInt(dbProperties.getProperty("db.pool.maxSize", "20"));
    }
    
    public int getConnectionPoolMaxWaitTime() {
        return Integer.parseInt(dbProperties.getProperty("db.pool.maxWaitTime", "30000"));
    }
    
    // Application configuration getters
    public String getAppName() {
        return appProperties.getProperty("app.name");
    }
    
    public String getAppVersion() {
        return appProperties.getProperty("app.version");
    }
    
    public String getAppTheme() {
        return appProperties.getProperty("app.theme", "default");
    }
    
    public int getSessionTimeout() {
        return Integer.parseInt(appProperties.getProperty("app.session.timeout", "3600"));
    }
    
    public long getMaxUploadSize() {
        return Long.parseLong(appProperties.getProperty("app.max.upload.size", "10485760"));
    }
    
    public boolean isBackupEnabled() {
        return Boolean.parseBoolean(appProperties.getProperty("app.backup.enabled", "true"));
    }
    
    public int getBackupInterval() {
        return Integer.parseInt(appProperties.getProperty("app.backup.interval", "86400"));
    }
    
    // Environment-specific methods
    public boolean isDevelopmentMode() {
        return "development".equals(System.getProperty("app.environment", "production"));
    }
    
    public boolean isProductionMode() {
        return !isDevelopmentMode();
    }
    
    // Validation methods
    public boolean validateDatabaseConfig() {
        return getDatabaseUrl() != null && 
               getDatabaseUser() != null && 
               getDatabasePassword() != null;
    }
}