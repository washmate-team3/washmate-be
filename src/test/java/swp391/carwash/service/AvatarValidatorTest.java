package swp391.carwash.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import swp391.carwash.common.exception.ApiException;

class AvatarValidatorTest {
    private final AvatarValidator validator = new AvatarValidator();

    @Test
    void acceptsJpegPngAndWebpImages() {
        assertDoesNotThrow(() -> validator.validate(file("avatar.jpg", "image/jpeg", jpegBytes())));
        assertDoesNotThrow(() -> validator.validate(file("avatar.png", "image/png", pngBytes())));
        assertDoesNotThrow(() -> validator.validate(file("avatar.webp", "image/webp", webpBytes())));
    }

    @Test
    void rejectsAllowedMimeTypeWithInvalidSignature() {
        ApiException exception = assertThrows(ApiException.class,
                () -> validator.validate(file("avatar.jpg", "image/jpeg", "not an image".getBytes())));

        assertEquals("Avatar file content does not match a supported image format", exception.getMessage());
    }

    @Test
    void rejectsUnsupportedMimeType() {
        ApiException exception = assertThrows(ApiException.class,
                () -> validator.validate(file("avatar.pdf", "application/pdf", "fake pdf".getBytes())));

        assertEquals("Only jpg, png, and webp avatar images are allowed", exception.getMessage());
    }

    private MockMultipartFile file(String name, String contentType, byte[] bytes) {
        return new MockMultipartFile("file", name, contentType, bytes);
    }

    private byte[] jpegBytes() {
        return new byte[] {
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00,
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00
        };
    }

    private byte[] pngBytes() {
        return new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x00
        };
    }

    private byte[] webpBytes() {
        return new byte[] {
                'R', 'I', 'F', 'F',
                0x00, 0x00, 0x00, 0x00,
                'W', 'E', 'B', 'P'
        };
    }
}
