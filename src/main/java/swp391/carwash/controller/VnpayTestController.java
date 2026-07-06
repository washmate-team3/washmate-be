package swp391.carwash.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import swp391.carwash.service.VnpayService;
import java.util.Map;
import java.net.URI;
import org.springframework.http.ResponseEntity;

/**
 * Endpoint hỗ trợ test VNPAY return flow ở môi trường local.
 * Chỉ được bật với profile "local" — KHÔNG expose ở production.
 */
@RestController
@Profile("local")
public class VnpayTestController {
    private static final Logger log = LoggerFactory.getLogger(VnpayTestController.class);

    private final VnpayService vnpayService;

    public VnpayTestController(VnpayService vnpayService) {
        this.vnpayService = vnpayService;
    }

    @GetMapping("/api/test-vnpay-return")
    public ResponseEntity<String> testReturn(@RequestParam Map<String, String> parameters) {
        try {
            URI redirect = vnpayService.buildReturnRedirect(parameters);
            return ResponseEntity.ok("Success, would redirect to: " + redirect.toString());
        } catch (Exception e) {
            log.error("VNPAY test return failed", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
