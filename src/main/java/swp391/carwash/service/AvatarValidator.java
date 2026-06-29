package swp391.carwash.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import swp391.carwash.common.exception.ApiException;

@Component
public class AvatarValidator {
    private static final long MAX_AVATAR_BYTES = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Avatar file is required");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Avatar file must be 5MB or smaller");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only jpg, png, and webp avatar images are allowed");
        }
        if (!hasValidImageSignature(readHeader(file, 12), contentType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Avatar file content does not match a supported image format");
        }
    }

    public String resolveExtension(MultipartFile file) {
        return switch (file.getContentType()) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }

    private byte[] readHeader(MultipartFile file, int length) {
        byte[] header = new byte[length];
        try (InputStream inputStream = file.getInputStream()) {
            int read = inputStream.read(header);
            if (read < length) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Avatar file is invalid");
            }
            return header;
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot read avatar file");
        }
    }

    private boolean hasValidImageSignature(byte[] header, String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> isJpeg(header);
            case "image/png" -> isPng(header);
            case "image/webp" -> isWebp(header);
            default -> false;
        };
    }

    private boolean isJpeg(byte[] header) {
        return header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF;
    }

    private boolean isPng(byte[] header) {
        return header[0] == (byte) 0x89
                && header[1] == 0x50
                && header[2] == 0x4E
                && header[3] == 0x47
                && header[4] == 0x0D
                && header[5] == 0x0A
                && header[6] == 0x1A
                && header[7] == 0x0A;
    }

    private boolean isWebp(byte[] header) {
        return header[0] == 'R'
                && header[1] == 'I'
                && header[2] == 'F'
                && header[3] == 'F'
                && header[8] == 'W'
                && header[9] == 'E'
                && header[10] == 'B'
                && header[11] == 'P';
    }
}
