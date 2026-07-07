package swp391.carwash.common;

import java.time.ZoneId;

/**
 * Timezone chuẩn của hệ thống. Quy ước: DB lưu UTC, mọi tính toán
 * "theo ngày" (báo cáo, filter from/to date, VNPAY) dùng giờ Việt Nam.
 * KHÔNG dùng ZoneId.systemDefault() — server production có thể chạy UTC.
 */
public final class TimeZones {
    public static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

    private TimeZones() {
    }
}
