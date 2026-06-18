package swp391.carwash.service.otp;

public interface OtpSender {
 /**
     * Gửi OTP đến địa chỉ đích (email hoặc phone).
     * @param destination email hoặc phone
     * @param otp         mã OTP gốc (chưa hash)
     */
    void send(String destination, String otp);
}
