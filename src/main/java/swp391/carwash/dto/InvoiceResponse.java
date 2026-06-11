package swp391.carwash.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import swp391.carwash.entity.Invoice;

public record InvoiceResponse(
        Integer id,
        String invoiceCode,
        Integer bookingId,
        Integer paymentId,
        Integer garageId,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal penaltyTotal,
        BigDecimal totalAmount,
        String status,
        OffsetDateTime issuedAt,
        OffsetDateTime paidAt
) {
    public static InvoiceResponse from(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceCode(),
                invoice.getBooking().getId(),
                invoice.getPayment() == null ? null : invoice.getPayment().getId(),
                invoice.getGarage().getId(),
                invoice.getSubtotal(),
                invoice.getDiscount(),
                invoice.getPenaltyTotal(),
                invoice.getTotalAmount(),
                invoice.getStatus().name(),
                invoice.getIssuedAt(),
                invoice.getPaidAt()
        );
    }
}
