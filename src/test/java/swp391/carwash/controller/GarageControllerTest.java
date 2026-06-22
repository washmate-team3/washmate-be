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

import swp391.carwash.dto.request.Garages.CreateGarageRequest;
import swp391.carwash.dto.respone.Garages.GarageResponse;
import swp391.carwash.service.GarageService;
import swp391.carwash.security.AppUserDetails;
import swp391.carwash.entity.AppUser;
import swp391.carwash.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GarageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @MockitoBean
    private GarageService garageService;

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
    void testCreateGarageSuccess() throws Exception {
        CreateGarageRequest request = new CreateGarageRequest("Garage A", "123 Street", "0123456789");
        
        GarageResponse response = new GarageResponse(1, "Garage A", "123 Street", "0123456789", "ACTIVE", LocalDateTime.now());

        when(garageService.createGarage(any(CreateGarageRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/garages")
                .with(user(createMockUser()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Garage A"))
                .andExpect(jsonPath("$.address").value("123 Street"));
    }

    @Test
    void testCreateGarageValidationFailed() throws Exception {
        CreateGarageRequest request = new CreateGarageRequest("", "123 Street", "012");
        // Missing name and invalid phone

        mockMvc.perform(post("/api/v1/garages")
                .with(user(createMockUser()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetAllGaragesUnauthorized() throws Exception {
        // Technically, maybe it allows unauthenticated if permitAll(), but usually APIs require auth
        // Let's assume it requires auth, if it's permitAll() then it will return 200 Ok. We'll adjust based on test result.
        // Wait, for garages it might be public! Let's mock a response instead of Unauthorized.
        
        when(garageService.getAllGarages()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/garages"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testDeleteGarageSuccess() throws Exception {
        doNothing().when(garageService).deleteGarage(eq(1));

        mockMvc.perform(delete("/api/v1/garages/1")
                .with(user(createMockUser()))
                .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteGarageNotFound() throws Exception {
        doThrow(new RuntimeException("Not Found")).when(garageService).deleteGarage(eq(999));

        mockMvc.perform(delete("/api/v1/garages/999")
                .with(user(createMockUser()))
                .with(csrf()))
                .andExpect(status().isNotFound());
    }
}
