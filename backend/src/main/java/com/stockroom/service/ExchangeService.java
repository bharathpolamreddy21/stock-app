package com.stockroom.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockroom.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Fetches currency exchange rates.
 *
 * Behaviour:
 *   - EXCHANGE_API_KEY is set   → calls ExchangeRate-API (free tier, 1500 req/month)
 *   - EXCHANGE_API_KEY is empty → returns hardcoded mock rates (works out of the box)
 *
 * Results are cached in-memory for EXCHANGE_CACHE_TTL_SECONDS (ConfigMap).
 * This makes the TTL observable: change it in the ConfigMap, rolling restart,
 * watch how quickly the cached value expires.
 *
 * K8s teaching point:
 *   EXCHANGE_API_KEY comes from a K8s Secret.
 *   EXCHANGE_API_URL and EXCHANGE_CACHE_TTL_SECONDS come from ConfigMap.
 */
@Service
public class ExchangeService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeService.class);

    private final AppProperties props;
    private final HttpClient    httpClient;
    private final ObjectMapper  objectMapper;

    // Simple in-memory cache
    private Map<String, Object> cachedRates;
    private Instant             cachedAt;

    public ExchangeService(AppProperties props) {
        this.props        = props;
        this.httpClient   = HttpClient.newBuilder()
                                      .connectTimeout(Duration.ofSeconds(5))
                                      .build();
        this.objectMapper = new ObjectMapper();
    }

    public Map<String, Object> getRates(String base) {
        // Return cached value if still fresh
        if (cachedRates != null && cachedAt != null) {
            long ageSeconds = Duration.between(cachedAt, Instant.now()).getSeconds();
            if (ageSeconds < props.getExchange().getCacheTtlSeconds()) {
                log.debug("Returning cached exchange rates (age={}s)", ageSeconds);
                return cachedRates;
            }
        }

        Map<String, Object> rates;
        String              apiKey = props.getExchange().getApiKey();

        if (apiKey == null || apiKey.isBlank()) {
            log.info("EXCHANGE_API_KEY not set — using mock rates");
            rates = mockRates(base);
        } else {
            rates = fetchLiveRates(base, apiKey);
        }

        cachedRates = rates;
        cachedAt    = Instant.now();
        return rates;
    }

    private Map<String, Object> fetchLiveRates(String base, String apiKey) {
        String url = props.getExchange().getApiUrl() + "/" + apiKey + "/latest/" + base;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            JsonNode root = objectMapper.readTree(response.body());

            if (!"success".equals(root.path("result").asText())) {
                log.warn("Exchange API returned non-success, falling back to mock");
                return mockRates(base);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("base",     base);
            result.put("source",   "live");
            result.put("fetchedAt", Instant.now().toString());
            result.put("cacheTtlSeconds", props.getExchange().getCacheTtlSeconds());

            Map<String, Double> ratesMap = new HashMap<>();
            root.path("conversion_rates").fields()
                .forEachRemaining(e -> ratesMap.put(e.getKey(), e.getValue().asDouble()));
            result.put("rates", ratesMap);

            log.info("Fetched live exchange rates for base={}", base);
            return result;

        } catch (Exception e) {
            log.warn("Exchange API call failed ({}), falling back to mock", e.getMessage());
            return mockRates(base);
        }
    }

    /** Mock rates — always available without any API key */
    private Map<String, Object> mockRates(String base) {
        Map<String, Double> rates = new HashMap<>();
        // Approximate rates relative to USD
        Map<String, Double> usdBase = Map.of(
            "USD", 1.0,    "INR", 83.5,  "EUR", 0.92,
            "GBP", 0.79,   "JPY", 149.5, "AUD", 1.53,
            "CAD", 1.36,   "SGD", 1.34,  "AED", 3.67
        );
        double baseRate = usdBase.getOrDefault(base.toUpperCase(), 1.0);
        usdBase.forEach((cur, rate) -> rates.put(cur, Math.round((rate / baseRate) * 10000.0) / 10000.0));

        Map<String, Object> result = new HashMap<>();
        result.put("base",            base.toUpperCase());
        result.put("source",          "mock");
        result.put("fetchedAt",       Instant.now().toString());
        result.put("cacheTtlSeconds", props.getExchange().getCacheTtlSeconds());
        result.put("rates",           rates);
        return result;
    }
}
