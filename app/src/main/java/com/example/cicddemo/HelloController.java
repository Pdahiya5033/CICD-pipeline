package com.example.cicddemo;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    @GetMapping("/api/health")
    public ResponseEntity<Map<String,String>> health() {
        return ResponseEntity.ok(Map.of("status","UP"));
    }

    @GetMapping("/api/hello")
    public ResponseEntity<Map<String,String>> hello() {
        return ResponseEntity.ok(Map.of(
            "application","secure-cicd-demo",
            "message","Spring Boot app running on Tomcat"
        ));
    }
}
