package swp391.carwash.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mail.javamail.JavaMailSender;

import swp391.carwash.dto.BookingCreateRequest;
import swp391.carwash.dto.BookingResponse;
import swp391.carwash.service.BookingService;
import swp391.carwash.security.AppUserDetails;
import swp391.carwash.entity.AppUser;
import swp391.carwash.enums.PaymentMethod;
import swp391.carwash.enums.UserStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @MockitoBean
    private BookingService bookingService;

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
    void testGetMyBookingsAuthorized() throws Exception {
        BookingResponse response = new BookingResponse(100, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        
        when(bookingService.getMyBookings(any(AppUserDetails.class))).thenReturn(List.of(response));

        mockMvc.perform(get("/api/bookings/me")
                .with(user(createMockUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100));
    }

    @Test
    void testGetMyBookingsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/bookings/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCreateBookingSuccess() throws Exception {
        String requestJson = "{\n" +
                "  \"garageId\": 1,\n" +
                "  \"slotId\": 5,\n" +
                "  \"serviceId\": 2,\n" +
                "  \"vehicleId\": 10,\n" +
                "  \"bookingDate\": \"2026-06-22\",\n" +
                "  \"discountAmount\": 0,\n" +
                "  \"paymentMethod\": \"CASH\"\n" +
                "}";

        BookingResponse response = new BookingResponse(100, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        when(bookingService.createBooking(any(BookingCreateRequest.class), any(AppUserDetails.class))).thenReturn(response);

        mockMvc.perform(post("/api/bookings")
                .with(user(createMockUser()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100));
    }
}
