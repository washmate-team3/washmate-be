package swp391.carwash.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import swp391.carwash.common.exception.ApiException;

@Service
public class JwtService {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final byte[] secret;
    private final long accessTokenSeconds;
    private final long refreshTokenSeconds;

    public JwtService(
            @Value("${washmate.security.jwt.secret}") String secret,
            @Value("${washmate.security.jwt.access-token-minutes}") long accessTokenMinutes,
            @Value("${washmate.security.jwt.refresh-token-days}") long refreshTokenDays) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 characters");
        }
        if (accessTokenMinutes <= 0 || refreshTokenDays <= 0) {
            throw new IllegalArgumentException("JWT token lifetime must be positive");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.accessTokenSeconds = accessTokenMinutes * 60;
        this.refreshTokenSeconds = refreshTokenDays * 24 * 60 * 60;
    }

    public String createAccessToken(AppUserDetails user) {
        return createToken(user, "access", accessTokenSeconds);
    }

    public String createRefreshToken(AppUserDetails user) {
        return createToken(user, "refresh", refreshTokenSeconds);
    }

    public long getAccessTokenSeconds() {
        return accessTokenSeconds;
    }

    public Integer extractUserId(String token, String expectedType) {
        String payload = parsePayload(token);
        String type = stringClaim(payload, "typ");
        if (!expectedType.equals(type)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid token type");
        }
        long exp = longClaim(payload, "exp");
        if (exp < Instant.now().getEpochSecond()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token expired");
        }
        return (int) longClaim(payload, "sub");
    }

    private String createToken(AppUserDetails user, String type, long ttlSeconds) {
        try {
            long now = Instant.now().getEpochSecond();
            long exp = Instant.now().plusSeconds(ttlSeconds).getEpochSecond();
            String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String payload = "{"
                    + "\"sub\":" + user.getId() + ","
                    + "\"typ\":\"" + type + "\","
                    + "\"roles\":" + stringArray(user.getRoleNames()) + ","
                    + "\"garageIds\":" + user.getGarageIds() + ","
                    + "\"iat\":" + now + ","
                    + "\"exp\":" + exp
                    + "}";
            String unsigned = encode(header) + "." + encode(payload);
            return unsigned + "." + sign(unsigned);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot create token");
        }
    }

    private String parsePayload(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid token");
            }
            String unsigned = parts[0] + "." + parts[1];
            if (!MessageDigest.isEqual(sign(unsigned).getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid token signature");
            }
            return new String(DECODER.decode(parts[1]), StandardCharsets.UTF_8);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
    }

    private String stringArray(java.util.List<String> values) {
        return values.stream()
                .map(value -> "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String stringClaim(String payload, String name) {
        Matcher matcher = Pattern.compile("\\\"" + name + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").matcher(payload);
        if (!matcher.find()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        return matcher.group(1);
    }

    private long longClaim(String payload, String name) {
        Matcher matcher = Pattern.compile("\\\"" + name + "\\\"\\s*:\\s*(\\d+)").matcher(payload);
        if (!matcher.find()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        return Long.parseLong(matcher.group(1));
    }

    private String encode(String value) {
        return ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String unsigned) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return ENCODER.encodeToString(mac.doFinal(unsigned.getBytes(StandardCharsets.UTF_8)));
    }
}
