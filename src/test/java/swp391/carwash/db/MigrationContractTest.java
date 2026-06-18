package swp391.carwash.db;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Migrations were squashed/removed")
class MigrationContractTest {
    private static final Path MIGRATION_DIR = Path.of("src/main/resources/db/migration");

    @Test
    void paymentInvoiceMigrationDefinesDbSideGuards() throws IOException {
        String migration = Files
                .readString(MIGRATION_DIR.resolve("V4__align_payment_invoice_loyalty_source_of_truth.sql"));

        assertTrue(migration.contains("validate_payment_paid_amount"));
        assertTrue(migration.contains("Payment amount % does not match booking final amount %"));
    }

    @Test
    void businessWorkflowTriggersAreRemovedAfterApplicationServicesTakeOwnership() throws IOException {
        String migration = Files
                .readString(MIGRATION_DIR.resolve("V6__move_business_logic_to_application_services.sql"));
        String paymentService = Files.readString(Path.of("src/main/java/swp391/carwash/service/PaymentService.java"));
        String loyaltyService = Files.readString(Path.of("src/main/java/swp391/carwash/service/LoyaltyService.java"));

        assertTrue(migration.contains("DROP TRIGGER IF EXISTS trg_create_payment_for_booking"));
        assertTrue(migration.contains("DROP TRIGGER IF EXISTS trg_sync_payment_from_transaction_insert"));
        assertTrue(migration.contains("DROP TRIGGER IF EXISTS trg_create_invoice_when_payment_paid"));
        assertTrue(migration.contains("DROP TRIGGER IF EXISTS trg_sync_loyalty_on_booking_completed"));
        assertTrue(paymentService.contains("rollbackEarnedPointsForBooking"));
        assertTrue(loyaltyService.contains("existsByBookingIdAndTransactionType"));
    }

    @Test
    void otpMigrationStoresHashedCodesAndLimitsDuplicatePhoneRows() throws IOException {
        String migration = Files.readString(MIGRATION_DIR.resolve("V7__harden_otp_storage.sql"));
        String otpEntity = Files.readString(Path.of("src/main/java/swp391/carwash/entity/OtpCode.java"));

        assertTrue(migration.contains("ALTER COLUMN code TYPE VARCHAR(255)"));
        assertTrue(migration.contains("failed_attempts"));
        assertTrue(migration.contains("CREATE UNIQUE INDEX IF NOT EXISTS uq_otp_code_phone"));
        assertTrue(otpEntity.contains("length = 255"));
        assertTrue(otpEntity.contains("failedAttempts"));
    }

    @Test
    void authMigrationsPersistRefreshTokensAndEmailOtpIdentifiers() throws IOException {
        String refreshMigration = Files.readString(MIGRATION_DIR.resolve("V8__create_refresh_token_table.sql"));
        String otpMigration = Files.readString(MIGRATION_DIR.resolve("V9__support_email_otp_identifiers.sql"));
        String userMigration = Files.readString(MIGRATION_DIR.resolve("V10__harden_user_auth_state.sql"));
        String refreshEntity = Files.readString(Path.of("src/main/java/swp391/carwash/entity/RefreshToken.java"));
        String otpEntity = Files.readString(Path.of("src/main/java/swp391/carwash/entity/OtpCode.java"));
        String userStatus = Files.readString(Path.of("src/main/java/swp391/carwash/enums/UserStatus.java"));

        assertTrue(refreshMigration.contains("CREATE TABLE IF NOT EXISTS refresh_token"));
        assertTrue(refreshMigration.contains("token_hash VARCHAR(128) NOT NULL UNIQUE"));
        assertTrue(refreshEntity.contains("tokenHash"));
        assertTrue(otpMigration.contains("channel VARCHAR(20)"));
        assertTrue(otpMigration.contains("identifier VARCHAR(255)"));
        assertTrue(otpEntity.contains("OtpChannel"));
        assertTrue(userMigration.contains("PENDING_VERIFY"));
        assertTrue(userMigration.contains("failed_login_count"));
        assertTrue(userStatus.contains("PENDING_VERIFY"));
    }
}
