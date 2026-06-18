package swp391.carwash.service.otp;

import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.MessagingException;

@Service
@Primary
@Profile("!test")   // không dùng khi chạy test
public class EmailOtpSender implements OtpSender {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    public EmailOtpSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    @Override
    public void send(String email, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(from);
            helper.setTo(email);
            helper.setSubject("Mã xác thực WashMate — " + otp);
            helper.setText(buildHtml(otp), true);

            mailSender.send(message);

        } catch (MessagingException e) {
            // Log lỗi nhưng không throw ra ngoài vì đang async
            // User đã nhận response "đã gửi" rồi
            throw new RuntimeException("Gửi email OTP thất bại tới: " + email, e);
        }
    }

    private String buildHtml(String otp) {
        return """
            <div style="font-family:sans-serif;max-width:480px;margin:40px auto;
                        padding:32px;border:1px solid #e5e7eb;border-radius:12px">
              <h2 style="margin:0 0 8px;color:#111">Mã xác thực WashMate</h2>
              <p style="color:#555;margin:0 0 24px">
                Nhập mã dưới đây để xác thực. Mã có hiệu lực trong <strong>5 phút</strong>.
              </p>
              <div style="background:#f4f4f5;border-radius:8px;padding:20px;
                          text-align:center;font-size:40px;font-weight:700;
                          letter-spacing:10px;color:#111">
                %s
              </div>
              <p style="color:#999;font-size:12px;margin-top:20px">
                Nếu bạn không yêu cầu mã này, hãy bỏ qua email này.
                Không chia sẻ mã với bất kỳ ai.
              </p>
            </div>
            """.formatted(otp);
    }
}