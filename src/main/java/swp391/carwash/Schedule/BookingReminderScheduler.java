package swp391.carwash.Schedule;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import swp391.carwash.entity.Booking;
import swp391.carwash.repository.BookingRepository;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BookingReminderScheduler {


    private final BookingRepository bookingRepository;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String mailFrom;


    @Scheduled(fixedRate = 5000)
    @Transactional
    public void sendReminder(){
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime start = now.plusMinutes(59);
        OffsetDateTime end = now.plusMinutes(61);

        List<Booking> bookings = bookingRepository.findBookingNeedReminder(
                start.toLocalDateTime(),
                start.toLocalDateTime(),
                end.toLocalDateTime());

        for(Booking booking : bookings){
            try {
                // 2. Viết logic tạo và gửi Gmail thực tế
                org.springframework.mail.SimpleMailMessage message = new org.springframework.mail.SimpleMailMessage();
                message.setFrom(mailFrom);
                message.setTo(booking.getUser().getEmail());
                message.setSubject("Nhắc nhở lịch rửa xe của bạn");
                message.setText("Chào bạn, lịch đặt của bạn (Mã: " + booking.getId() + ") sẽ bắt đầu trong 1 tiếng nữa!");

                mailSender.send(message); // Thực hiện gửi mail

                System.out.println("Đã gửi Gmail nhắc nhở thành công cho booking: " + booking.getId());

                // Đánh dấu đã gửi để không bị gửi lặp lại
                booking.setReminderSent(true);
                bookingRepository.save(booking);

            } catch (Exception e) {
                System.err.println("Lỗi gửi mail cho booking " + booking.getId() + ": " + e.getMessage());
            }
        }
    }
}