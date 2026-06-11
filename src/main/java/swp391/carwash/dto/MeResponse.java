package swp391.carwash.dto;

import java.util.List;

public record MeResponse(Integer id, String email, String fullName, String phone, String status, List<String> roles, List<Integer> garageIds) {
}
