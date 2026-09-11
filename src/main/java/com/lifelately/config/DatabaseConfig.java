package com.lifelately.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record DatabaseConfig(String url, String username, String password) {
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/life_lately?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=3000";

    public static DatabaseConfig load() {
        Properties properties = new Properties();
        Path localConfig = Path.of("config.properties");

        if (Files.exists(localConfig)) {
            try (InputStream input = Files.newInputStream(localConfig)) {
                properties.load(input);
            } catch (IOException exception) {
                throw new IllegalStateException("Could not read config.properties", exception);
            }
        }

        return new DatabaseConfig(
                properties.getProperty("db.url", DEFAULT_URL).trim(),
                properties.getProperty("db.username", "root").trim(),
                properties.getProperty("db.password", "")
        );
    }

    public String serverUrl() {
        int queryStart = url.indexOf('?');
        String query = queryStart >= 0 ? url.substring(queryStart) : "";
        String withoutQuery = queryStart >= 0 ? url.substring(0, queryStart) : url;
        int finalSlash = withoutQuery.lastIndexOf('/');
        return withoutQuery.substring(0, finalSlash + 1) + query;
    }
}
