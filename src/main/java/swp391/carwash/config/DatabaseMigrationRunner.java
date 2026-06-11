package swp391.carwash.config;

import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.flywaydb.core.Flyway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "washmate.database.migration-runner.enabled", havingValue = "true")
@Order(0)
@RequiredArgsConstructor
public class DatabaseMigrationRunner implements CommandLineRunner {
    private final DataSource dataSource;

    @Override
    public void run(String... args) {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load()
                .migrate();
    }
}