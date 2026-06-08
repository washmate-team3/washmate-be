package swp391.carwash.controller;

import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
                "application", "car-wash-management",
                "status", "UP",
                "message", "WashMate backend is running",
                "timestamp", OffsetDateTime.now().toString()
        );
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return root();
    }
}