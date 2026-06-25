package swp391.carwash.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import swp391.carwash.common.exception.ApiException;
import swp391.carwash.dto.CustomerAiInsightAdminResponse;
import swp391.carwash.entity.AppUser;
import swp391.carwash.entity.CustomerAiInsight;
import swp391.carwash.entity.Garage;
import swp391.carwash.repository.AppUserRepository;
import swp391.carwash.repository.BookingBehaviorMonthlyView;
import swp391.carwash.repository.BookingRepository;
import swp391.carwash.repository.CustomerAiInsightRepository;
import swp391.carwash.repository.GarageRepository;

@ExtendWith(MockitoExtension.class)
class AiInsightGenerationServiceTest {
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private CustomerAiInsightRepository customerAiInsightRepository;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private GarageRepository garageRepository;

    private AiInsightGenerationService service;
    private AppUser user;
    private Garage garage;

    @BeforeEach
    void setUp() {
        service = new AiInsightGenerationService(
                bookingRepository,
                customerAiInsightRepository,
                appUserRepository,
                garageRepository,
                new AiInsightRuleEngine());
        user = AppUser.builder().id(10).fullName("Customer").phone("0911111111").build();
        garage = Garage.builder().id(1).name("Garage").address("Address").phone("0900000000").build();
    }

    @Test
    void generateInsightsCreatesFourInsightTypesForEachBehavior() {
        BookingBehaviorMonthlyView behavior = behavior(10, 1, "2026-06", 3, 3, 0, 0);
        List<CustomerAiInsight> saved = new ArrayList<>();

        when(bookingRepository.findBookingBehaviorMonthly(1, "2026-06")).thenReturn(List.of(behavior));
        when(appUserRepository.getReferenceById(10)).thenReturn(user);
        when(garageRepository.getReferenceById(1)).thenReturn(garage);
        when(customerAiInsightRepository.findByUserIdAndGarageIdAndPeriodAndInsightTypeAndModelVersion(
                any(), any(), any(), any(), any())).thenReturn(Optional.empty());
        when(customerAiInsightRepository.save(any(CustomerAiInsight.class))).thenAnswer(invocation -> {
            CustomerAiInsight insight = invocation.getArgument(0);
            insight.setId(saved.size() + 1);
            saved.add(insight);
            return insight;
        });
        when(customerAiInsightRepository.findByGarageIdAndPeriodOrderByGeneratedAtDesc(1, "2026-06"))
                .thenAnswer(invocation -> saved);

        List<CustomerAiInsightAdminResponse> response = service.generateInsights(1, "2026-06");

        assertEquals(4, response.size());
        assertEquals(List.of(
                        AiInsightRuleEngine.CUSTOMER_SEGMENT,
                        AiInsightRuleEngine.CHURN_RISK,
                        AiInsightRuleEngine.PROMOTION_RECOMMENDATION,
                        AiInsightRuleEngine.SERVICE_RECOMMENDATION),
                response.stream().map(CustomerAiInsightAdminResponse::insightType).toList());
        verify(customerAiInsightRepository).flush();
    }

