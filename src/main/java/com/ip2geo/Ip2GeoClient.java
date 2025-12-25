package com.ip2geo;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class Ip2GeoClient {

    private static final String BASE_URL = "https://api.ip2geoapi.com/ip";

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public Ip2GeoClient(String apiKey) {
        this(apiKey, 60);
    }

    public Ip2GeoClient(String apiKey, int timeoutSeconds) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Transport-only lookup
     * - Returns Map for JSON (default)
     * - Returns String for XML / YAML / JSONP
     * - Throws ONLY on transport failure
     */
    public Object lookup(String ip, String format, String callback) {
        if (callback != null && !"jsonp".equals(format)) {
            throw new IllegalArgumentException(
                    "callback can only be used when format is 'jsonp'"
            );
        }

        try {
            String url = buildUrl(ip, format, callback);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // JSON default → decode but DO NOT judge
            if (format == null || "json".equals(format)) {
                return objectMapper.readValue(response.body(), Map.class);
            }

            // XML / YAML / JSONP → raw response
            return response.body();

        } catch (IOException | InterruptedException e) {
            // TRUE transport failure only
            throw new RuntimeException("Unable to reach Ip2Geo API", e);
        }
    }

    private String buildUrl(String ip, String format, String callback) {
        StringBuilder sb = new StringBuilder();

        if (ip != null && !ip.isEmpty()) {
            sb.append(BASE_URL).append("/").append(encode(ip));
        } else {
            sb.append(BASE_URL);
        }

        Map<String, String> params = new HashMap<>();

        if (apiKey != null && !apiKey.isEmpty()) {
            params.put("key", apiKey);
        }
        if (format != null) {
            params.put("format", format);
        }
        if (callback != null) {
            params.put("callback", callback);
        }

        if (!params.isEmpty()) {
            sb.append("?");
            boolean first = true;
            for (Map.Entry<String, String> e : params.entrySet()) {
                if (!first) sb.append("&");
                first = false;
                sb.append(encode(e.getKey()))
                  .append("=")
                  .append(encode(e.getValue()));
            }
        }

        return sb.toString();
    }

    private String encode(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }
}
