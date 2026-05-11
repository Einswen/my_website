package com.einswen.blogapi.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Guestbook guestbook = new Guestbook();
    private final Cors cors = new Cors();
    private final Pet pet = new Pet();
    private final Ai ai = new Ai();

    public Guestbook getGuestbook() {
        return guestbook;
    }

    public Cors getCors() {
        return cors;
    }

    public Pet getPet() {
        return pet;
    }

    public Ai getAi() {
        return ai;
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

    public static class Pet {
        private int satietyDecayMinutesPerPoint = 35;

        public int getSatietyDecayMinutesPerPoint() {
            return satietyDecayMinutesPerPoint;
        }

        public void setSatietyDecayMinutesPerPoint(int satietyDecayMinutesPerPoint) {
            this.satietyDecayMinutesPerPoint = satietyDecayMinutesPerPoint;
        }
    }

    public static class Ai {
        private final Deepseek deepseek = new Deepseek();

        public Deepseek getDeepseek() {
            return deepseek;
        }
    }

    public static class Deepseek {
        private String apiKey = "";
        private String baseUrl = "https://api.deepseek.com";
        private String model = "deepseek-chat";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }
}
