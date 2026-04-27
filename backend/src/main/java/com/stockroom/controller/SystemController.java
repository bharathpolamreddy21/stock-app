package com.stockroom.controller;

import com.stockroom.config.AppProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * System endpoints used by Kubernetes and ops tooling.
 *
 * /health  — liveness probe   : always 200 if the JVM is alive
 * /ready   — readiness probe  : 503 if DB is unreachable or manually toggled
 * /api/info— runtime metadata : great for verifying rolling updates landed
 *
 * K8s teaching exercise:
 *   POST /api/info/simulate-unready  — flips the ready flag to false
 *   Watch: kubectl get pods -w  → pod transitions to 0/1 READY
 *   The Service immediately stops routing traffic to that pod.
 *   POST again to restore.
 */
@RestController
public class SystemController {

    private final AppProperties props;
    private final JdbcTemplate  jdbcTemplate;
    private final long          startMs = System.currentTimeMillis();

    // Manually togglable readiness — for K8s probe demo
    private static final AtomicBoolean READY = new AtomicBoolean(true);

    public SystemController(AppProperties props, JdbcTemplate jdbcTemplate) {
        this.props        = props;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Liveness — is the process alive? */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return map(
            "status",         "ok",
            "timestamp",      Instant.now().toString(),
            "uptime_seconds", (System.currentTimeMillis() - startMs) / 1000.0
        );
    }

    /** Readiness — is the app ready to serve traffic? */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> ready() {
        if (!READY.get()) {
            return ResponseEntity.status(503).body(map(
                "status", "not_ready",
                "reason", "manually toggled via /api/info/simulate-unready"
            ));
        }

        boolean dbOk = checkDatabase();
        if (!dbOk) {
            return ResponseEntity.status(503).body(map(
                "status", "not_ready",
                "reason", "database unreachable"
            ));
        }

        return ResponseEntity.ok(map(
            "status", "ready",
            "db",     "ok"
        ));
    }

    /** Pod info — useful for verifying rolling updates took effect */
    @GetMapping("/api/info")
    public Map<String, Object> info() {
        return map(
            "appName",        props.getName(),
            "appEnv",         props.getEnv(),
            "appVersion",     props.getVersion(),
            "hostname",       hostname(),
            "uptimeSeconds",  (System.currentTimeMillis() - startMs) / 1000.0,
            "timestamp",      Instant.now().toString(),
            "ready",          READY.get(),
            "javaVersion",    System.getProperty("java.version")
        );
    }

    /** Toggle readiness — for K8s probe demo */
    @PostMapping("/api/info/simulate-unready")
    public Map<String, Object> simulateUnready() {
        READY.set(!READY.get());
        return map("ready", READY.get(), "message",
                   READY.get() ? "App is now READY" : "App is now NOT READY — watch kubectl get pods");
    }

    private boolean checkDatabase() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String hostname() {
        try { return InetAddress.getLocalHost().getHostName(); }
        catch (Exception e) { return "unknown"; }
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> map(Object... kv) {
        Map<Object, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length - 1; i += 2) m.put(kv[i], kv[i + 1]);
        return (Map<K, V>) m;
    }
}
