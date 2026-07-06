package swp391.carwash.service.Loyalty.Support;

import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

@Component
public class QuarterCalculator {


    public QuarterPeriod previousQuarter() {

        LocalDate today = LocalDate.now();

        int currentQuarter = ((today.getMonthValue() - 1) / 3) + 1;

        LocalDate currentQuarterStart = switch (currentQuarter) {
            case 1 -> LocalDate.of(today.getYear(), 1, 1);
            case 2 -> LocalDate.of(today.getYear(), 4, 1);
            case 3 -> LocalDate.of(today.getYear(), 7, 1);
            default -> LocalDate.of(today.getYear(), 10, 1);
        };

        LocalDate previousQuarterStart = currentQuarterStart.minusMonths(3);

        return new QuarterPeriod(
                previousQuarterStart.atStartOfDay().atOffset(ZoneOffset.UTC),
                currentQuarterStart.atStartOfDay().atOffset(ZoneOffset.UTC)
        );
    }
    public QuarterPeriod currentQuarter() {
        LocalDate today = LocalDate.now();

        int currentQuarter = ((today.getMonthValue() - 1) / 3) + 1;

        LocalDate start = switch (currentQuarter) {
            case 1 -> LocalDate.of(today.getYear(), 1, 1);
            case 2 -> LocalDate.of(today.getYear(), 4, 1);
            case 3 -> LocalDate.of(today.getYear(), 7, 1);
            default -> LocalDate.of(today.getYear(), 10, 1);
        };

        LocalDate end = start.plusMonths(3);

        return new QuarterPeriod(
                start.atStartOfDay().atOffset(ZoneOffset.UTC),
                end.atStartOfDay().atOffset(ZoneOffset.UTC)
        );
    }
}
