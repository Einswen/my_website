package com.einswen.blogapi.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.sqlite.SQLiteDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceConfig {

    private final AppProperties appProperties;

    public DataSourceConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Bean
    public DataSource dataSource() throws IOException {
        Path dbPath = Path.of(appProperties.getGuestbook().getDbPath()).toAbsolutePath().normalize();
        Path parent = dbPath.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + dbPath);
        return dataSource;
    }
}
