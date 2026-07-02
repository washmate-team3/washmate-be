package swp391.carwash.dto.insight;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.util.StringUtils;
import swp391.carwash.enums.InsightType;

public record AIChatRequest(
        @Size(max = 1000)
        String question,

        @Size(max = 1000)
        String message,

        Integer insightId,

        LocalDate fromDate,

        LocalDate toDate,

        InsightType type,

        String status,

        @Size(max = 10)
        List<@Valid AIChatMessage> history
) {
        public String effectiveQuestion() {
                return StringUtils.hasText(question) ? question : message;
        }

        @AssertTrue(message = "question or message is required")
        public boolean hasQuestionOrMessage() {
                return StringUtils.hasText(question) || StringUtils.hasText(message);
        }
}
