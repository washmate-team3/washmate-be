package swp391.carwash.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "washmate.payment.vnpay")
public class VnpayProperties {
    private boolean enabled;
    private String tmnCode;
    private String hashSecret;
    private String payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private String returnUrl;
    private String frontendResultUrl = "http://localhost:3000/payment/vnpay-return";
    private long timeoutMinutes = 15;

    /**
     * Cho phép return URL settle payment thay IPN (chỉ dùng khi test local,
     * vì VNPAY IPN không gọi được tới localhost). Mặc định TẮT — production
     * chỉ settle qua IPN đã verify chữ ký.
     */
    private boolean returnFallbackEnabled = false;
}
