package swp391.carwash.service.otp;

import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// service/otp/MockOtpSender.java
@Service
@Profile("test")
public class MockOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(MockOtpSender.class);

    @Override
    public void send(String destination, String otp) {
        // Không gửi thật, chỉ log ra console
        log.info("[MOCK OTP] destination={} otp={}", destination, otp);
    }
}