    @Test
    void generateInsightsUpdatesExistingInsightInsteadOfCreatingDuplicate() {
        BookingBehaviorMonthlyView behavior = behavior(10, 1, "2026-06", 7, 5, 0, 0);
        CustomerAiInsight existing = CustomerAiInsight.builder()
                .id(99)
                .user(user)
                .garage(garage)
                .period("2026-06")
                .insightType(AiInsightRuleEngine.CUSTOMER_SEGMENT)
                .modelVersion(AiInsightRuleEngine.MODEL_VERSION)
                .predictionValue(java.util.Map.of("label", "OLD"))
                .confidenceScore(new BigDecimal("0.60"))
                .build();
        List<CustomerAiInsight> saved = new ArrayList<>();

        when(bookingRepository.findBookingBehaviorMonthly(1, "2026-06")).thenReturn(List.of(behavior));
        when(appUserRepository.getReferenceById(10)).thenReturn(user);
        when(garageRepository.getReferenceById(1)).thenReturn(garage);
        when(customerAiInsightRepository.findByUserIdAndGarageIdAndPeriodAndInsightTypeAndModelVersion(
                any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> AiInsightRuleEngine.CUSTOMER_SEGMENT.equals(invocation.getArgument(3))
                        ? Optional.of(existing)
                        : Optional.empty());
        when(customerAiInsightRepository.save(any(CustomerAiInsight.class))).thenAnswer(invocation -> {
            CustomerAiInsight insight = invocation.getArgument(0);
            saved.add(insight);
            return insight;
        });
        when(customerAiInsightRepository.findByGarageIdAndPeriodOrderByGeneratedAtDesc(1, "2026-06"))
                .thenAnswer(invocation -> saved);

        service.generateInsights(1, "2026-06");

        CustomerAiInsight updated = saved.stream()
                .filter(insight -> AiInsightRuleEngine.CUSTOMER_SEGMENT.equals(insight.getInsightType()))
                .findFirst()
                .orElseThrow();
        assertEquals(99, updated.getId());
        assertEquals("VIP", updated.getPredictionValue().get("label"));
        assertEquals(4, saved.size());
    }

    @Test
    void generateInsightsReturnsEmptyListWhenThereIsNoBehaviorData() {
        when(bookingRepository.findBookingBehaviorMonthly(1, "2026-06")).thenReturn(List.of());
        when(customerAiInsightRepository.findByGarageIdAndPeriodOrderByGeneratedAtDesc(1, "2026-06"))
                .thenReturn(List.of());

        List<CustomerAiInsightAdminResponse> response = service.generateInsights(1, "2026-06");

        assertEquals(List.of(), response);
    }

    @Test
    void getInsightsRejectsInvalidPeriod() {
        ApiException exception = assertThrows(ApiException.class, () -> service.getInsights(1, "2026-13"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Period must use YYYY-MM format", exception.getMessage());
    }

    private BookingBehaviorMonthlyView behavior(
            Integer userId,
            Integer garageId,
            String monthYear,
            Integer totalBookings,
            Integer completedCount,
            Integer cancelledCount,
            Integer noShowCount) {
        return new TestBookingBehaviorMonthlyView(
                userId,
                garageId,
                monthYear,
                totalBookings,
                completedCount,
                cancelledCount,
                noShowCount,
                new BigDecimal("300000"),
                2,
                "ACTIVE",
                OffsetDateTime.now());
    }

    private record TestBookingBehaviorMonthlyView(
            Integer userId,
            Integer garageId,
            String monthYear,
            Integer totalBookings,
            Integer completedCount,
            Integer cancelledCount,
            Integer noShowCount,
            BigDecimal totalSpent,
            Integer preferredSlotId,
            String status,
            OffsetDateTime lastUpdated
    ) implements BookingBehaviorMonthlyView {
        @Override
        public Integer getUserId() {
            return userId;
        }

        @Override
        public Integer getGarageId() {
            return garageId;
        }

        @Override
        public String getMonthYear() {
            return monthYear;
        }

        @Override
        public Integer getTotalBookings() {
            return totalBookings;
        }

        @Override
        public Integer getCompletedCount() {
            return completedCount;
        }

        @Override
        public Integer getCancelledCount() {
            return cancelledCount;
        }

        @Override
        public Integer getNoShowCount() {
            return noShowCount;
        }

        @Override
        public BigDecimal getTotalSpent() {
            return totalSpent;
        }

        @Override
        public Integer getPreferredSlotId() {
            return preferredSlotId;
        }

        @Override
        public String getStatus() {
            return status;
        }

        @Override
        public OffsetDateTime getLastUpdated() {
            return lastUpdated;
        }
    }
}
