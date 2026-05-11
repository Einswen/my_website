package com.einswen.blogapi.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Guestbook guestbook = new Guestbook();
    private final Cors cors = new Cors();

    public Guestbook getGuestbook() {
        return guestbook;
    }

    public Cors getCors() {
        return cors;
    }

    public static class Guestbook {
        private String dbPath = "backend/data/guestbook.sqlite3";
        private String ipHashSalt = "einswen-guestbook";
        private long cooldownSeconds = 3600;

        public String getDbPath() {
            return dbPath;
        }

        public void setDbPath(String dbPath) {
            this.dbPath = dbPath;
        }

        public String getIpHashSalt() {
            return ipHashSalt;
        }

        public void setIpHashSalt(String ipHashSalt) {
            this.ipHashSalt = ipHashSalt;
        }

        public long getCooldownSeconds() {
            return cooldownSeconds;
        }

        public void setCooldownSeconds(long cooldownSeconds) {
            this.cooldownSeconds = cooldownSeconds;
        }
    }

    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }
}
