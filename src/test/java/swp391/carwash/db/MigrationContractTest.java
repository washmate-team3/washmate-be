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
        String migration = Files.readString(MIGRATION_DIR.resolve("V1__baseline_schema.sql"));

        assertTrue(migration.contains("CREATE TRIGGER trg_check_booking_slot_capacity"));
        assertTrue(migration.contains("CREATE TRIGGER trg_refresh_booking_slot_capacity_insert_update"));
        assertTrue(migration.contains("CREATE TRIGGER trg_refresh_booking_slot_capacity_delete"));
        assertTrue(migration.contains("CREATE TRIGGER trg_validate_payment_paid_amount"));
    }

    @Test
    void publicSchemaMigrationEnablesRlsAndRevokesDataApiRoles() throws IOException {
        String migration = Files.readString(MIGRATION_DIR.resolve("V1__baseline_schema.sql"));

        assertTrue(migration.contains("ENABLE ROW LEVEL SECURITY"));
        assertTrue(migration.contains("rolname = 'anon'"));
        assertTrue(migration.contains("rolname = 'authenticated'"));
        assertTrue(migration.contains("REVOKE ALL PRIVILEGES ON ALL TABLES"));
        assertFalse(migration.contains("CREATE POLICY"));
    }

    @Test
    void insightMigrationEnablesRlsAndAddsReportingIndexes() throws IOException {
        String migration = Files.readString(MIGRATION_DIR.resolve("V1__baseline_schema.sql"));
        String sourceMigration = Files.readString(MIGRATION_DIR.resolve("V1__baseline_schema.sql"));

        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS insight_rule_config"));
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS business_insight"));
        assertTrue(migration.contains("CREATE TABLE insight_ai_enrichment"));
        assertTrue(migration.contains("ALTER TABLE business_insight ENABLE ROW LEVEL SECURITY"));
        assertTrue(migration.contains("ALTER TABLE insight_ai_enrichment ENABLE ROW LEVEL SECURITY"));
        assertTrue(migration.contains("idx_invoice_insight_paid_status_date"));
        assertTrue(migration.contains("idx_booking_insight_booking_date"));
        assertTrue(sourceMigration.contains("source VARCHAR(20)"));
        assertTrue(sourceMigration.contains("evidence_json JSONB"));
        assertTrue(sourceMigration.contains("verified BOOLEAN"));
    }
}
