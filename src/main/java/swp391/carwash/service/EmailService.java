package swp391.carwash.service;

public interface EmailService {

    void sendBookingReminderEmail(
            String to,
            String subject,
            String content
    );

}