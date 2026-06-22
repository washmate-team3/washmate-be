package swp391.carwash.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import swp391.carwash.service.EmailService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class EmailTestController {

    private final EmailService emailService;

    @GetMapping("/mail")
    public String sendMail() {

        System.out.println("before");
        emailService.sendBookingReminderEmail(
                "lehoang48692005@gmail.com",
                "Test Reminder",
                "Hoàng ăn cứt"
        );

        System.out.println("After");
        return "Mail Sent";
    }
}
