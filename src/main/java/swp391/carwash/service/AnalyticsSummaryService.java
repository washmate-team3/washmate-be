package swp391.carwash.service;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.dto.AnalyticsSummaryResponse;
import swp391.carwash.enums.BookingStatus;
import swp391.carwash.enums.InvoiceStatus;
import swp391.carwash.enums.PaymentStatus;
import swp391.carwash.enums.UserStatus;
import swp391.carwash.repository.AppUserRepository;
import swp391.carwash.repository.BookingRepository;
import swp391.carwash.repository.InvoiceRepository;
import swp391.carwash.repository.PaymentRepository;

@Service
@RequiredArgsConstructor
public class AnalyticsSummaryService {
    private final AppUserRepository appUserRepository;
    private final BookingRepository bookingRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse getSummary() {
        BigDecimal paidRevenue = invoiceRepository.sumTotalAmountByStatus(InvoiceStatus.PAID);
        return new AnalyticsSummaryResponse(
                appUserRepository.count(),
                appUserRepository.countByStatus(UserStatus.ACTIVE),
                bookingRepository.count(),
                bookingRepository.countByStatus(BookingStatus.PENDING),
                bookingRepository.countByStatus(BookingStatus.COMPLETED),
                bookingRepository.countByStatus(BookingStatus.REJECTED),
                invoiceRepository.count(),
                invoiceRepository.countByStatus(InvoiceStatus.PAID),
                paidRevenue,
                paymentRepository.countByStatus(PaymentStatus.PENDING),
                paymentRepository.countByStatus(PaymentStatus.PAID)
        );
    }
}
