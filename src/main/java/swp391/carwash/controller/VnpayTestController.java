package swp391.carwash.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import swp391.carwash.service.VnpayService;
import java.util.Map;
import java.net.URI;
import org.springframework.http.ResponseEntity;

@RestController
public class VnpayTestController {

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
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage() + "\n" + e.toString());
        }
    }
}
