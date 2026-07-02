package swp391.carwash.Schedule;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import swp391.carwash.dto.insight.InsightGenerateResponse;
import swp391.carwash.service.InsightService;

/**
 * Scheduled jobs để tự động generate AutoWash Insights định kỳ.
 *
 * Daily job  : chạy 00:30 mỗi ngày — phân tích dữ liệu ngày hôm qua.
 * Monthly job: chạy 01:00 ngày 1 hàng tháng — phân tích toàn bộ tháng trước.
 *
 * Cả 2 job dùng upsert logic từ InsightService nên an toàn khi chạy lại
 * nhiều lần — không tạo insight trùng cùng (rule_code, from_date, to_date).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InsightScheduler {

    private final InsightService insightService;

    /**
     * Chạy mỗi ngày lúc 00:30.
     * Phân tích dữ liệu của ngày hôm qua để sinh insight.
     */
    @Scheduled(cron = "0 30 0 * * *")
    public void generateDailyInsights() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("[InsightScheduler] Daily job — generating insights for {}", yesterday);
        try {
            InsightGenerateResponse response = insightService.generateInsights(yesterday, yesterday);
            log.info("[InsightScheduler] Daily job done — generated={}, created={}, updated={}",
                    response.generatedCount(), response.createdCount(), response.updatedCount());
        } catch (Exception e) {
            log.error("[InsightScheduler] Daily job failed for {}: {}", yesterday, e.getMessage(), e);
        }
    }

    /**
     * Chạy vào ngày 1 hàng tháng lúc 01:00.
     * Phân tích toàn bộ tháng trước để sinh insight tổng hợp.
     */
    @Scheduled(cron = "0 0 1 1 * *")
    public void generateMonthlyInsights() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayLastMonth = today.minusMonths(1).withDayOfMonth(1);
        LocalDate lastDayLastMonth = today.withDayOfMonth(1).minusDays(1);
        log.info("[InsightScheduler] Monthly job — generating insights for {} to {}",
                firstDayLastMonth, lastDayLastMonth);
        try {
            InsightGenerateResponse response = insightService.generateInsights(firstDayLastMonth, lastDayLastMonth);
            log.info("[InsightScheduler] Monthly job done — generated={}, created={}, updated={}",
                    response.generatedCount(), response.createdCount(), response.updatedCount());
        } catch (Exception e) {
            log.error("[InsightScheduler] Monthly job failed for {} to {}: {}",
                    firstDayLastMonth, lastDayLastMonth, e.getMessage(), e);
        }
    }
}
