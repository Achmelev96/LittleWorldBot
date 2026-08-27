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
        return properties.getProperty(key);
    }

    public static String getOrDefault(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
