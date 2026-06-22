package swp391.carwash.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import swp391.carwash.event.InvoiceCreatedEvent;
import swp391.carwash.repository.InvoiceRepository;
import swp391.carwash.service.mail.InvoiceEmailSender;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceEventListener {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceEmailSender invoiceEmailSender;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleInvoiceCreatedEvent(InvoiceCreatedEvent event) {
        log.info("Handling InvoiceCreatedEvent for invoice ID: {}", event.invoiceId());
        try {
            invoiceRepository.findDetailedById(event.invoiceId()).ifPresentOrElse(invoice -> {
                String toEmail = invoice.getBooking().getUser().getEmail();
                if (toEmail != null && !toEmail.isBlank()) {
                    invoiceEmailSender.sendInvoiceEmail(invoice, toEmail);
                } else {
                    log.warn("Cannot send invoice email. User email is null or empty for invoice ID: {}", invoice.getId());
                }
            }, () -> log.error("Invoice not found for ID: {}", event.invoiceId()));
        } catch (Exception e) {
            log.error("Error processing InvoiceCreatedEvent for invoice ID: {}", event.invoiceId(), e);
        }
    }
}
