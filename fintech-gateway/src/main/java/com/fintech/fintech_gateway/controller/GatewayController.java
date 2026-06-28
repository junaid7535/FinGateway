package com.fintech.fintech_gateway.controller;

import com.fintech.fintech_gateway.proxy.ProxyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.stream.Collectors;

@RestController
public class GatewayController {

    private final ProxyService proxyService;

    public GatewayController(ProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @RequestMapping("/**")
    public ResponseEntity<byte[]> handle(HttpServletRequest request)
            throws IOException {

        String path = request.getRequestURI();

        // Never forward internal paths to backend
        if (path.contains("/favicon.ico") ||
                path.contains("/swagger-ui") ||
                path.contains("/v3/api-docs") ||
                path.startsWith("/feedback") ||
                path.startsWith("/health") ||
                path.startsWith("/ping") ||
                path.startsWith("/actuator")) {
            return ResponseEntity.ok(new byte[0]);
        }

        String query = request.getQueryString();
        if (query != null) path = path + "?" + query;

        String body = request.getReader()
                .lines()
                .collect(Collectors.joining());

        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        ResponseEntity<String> upstream = proxyService.forward(path, method, body);

        String responseBody = upstream.getBody() != null ? upstream.getBody() : "";
        byte[] bytes = responseBody.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        // Return byte[] with explicit Content-Length
        // This prevents Spring adding Transfer-Encoding: chunked on top
        return ResponseEntity
                .status(upstream.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .contentLength(bytes.length)
                .body(bytes);
    }
}