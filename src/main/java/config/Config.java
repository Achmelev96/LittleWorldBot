package config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public final class Config {
    private static final Properties properties = new Properties();

    static {
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            properties.load(fis);
        } catch (IOException e) {
            throw new RuntimeException("load properties error", e);
        }
    }

    public static String get(String key) {
        String environmentValue = System.getenv(key);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue;
        }
        return properties.getProperty(key);
    }

    public static String getOrDefault(String key, String defaultValue) {
        String value = get(key);
        return value == null ? defaultValue : value;
    }

    public static long getLongOrDefault(String key, long defaultValue) {
        String value = get(key);
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException error) {
            throw new IllegalStateException("Config property " + key + " must be a valid number", error);
        }
    }
}
