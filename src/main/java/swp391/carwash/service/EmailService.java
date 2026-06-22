package swp391.carwash.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendBookingSuccessEmail(String toEmail, String customerName, String bookingId, Double amount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("✨ [WashMate] Xác nhận thanh toán đơn đặt lịch thành công!");

            // Tạo nội dung Email bằng HTML nhìn cho chuyên nghiệp
            String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; padding: 20px; border-radius: 8px;'>"
                    + "<h2 style='color: #2e7d32;'>Thanh toán thành công!</h2>"
                    + "<p>Chào <b>" + customerName + "</b>,</p>"
                    + "<p>Cảm ơn bạn đã tin tưởng và sử dụng dịch vụ tại <b>WashMate</b>. Lịch đặt của bạn đã được hệ thống xác nhận thanh toán hoàn tất.</p>"
                    + "<hr style='border: 0; border-top: 1px solid #eee;'>"
                    + "<p><b>Thông tin đơn hàng:</b></p>"
                    + "<ul>"
                    + "<li><b>Mã Booking:</b> #" + bookingId + "</li>"
                    + "<li><b>Số tiền đã thanh toán:</b> " + String.format("%,.0f", amount) + " VND</li>"
                    + "<li><b>Trạng thái:</b> Đã thanh toán</li>"
                    + "</ul>"
                    + "<hr style='border: 0; border-top: 1px solid #eee;'>"
                    + "<p style='font-size: 12px; color: #757575;'>Đây là email tự động, vui lòng không phản hồi thư này.</p>"
                    + "</div>";

            helper.setText(htmlContent, true); // Tham số true kích hoạt chế độ render HTML

            mailSender.send(message);
            System.out.println("👉 Email thông báo booking đã được gửi tới: " + toEmail);

        } catch (MessagingException e) {
            // Log lỗi ra nếu không gửi được mail
            System.err.println("❌ Lỗi xảy ra khi gửi email: " + e.getMessage());
        }
    }
}