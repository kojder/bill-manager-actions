package com.example.bill_manager.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

  /** Liveness probe — confirms the application process is running and accepting HTTP requests. */
  @GetMapping("/health")
  public ResponseEntity<HealthResponse> health() {
    return ResponseEntity.ok(new HealthResponse("UP"));
  }

  /** Readiness probe — confirms the application is ready to handle traffic. */
  @GetMapping("/health/ready")
  public ResponseEntity<HealthResponse> ready() {
    return ResponseEntity.ok(new HealthResponse("READY"));
  }
}
