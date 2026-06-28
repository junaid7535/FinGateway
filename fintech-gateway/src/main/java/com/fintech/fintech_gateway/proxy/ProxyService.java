package com.fintech.fintech_gateway.proxy;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;

@Service
public class ProxyService {

    private final RestClient restClient;

    public ProxyService(RestClient restClient) {
        this.restClient = restClient;
    }

    public ResponseEntity<String> forward(String path, HttpMethod method, String body) {
        try {
            if (method == HttpMethod.GET) {
                return restClient.get()
                        .uri(path)
                        .retrieve()
                        .toEntity(String.class);
            } else {
                return restClient.method(method)
                        .uri(path)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .toEntity(String.class);
            }
        } catch (Exception e) {
            System.err.println("Backend unavailable: " + e.getMessage());
            return ResponseEntity.status(503)
                    .body("{\"error\":\"Service temporarily unavailable\",\"message\":\"Please try again later\"}");
        }
    }
}