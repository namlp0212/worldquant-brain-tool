package demo.webapp;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Configuration loader that reads settings from config.properties file.
 *
 * The loader searches for config.properties in the following order:
 * 1. Current working directory
 * 2. User home directory
 * 3. Classpath (src/main/resources)
 *
 * Environment variables can override config file values using the prefix "WQ_"
 * e.g., WQ_COOKIE, WQ_SMTP_HOST, etc.
 */
public class ConfigLoader {

    private static final Properties properties = new Properties();
    private static boolean loaded = false;

    static {
        loadConfig();
    }

    private static void loadConfig() {
        if (loaded) return;

        // Try to load from different locations
        String[] configPaths = {
            "config.properties",                                    // Current directory
            System.getProperty("user.home") + "/config.properties", // Home directory
        };

        boolean configFound = false;

        // Try file system paths first
        for (String path : configPaths) {
            Path configPath = Paths.get(path);
            if (Files.exists(configPath)) {
                try (InputStream input = new FileInputStream(configPath.toFile())) {
                    properties.load(input);
                    System.out.println("Config loaded from: " + configPath.toAbsolutePath());
                    configFound = true;
                    break;
                } catch (IOException e) {
                    System.err.println("Error loading config from " + path + ": " + e.getMessage());
                }
            }
        }

        // Try classpath as fallback
        if (!configFound) {
            try (InputStream input = ConfigLoader.class.getClassLoader()
                    .getResourceAsStream("config.properties")) {
                if (input != null) {
                    properties.load(input);
                    System.out.println("Config loaded from classpath");
                    configFound = true;
                }
            } catch (IOException e) {
                System.err.println("Error loading config from classpath: " + e.getMessage());
            }
        }

        if (!configFound) {
            System.err.println("WARNING: config.properties not found! Using default values.");
            System.err.println("Please create config.properties from config.properties.example");
        }

        loaded = true;
    }

    /**
     * Get a configuration value. Environment variables take precedence over config file.
     * Environment variable format: WQ_<PROPERTY_NAME_UPPERCASE_WITH_UNDERSCORES>
     * e.g., wq.cookie -> WQ_COOKIE, smtp.host -> WQ_SMTP_HOST
     */
    public static String get(String key) {
        // Check environment variable first (convert key to env var format)
        String envKey = "WQ_" + key.toUpperCase().replace(".", "_");
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        return properties.getProperty(key);
    }

    /**
     * Get a configuration value with a default fallback.
     */
    public static String get(String key, String defaultValue) {
        String value = get(key);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

    /**
     * Get an integer configuration value.
     */
    public static int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            System.err.println("Invalid integer value for " + key + ": " + value);
            return defaultValue;
        }
    }

    /**
     * Get a double configuration value.
     */
    public static double getDouble(String key, double defaultValue) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            System.err.println("Invalid double value for " + key + ": " + value);
            return defaultValue;
        }
    }

    /**
     * Get a boolean configuration value.
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    // ==================== Convenience getters ====================

    public static String getCookie() {
        return get("wq.cookie", "");
    }

    public static String getSmtpHost() {
        return get("smtp.host", "smtp.gmail.com");
    }

    public static int getSmtpPort() {
        return getInt("smtp.port", 587);
    }

    public static String getSmtpUsername() {
        return get("smtp.username", "");
    }

    public static String getSmtpPassword() {
        return get("smtp.password", "");
    }

    public static String getEmailRecipient() {
        return get("email.recipient", "");
    }

    public static int getThreadPoolSize() {
        return getInt("thread.pool.size", 3);
    }

    public static double getMinCorrelation() {
        return getDouble("alpha.min.correlation", 0.7);
    }

    public static double getMinFitness() {
        return getDouble("alpha.min.fitness", 1.3);
    }

    // ==================== Regular Alpha Filter getters ====================

    public static String getRegularFilterRegion() {
        return get("filter.regular.region", "JPN");
    }

    public static String getRegularFilterDateFrom() {
        return get("filter.regular.date.from", "2026-01-29T00:00:00-05:00");
    }

    public static String getRegularFilterDateTo() {
        return get("filter.regular.date.to", "2026-02-07T00:00:00-05:00");
    }

    public static double getRegularFilterMinFitness() {
        return getDouble("filter.regular.min.fitness", 1.0);
    }

    public static int getRegularFilterLimit() {
        return getInt("filter.regular.limit", 5);
    }

    public static String getRegularFilterStatus() {
        return get("filter.regular.status", "UNSUBMITTED%1FIS_FAIL");
    }

    public static boolean getRegularFilterFavorite() {
        return getBoolean("filter.regular.favorite", false);
    }

    /**
     * Reloads configuration from file. Use after updating config via web interface.
     */
    public static void reload() {
        loaded = false;
        properties.clear();
        loadConfig();
    }
}
