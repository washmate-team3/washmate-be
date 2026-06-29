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

import swp391.carwash.dto.MeResponse;
import swp391.carwash.dto.AvatarUploadResponse;
import swp391.carwash.dto.UpdateProfileRequest;
import swp391.carwash.service.UserService;
import swp391.carwash.security.AppUserDetails;
import swp391.carwash.entity.AppUser;
import swp391.carwash.enums.UserStatus;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @MockitoBean
    private UserService userService;

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
    void testGetMeUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetMeAuthorized() throws Exception {
        AppUserDetails userDetails = createMockUser();

        mockMvc.perform(get("/api/users/me").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.fullName").value("Test User"));
    }

    @Test
    void testUpdateProfile() throws Exception {
        AppUserDetails userDetails = createMockUser();
        UpdateProfileRequest request = new UpdateProfileRequest("Updated User", "0987654321");
        
        MeResponse mockResponse = new MeResponse(1, "user@example.com", "Updated User", "0987654321", "ACTIVE", Collections.emptyList(), Collections.emptyList());

        when(userService.updateProfile(eq(1), any(UpdateProfileRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(put("/api/users/me")
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated User"))
                .andExpect(jsonPath("$.phone").value("0987654321"));
    }

    @Test
    void testUploadAvatar() throws Exception {
        AppUserDetails userDetails = createMockUser();
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "file",
                "avatar.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0, 0, 0, 0, 0, 0, 0}
        );
        when(userService.uploadAvatar(eq(1), any())).thenReturn(new AvatarUploadResponse("https://example.test/avatar.jpg"));

        mockMvc.perform(multipart("/api/users/me/avatar")
                        .file(file)
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("https://example.test/avatar.jpg"));
    }

    @Test
    void testDeleteAvatar() throws Exception {
        AppUserDetails userDetails = createMockUser();
        when(userService.deleteAvatar(1)).thenReturn(new AvatarUploadResponse(null));

        mockMvc.perform(delete("/api/users/me/avatar").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").doesNotExist());
    }
}
