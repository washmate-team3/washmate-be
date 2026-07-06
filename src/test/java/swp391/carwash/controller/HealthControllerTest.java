package swp391.carwash.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.javamail.JavaMailSender;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @Test
    void testRootPathIsAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("WashMate backend is running"))); 
    }

    @Test
    void testHealthCheckIsAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP")); // Tùy thuộc vào implementation
    }

    @Test
    void testCorsValidOrigin() throws Exception {
        mockMvc.perform(get("/api/health")
                .header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void testCorsPreflightRequest() throws Exception {
        mockMvc.perform(options("/api/health")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("GET")))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void testCorsInvalidOrigin() throws Exception {
        mockMvc.perform(get("/api/health")
                .header("Origin", "http://evil-domain.com"))
                .andExpect(status().isForbidden()); // Tùy vào cấu hình CorsFilter
    }

    @Test
    void testProtectedEndpointRequiresToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

}
