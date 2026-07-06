package swp391.carwash.security;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Nguồn duy nhất cho logic phân quyền theo garage.
 * Scope hệ thống: 1 OWNER quản lý nhiều garage (OWNER có toàn quyền),
 * STAFF chỉ thao tác trên các garage được phân công (garageIds trong JWT).
 * ADMIN/MANAGER giữ lại để tương thích dữ liệu role cũ.
 */
@Component
public class GarageAccessEvaluator {

    public boolean canOperate(Integer garageId, AppUserDetails principal) {
        List<String> roles = principal.getRoleNames();
        if (roles.contains("OWNER") || roles.contains("ADMIN")) {
            return true;
        }
        return (roles.contains("STAFF") || roles.contains("MANAGER"))
                && principal.getGarageIds().contains(garageId);
    }
}
