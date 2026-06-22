package swp391.carwash.service.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import swp391.carwash.entity.Invoice;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceEmailSender {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    public void sendInvoiceEmail(Invoice invoice, String toEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject("Hóa đơn thanh toán - " + invoice.getInvoiceCode());
            helper.setText(buildHtml(invoice), true);

            mailSender.send(message);
            log.info("Invoice email sent successfully to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send invoice email to: {}", toEmail, e);
            throw new RuntimeException("Gửi email hóa đơn thất bại", e);
        }
    }

    private String buildHtml(Invoice invoice) {
        String garageName = invoice.getGarage() != null ? invoice.getGarage().getName() : "WashMate Garage";
        String serviceName = invoice.getBooking() != null && invoice.getBooking().getService() != null 
                ? invoice.getBooking().getService().getName() : "Dịch vụ rửa xe";
        String dateStr = invoice.getIssuedAt() != null 
                ? invoice.getIssuedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "";

        return """
            <div style="font-family:sans-serif;max-width:600px;margin:20px auto;
                        padding:30px;border:1px solid #e5e7eb;border-radius:12px">
              <div style="text-align:center;margin-bottom:20px;">
                  <h1 style="margin:0;color:#2563eb;">WASHMATE</h1>
                  <p style="color:#6b7280;margin:5px 0;">Hóa đơn thanh toán dịch vụ</p>
              </div>
              <hr style="border:none;border-top:1px solid #e5e7eb;margin:20px 0;">
              
              <p style="margin:0 0 10px;"><strong>Mã hóa đơn:</strong> %s</p>
              <p style="margin:0 0 10px;"><strong>Ngày thanh toán:</strong> %s</p>
              <p style="margin:0 0 10px;"><strong>Garage:</strong> %s</p>
              <p style="margin:0 0 20px;"><strong>Dịch vụ:</strong> %s</p>
              
              <table style="width:100%%;border-collapse:collapse;margin-bottom:20px;">
                  <tr style="background-color:#f3f4f6;">
                      <th style="padding:10px;text-align:left;border-bottom:1px solid #d1d5db;">Mô tả</th>
                      <th style="padding:10px;text-align:right;border-bottom:1px solid #d1d5db;">Thành tiền</th>
                  </tr>
                  <tr>
                      <td style="padding:10px;border-bottom:1px solid #e5e7eb;">Giá dịch vụ</td>
                      <td style="padding:10px;text-align:right;border-bottom:1px solid #e5e7eb;">%s VND</td>
                  </tr>
                  <tr>
                      <td style="padding:10px;border-bottom:1px solid #e5e7eb;">Giảm giá</td>
                      <td style="padding:10px;text-align:right;border-bottom:1px solid #e5e7eb;">-%s VND</td>
                  </tr>
                  <tr style="font-weight:bold;font-size:16px;">
                      <td style="padding:10px;">Tổng cộng</td>
                      <td style="padding:10px;text-align:right;color:#16a34a;">%s VND</td>
                  </tr>
              </table>
              
              <p style="color:#4b5563;font-size:14px;text-align:center;margin-top:30px;">
                Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi.<br>
                Nếu có thắc mắc, vui lòng liên hệ hotline hỗ trợ.
              </p>
            </div>
            """.formatted(
                invoice.getInvoiceCode(),
                dateStr,
                garageName,
                serviceName,
                String.format("%,.0f", invoice.getSubtotal()),
                String.format("%,.0f", invoice.getDiscount()),
                String.format("%,.0f", invoice.getTotalAmount())
            );
    }
}
