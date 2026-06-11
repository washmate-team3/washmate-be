package swp391.carwash.db;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MigrationContractTest {
    private static final Path MIGRATION_DIR = Path.of("src/main/resources/db/migration");

    @Test
    void paymentInvoiceMigrationDefinesDbSideGuards() throws IOException {
        String migration = Files.readString(MIGRATION_DIR.resolve("V4__align_payment_invoice_loyalty_source_of_truth.sql"));

        assertTrue(migration.contains("validate_payment_paid_amount"));
        assertTrue(migration.contains("Payment amount % does not match booking final amount %"));
        assertTrue(migration.contains("ELSIF NEW.status = 'REFUNDED'"));
        assertTrue(migration.contains("sync_payment_from_transaction"));
    }

    @Test
    void loyaltyTriggerMigrationIsTheOnlyRollbackSourceOfTruth() throws IOException {
        String migration = Files.readString(MIGRATION_DIR.resolve("V3__sync_booking_payment_loyalty_triggers.sql"));
        String paymentService = Files.readString(Path.of("src/main/java/swp391/carwash/service/PaymentService.java"));

        assertTrue(migration.contains("process_loyalty_for_completed_paid_booking"));
        assertTrue(migration.contains("trg_sync_loyalty_on_booking_completed"));
        assertTrue(migration.contains("rollback_loyalty_for_refunded_booking"));
        assertFalse(paymentService.contains("rollbackEarnedPointsForBooking"));
        assertFalse(paymentService.contains("LoyaltyRollbackService"));
    }
}
