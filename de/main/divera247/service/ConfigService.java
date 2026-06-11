package de.main.divera247.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigService {
    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);
    private static final Path CONFIG_PATH = Path.of("config.properties");

    public AppConfig loadOrCreate() {
        try {
            if (Files.notExists(CONFIG_PATH)) {
                createDefaultConfig();
            }

            Properties properties = new Properties();
            try (InputStream inputStream = Files.newInputStream(CONFIG_PATH)) {
                properties.load(inputStream);
            }

            return new AppConfig(
                    properties.getProperty("language", "de").trim().toLowerCase(),
                    properties.getProperty("theme", "dark").trim().toLowerCase(),
                    readInt(properties, "map.defaultZoom", 13),
                    readInt(properties, "map.minZoom", 10),
                    readInt(properties, "map.maxZoom", 17)
            );
        } catch (IOException exception) {
            log.warn("Failed to load config.properties, using defaults.", exception);
            return new AppConfig("de", "dark", 13, 10, 17);
        }
    }

    private int readInt(Properties properties, String key, int fallback) {
        try {
            return Integer.parseInt(properties.getProperty(key, String.valueOf(fallback)).trim());
        } catch (NumberFormatException exception) {
            log.warn("Invalid config value for {}, using {}.", key, fallback);
            return fallback;
        }
    }

    private void createDefaultConfig() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("language", "de");
        properties.setProperty("theme", "dark");
        properties.setProperty("map.defaultZoom", "13");
        properties.setProperty("map.minZoom", "10");
        properties.setProperty("map.maxZoom", "17");

        try (OutputStream outputStream = Files.newOutputStream(CONFIG_PATH)) {
            properties.store(outputStream, "Divera247 Poller config");
        }
    }

    public static class AppConfig {
        private final String language;
        private final String theme;
        private final int defaultMapZoom;
        private final int minMapZoom;
        private final int maxMapZoom;

        private AppConfig(String language, String theme, int defaultMapZoom, int minMapZoom, int maxMapZoom) {
            this.language = language;
            this.theme = theme;
            this.defaultMapZoom = clamp(defaultMapZoom, 1, 19);
            this.minMapZoom = clamp(minMapZoom, 1, 19);
            this.maxMapZoom = clamp(maxMapZoom, 1, 19);
        }

        public boolean isGerman() {
            return !"en".equals(language);
        }

        public boolean isDarkMode() {
            return !"light".equals(theme);
        }

        public int defaultMapZoom() {
            return clamp(defaultMapZoom, minMapZoom(), maxMapZoom());
        }

        public int minMapZoom() {
            return Math.min(minMapZoom, maxMapZoom);
        }

        public int maxMapZoom() {
            return Math.max(minMapZoom, maxMapZoom);
        }

        private int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }
    }
}
