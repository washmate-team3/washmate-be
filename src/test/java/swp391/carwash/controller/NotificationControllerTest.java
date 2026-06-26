package swp391.carwash.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mail.javamail.JavaMailSender;

import swp391.carwash.dto.response.Notification.NotificationResponse;
import swp391.carwash.dto.response.PagedResponse;
import swp391.carwash.service.NotificationService;
import swp391.carwash.security.AppUserDetails;
import swp391.carwash.entity.AppUser;
import swp391.carwash.enums.UserStatus;

import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @MockitoBean
    private NotificationService notificationService;

    private AppUserDetails createMockUser() {
        AppUser appUser = new AppUser();
        appUser.setId(1);
        appUser.setEmail("user@example.com");
        appUser.setFullName("Test User");
        appUser.setPhone("0123456789");
        appUser.setStatus(UserStatus.ACTIVE);
        appUser.setUserRoles(Collections.emptySet());
        return new AppUserDetails(appUser);
    }

    @Test
    void testGetMyNotificationsAuthorized() throws Exception {
        NotificationResponse mockResponse = new NotificationResponse(
                100, 1, "BOOKING", "50", "INFO", "Booking Confirmed", "Your booking was confirmed", false, null, null);

        PagedResponse<NotificationResponse> pagedResponse = new PagedResponse<>(
                List.of(mockResponse), 0, 10, 1L, 1, true);

        when(notificationService.getUserNotifications(anyInt(), anyInt(), anyInt())).thenReturn(pagedResponse);

        mockMvc.perform(get("/api/v1/notifications")
                .with(user(createMockUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].notificationId").value(100));
    }

    @Test
    void testMarkAsRead() throws Exception {
        doNothing().when(notificationService).markAsRead(anyInt(), anyInt());

        mockMvc.perform(patch("/api/v1/notifications/100/read")
                .with(user(createMockUser()))
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void testMarkAllAsRead() throws Exception {
        doNothing().when(notificationService).markAllAsRead(anyInt());

        mockMvc.perform(patch("/api/v1/notifications/read-all")
                .with(user(createMockUser()))
                .with(csrf()))
                .andExpect(status().isOk());
    }
}
