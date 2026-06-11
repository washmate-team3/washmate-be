package swp391.carwash.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("garageScope")
public class GarageScopeSecurity {
    public boolean hasGarageAccess(Authentication authentication, Integer garageId) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserDetails userDetails)) {
            return false;
        }
        if (userDetails.getRoleNames().stream().anyMatch(role -> role.equals("ADMIN") || role.equals("OWNER"))) {
            return garageId == null || userDetails.getGarageIds().isEmpty() || userDetails.getGarageIds().contains(garageId);
        }
        return garageId != null && userDetails.getGarageIds().contains(garageId);
    }
}
