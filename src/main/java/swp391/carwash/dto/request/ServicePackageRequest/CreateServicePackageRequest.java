package swp391.carwash.dto.request.ServicePackageRequest;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class CreateServicePackageRequest {
    private Integer garageId; // Ép FE truyền Id cụ thể của chi nhánh
    private String name;
    private String description;
    private BigDecimal price;
    private Integer durationMinutes;
}