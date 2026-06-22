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
    void initialSchemaDefinesCapacityAndPaymentGuards() throws IOException {
        String migration = Files.readString(MIGRATION_DIR.resolve("V2__init_washmate_schema.sql"));

        assertTrue(migration.contains("CREATE TRIGGER trg_check_booking_slot_capacity"));
        assertTrue(migration.contains("CREATE TRIGGER trg_refresh_booking_slot_capacity_insert_update"));
        assertTrue(migration.contains("CREATE TRIGGER trg_refresh_booking_slot_capacity_delete"));
        assertTrue(migration.contains("CREATE TRIGGER trg_validate_payment_paid_amount"));
    }

    @Test
    void publicSchemaMigrationEnablesRlsAndRevokesDataApiRoles() throws IOException {
        String migration = Files.readString(MIGRATION_DIR.resolve("V3__secure_public_schema_for_backend_only_access.sql"));

        assertTrue(migration.contains("ENABLE ROW LEVEL SECURITY"));
        assertTrue(migration.contains("rolname = 'anon'"));
        assertTrue(migration.contains("rolname = 'authenticated'"));
        assertTrue(migration.contains("REVOKE ALL PRIVILEGES ON ALL TABLES"));
        assertFalse(migration.contains("CREATE POLICY"));
    }
}
