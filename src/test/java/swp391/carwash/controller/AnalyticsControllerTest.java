package swp391.carwash.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import swp391.carwash.entity.AppUser;
import swp391.carwash.enums.UserStatus;
import swp391.carwash.security.AppUserDetails;

import java.util.Collections;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JavaMailSender javaMailSender;

    private AppUserDetails createMockAdmin() {
        AppUser appUser = new AppUser();
        appUser.setId(1);
        appUser.setEmail("admin@example.com");
        appUser.setFullName("Test Admin");
        appUser.setPhone("0123456789");
        appUser.setStatus(UserStatus.ACTIVE);
        appUser.setUserRoles(Collections.emptySet());
        return new AppUserDetails(appUser);
    }

    @Test
    void getGarageDashboardReturnsEmptyMap() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/garage-owner/dashboard")
                .with(user(createMockAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap());
    }

    @Test
    void getBehavioralLogsReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/admin/behavioral-logs")
                .param("userId", "1")
                .param("monthYear", "2023-10")
                .with(user(createMockAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getCustomerSegmentsReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/admin/customer-segments")
                .param("segmentName", "VIP")
                .param("monthYear", "2023-10")
                .with(user(createMockAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
