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

import swp391.carwash.dto.LoginRequest;
import swp391.carwash.dto.RegisterRequest;
import swp391.carwash.dto.AuthResponse;
import swp391.carwash.dto.GoogleLoginRequest;
import swp391.carwash.dto.OtpResponse;
import swp391.carwash.common.exception.ApiException;
import swp391.carwash.service.AuthService;
import org.springframework.http.HttpStatus;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @MockitoBean
    private AuthService authService;

    @Test
    void testLoginValidInfo() throws Exception {
        LoginRequest request = new LoginRequest("admin", null, "password");

        AuthResponse mockResponse = new AuthResponse("mock-jwt-token", "refresh-token", "Bearer", 3600, null);

        when(authService.login(any(LoginRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock-jwt-token"));
    }

    @Test
    void testLoginWrongPassword() throws Exception {
        LoginRequest request = new LoginRequest("admin", null, "wrong");

        when(authService.login(any(LoginRequest.class))).thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testRegisterValidInfo() throws Exception {
        RegisterRequest request = new RegisterRequest("test@example.com", "Password123!", "New User", "0123456789");

        OtpResponse mockResponse = new OtpResponse("test@example.com", null, "OTP sent to email");

        when(authService.register(any(RegisterRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP sent to email"));
    }

    @Test
    void testGoogleLoginPublicEvenWithBadBearerHeader() throws Exception {
        GoogleLoginRequest request = new GoogleLoginRequest("google-id-token");
        AuthResponse mockResponse = new AuthResponse("google-access", "google-refresh", "Bearer", 3600, null);

        when(authService.loginWithGoogle(any(GoogleLoginRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/auth/google")
                .header("Authorization", "Bearer broken-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("google-access"));
    }
}
