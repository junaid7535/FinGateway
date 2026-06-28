package com.fintech.fintech_gateway.controller;

import com.fintech.fintech_gateway.feedback.FeedbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping("/false-positive")
    public ResponseEntity<Map<String, String>> reportFalsePositive(
            @RequestBody FeedbackRequest request) {
        feedbackService.handleFalsePositive(request.getIp());
        return ResponseEntity.ok(Map.of(
                "status", "resolved",
                "ip", request.getIp(),
                "message", "IP trust tier reset to MEDIUM"
        ));
    }

    // Request body class
    public static class FeedbackRequest {
        private String ip;
        private String reason;

        public String getIp() { return ip; }
        public void setIp(String ip) { this.ip = ip; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}