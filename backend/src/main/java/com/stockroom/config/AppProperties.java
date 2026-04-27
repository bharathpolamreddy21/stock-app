package com.stockroom.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed binding for all app.* properties.
 * Every field maps 1:1 to an environment variable via application.properties.
 * This class is the single source of truth for what config values the app reads.
 *
 * K8s mapping:
 *   Non-sensitive fields → injected from ConfigMap via envFrom
 *   Sensitive fields     → injected from Secret via secretKeyRef
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String name = "StockRoom";
    private String env  = "development";
    private String version = "1.0.0";

    private Upload upload   = new Upload();
    private Jwt    jwt      = new Jwt();
    private Exchange exchange = new Exchange();
    private Pagination pagination = new Pagination();
    private Seed seed = new Seed();

    // ── Nested: Upload ────────────────────────────────────────────────────────
    public static class Upload {
        private String path = "/tmp/stockroom/uploads";   // ConfigMap: UPLOAD_PATH
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }

    // ── Nested: JWT ───────────────────────────────────────────────────────────
    public static class Jwt {
        private String secret = "changeme";               // Secret:    JWT_SECRET
        private long   expiryHours = 24;                  // ConfigMap: JWT_EXPIRY_HOURS
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public long getExpiryHours() { return expiryHours; }
        public void setExpiryHours(long expiryHours) { this.expiryHours = expiryHours; }
    }

    // ── Nested: Exchange ──────────────────────────────────────────────────────
    public static class Exchange {
        private String apiUrl  = "https://v6.exchangerate-api.com/v6"; // ConfigMap
        private String apiKey  = "";                                    // Secret
        private long   cacheTtlSeconds = 60;                           // ConfigMap
        private String defaultBase = "USD";                            // ConfigMap
        public String getApiUrl() { return apiUrl; }
        public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public long getCacheTtlSeconds() { return cacheTtlSeconds; }
        public void setCacheTtlSeconds(long cacheTtlSeconds) { this.cacheTtlSeconds = cacheTtlSeconds; }
        public String getDefaultBase() { return defaultBase; }
        public void setDefaultBase(String defaultBase) { this.defaultBase = defaultBase; }
    }

    // ── Nested: Pagination ────────────────────────────────────────────────────
    public static class Pagination {
        private int defaultSize = 12;   // ConfigMap: PAGINATION_DEFAULT_SIZE
        public int getDefaultSize() { return defaultSize; }
        public void setDefaultSize(int defaultSize) { this.defaultSize = defaultSize; }
    }

    // ── Nested: Seed ──────────────────────────────────────────────────────────
    public static class Seed {
        private String adminPassword = "admin123";  // Secret: SEED_ADMIN_PASSWORD
        public String getAdminPassword() { return adminPassword; }
        public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }
    }

    // ── Root getters/setters ──────────────────────────────────────────────────
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEnv()  { return env; }
    public void setEnv(String env) { this.env = env; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public Upload getUpload() { return upload; }
    public void setUpload(Upload upload) { this.upload = upload; }
    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }
    public Exchange getExchange() { return exchange; }
    public void setExchange(Exchange exchange) { this.exchange = exchange; }
    public Pagination getPagination() { return pagination; }
    public void setPagination(Pagination pagination) { this.pagination = pagination; }
    public Seed getSeed() { return seed; }
    public void setSeed(Seed seed) { this.seed = seed; }
}
