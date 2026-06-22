package swp391.carwash.security;

import java.util.Collection;
import java.util.List;
import swp391.carwash.entity.AppUser;
import swp391.carwash.enums.RecordStatus;
import swp391.carwash.enums.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AppUserDetails implements UserDetails {
    private final AppUser user;

    public AppUserDetails(AppUser user) {
        this.user = user;
    }

    public Integer getId() {
        return user.getId();
    }

    public AppUser getUser() {
        return user;
    }

    public List<Integer> getGarageIds() {
        return user.getUserRoles().stream()
                .filter(userRole -> userRole.getStatus() == RecordStatus.ACTIVE)
                .filter(userRole -> userRole.getGarage() != null)
                .map(userRole -> userRole.getGarage().getId())
                .distinct()
                .toList();
    }

    public List<String> getRoleNames() {
        return user.getUserRoles().stream()
                .filter(userRole -> userRole.getStatus() == RecordStatus.ACTIVE)
                .map(userRole -> userRole.getRole().getRoleName().name())
                .distinct()
                .toList();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return getRoleNames().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail() != null ? user.getEmail() : user.getPhone();
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() != UserStatus.BLOCKED;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == UserStatus.ACTIVE;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
