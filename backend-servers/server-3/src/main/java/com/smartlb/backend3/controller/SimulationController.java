package com.smartlb.backend3.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
public class SimulationController {

    @GetMapping("/**")
    public Map<String, Object> handleMockRequest(@RequestHeader Map<String, String> headers, HttpServletResponse response) {
        // Set dynamic headers to simulate loaded response
        response.setHeader("X-Backend-Server", "Server 3");
        
        Map<String, Object> body = new HashMap<>();
        body.put("service", "SmartLB Java Backend Target Simulator");
        body.put("instance", 3);
        body.put("port", 5003);
        body.put("timestamp", Instant.now().toString());
        body.put("status", "Healthy");
        body.put("headers_received", headers);
        
        return body;
    }
}