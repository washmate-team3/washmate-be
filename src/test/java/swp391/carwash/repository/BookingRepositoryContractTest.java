package swp391.carwash.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class BookingRepositoryContractTest {
    private static final Path BOOKING_REPOSITORY =
            Path.of("src/main/java/swp391/carwash/repository/BookingRepository.java");
    private static final Path BOOKING_BEHAVIOR_VIEW_MIGRATION =
            Path.of("src/main/resources/db/migration/V4__create_booking_behavior_monthly_view.sql");

    @Test
    void bookingBehaviorAnalyticsReadsFromVersionedView() throws IOException {
        String repository = Files.readString(BOOKING_REPOSITORY);
        String migration = Files.readString(BOOKING_BEHAVIOR_VIEW_MIGRATION);

        assertTrue(repository.contains("FROM booking_behavior_monthly"));
        assertTrue(migration.contains("CREATE OR REPLACE VIEW booking_behavior_monthly"));
        assertFalse(migration.contains("CREATE TABLE IF NOT EXISTS booking_behavior_monthly"));
    }
}
