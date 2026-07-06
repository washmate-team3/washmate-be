package swp391.carwash.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class PostgresMigrationTest {
    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @BeforeAll
    static void migrate() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .load()
                .migrate();
    }

    @Test
    void migrationsInstallTriggersAndEnableRls() throws SQLException {
        try (Connection connection = POSTGRES.createConnection("")) {
            assertEquals(4, countNamedTriggers(connection));
            assertRlsEnabled(connection, "garage");
            assertRlsEnabled(connection, "app_user");
            assertRlsEnabled(connection, "booking");
            assertRlsEnabled(connection, "payment");
            assertRlsEnabled(connection, "insight_rule_config");
            assertRlsEnabled(connection, "business_insight");
            assertRlsEnabled(connection, "insight_ai_enrichment");
        }
    }

    @Test
    void capacityTriggerRejectsOverbookingAndMaintainsCounter() throws SQLException {
        try (Connection connection = POSTGRES.createConnection("")) {
            Fixture fixture = createFixture(connection, 1);
            insertBooking(connection, fixture, fixture.firstVehicleId(), "BOOKING-1");

            assertEquals(1, currentCapacity(connection, fixture));
            SQLException exception = assertThrows(SQLException.class,
                    () -> insertBooking(connection, fixture, fixture.secondVehicleId(), "BOOKING-2"));
            assertTrue(exception.getMessage().contains("Selected booking slot is full"));
        }
    }

    @Test
    void paymentTriggerRejectsPaidAmountDifferentFromBookingTotal() throws SQLException {
        try (Connection connection = POSTGRES.createConnection("")) {
            Fixture fixture = createFixture(connection, 2);
            int bookingId = insertBooking(connection, fixture, fixture.firstVehicleId(), "PAYMENT-BOOKING");

            SQLException exception = assertThrows(SQLException.class, () -> {
                try (PreparedStatement statement = connection.prepareStatement("""
                        insert into payment(booking_id, garage_id, amount, method, status)
                        values (?, ?, ?, 'CASH', 'PAID')
                        """)) {
                    statement.setInt(1, bookingId);
                    statement.setInt(2, fixture.garageId());
                    statement.setBigDecimal(3, new BigDecimal("90000.00"));
                    statement.executeUpdate();
                }
            });
            assertTrue(exception.getMessage().contains("does not match booking final amount"));
        }
    }

    private static int countNamedTriggers(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select count(*)
                from pg_trigger
                where not tgisinternal
                  and tgname in (
                    'trg_check_booking_slot_capacity',
                    'trg_refresh_booking_slot_capacity_insert_update',
                    'trg_refresh_booking_slot_capacity_delete',
                    'trg_validate_payment_paid_amount'
                  )
                """); ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getInt(1);
        }
    }

    private static void assertRlsEnabled(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select relrowsecurity
                from pg_class
                where oid = ('public.' || ?)::regclass
                """)) {
            statement.setString(1, tableName);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                assertTrue(result.getBoolean(1));
            }
        }
    }

    private static Fixture createFixture(Connection connection, int maxCapacity) throws SQLException {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        int garageId = insertReturningId(connection,
                "insert into garage(name, address, phone) values (?, ?, ?) returning garage_id",
                "Garage " + suffix, "Address " + suffix, "09" + suffix);
        int userId = insertReturningId(connection,
                "insert into app_user(email, full_name, phone) values (?, ?, ?) returning user_id",
                suffix + "@example.com", "User " + suffix, "08" + suffix);
        int firstVehicleId = insertReturningId(connection,
                "insert into vehicle(user_id, license_plate) values (?, ?) returning vehicle_id",
                userId, "PLATE-1-" + suffix);
        int secondVehicleId = insertReturningId(connection,
                "insert into vehicle(user_id, license_plate) values (?, ?) returning vehicle_id",
                userId, "PLATE-2-" + suffix);
        int slotId = insertReturningId(connection,
                "insert into booking_slot(garage_id, start_time, end_time, max_capacity) values (?, '08:00', '09:00', ?) returning slot_id",
                garageId, maxCapacity);
        int serviceId = insertReturningId(connection,
                "insert into service_package(garage_id, name, price, duration) values (?, ?, 100000.00, 30) returning service_id",
                garageId, "Service " + suffix);
        return new Fixture(garageId, userId, firstVehicleId, secondVehicleId, slotId, serviceId,
                LocalDate.now().plusDays(1));
    }

    private static int insertBooking(Connection connection, Fixture fixture, int vehicleId, String codePrefix)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into booking(
                    booking_code, user_id, garage_id, slot_id, service_id, vehicle_id,
                    booking_date, total_amount, discount_amount, final_amount, status
                ) values (?, ?, ?, ?, ?, ?, ?, 100000.00, 0.00, 100000.00, 'PENDING')
                returning booking_id
                """)) {
            statement.setString(1, codePrefix + "-" + UUID.randomUUID());
            statement.setInt(2, fixture.userId());
            statement.setInt(3, fixture.garageId());
            statement.setInt(4, fixture.slotId());
            statement.setInt(5, fixture.serviceId());
            statement.setInt(6, vehicleId);
            statement.setDate(7, Date.valueOf(fixture.bookingDate()));
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private static int currentCapacity(Connection connection, Fixture fixture) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select current_capacity
                from booking_slot_capacity
                where slot_id = ? and booking_date = ?
                """)) {
            statement.setInt(1, fixture.slotId());
            statement.setDate(2, Date.valueOf(fixture.bookingDate()));
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private static int insertReturningId(Connection connection, String sql, Object... parameters)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < parameters.length; index++) {
                statement.setObject(index + 1, parameters[index]);
            }
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private record Fixture(
            int garageId,
            int userId,
            int firstVehicleId,
            int secondVehicleId,
            int slotId,
            int serviceId,
            LocalDate bookingDate
    ) {
    }
}
