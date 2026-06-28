package com.fintech.fintech_gateway.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
@Order(1) // Runs before RequestLoggingFilter
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Prevent clickjacking
        response.setHeader("X-Frame-Options", "DENY");

        // Prevent MIME sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");

        // Force HTTPS in production
        response.setHeader("Strict-Transport-Security",
                "max-age=31536000; includeSubDomains");

        // Prevent XSS in older browsers
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // Control what browser can load
        response.setHeader("Content-Security-Policy",
                "default-src 'self'");

        // Don't send referrer to other domains
        response.setHeader("Referrer-Policy", "no-referrer");

        filterChain.doFilter(request, response);
    }
}