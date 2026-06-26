package swp391.carwash.dto.response.ServicePackage;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
public class ServicePackageResponse {
    private Integer servicePackageId;
    private Integer garageId;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer durationMinutes;
    private String status;
    private OffsetDateTime createdAt;
}