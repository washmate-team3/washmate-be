package swp391.carwash.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GarageResponse {
    private Integer garageId;
    private String name;
    private String address;
    private String phone;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
