package swp391.carwash.service.insight;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public final class InsightText {
    private static final Locale VIETNAM = Locale.forLanguageTag("vi-VN");

    private InsightText() {
    }

    public static String percent(double value) {
        return BigDecimal.valueOf(value)
                .setScale(1, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString() + "%";
    }

    public static String money(BigDecimal value) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        return NumberFormat.getNumberInstance(VIETNAM).format(safeValue.setScale(0, RoundingMode.HALF_UP)) + " VND";
    }

    public static String number(long value) {
        return NumberFormat.getNumberInstance(VIETNAM).format(value);
    }

    public static String minutes(double value) {
        return BigDecimal.valueOf(value)
                .setScale(1, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString() + " phút";
    }
}
