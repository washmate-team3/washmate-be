package swp391.carwash.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import swp391.carwash.common.exception.ApiException;

@Service
public class GoogleAuthService {

    private final GoogleIdTokenVerifier verifier;
    private final String clientId;

    public GoogleAuthService(@Value("${washmate.security.oauth2.google.client-id:}") String clientId) {
        this.clientId = clientId;
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    public GoogleIdToken.Payload verifyIdToken(String idTokenString) {
        if (!StringUtils.hasText(clientId)) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Google login is not configured");
        }

        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid Google ID token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Google email is not verified");
            }
            return payload;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Failed to verify Google ID token");
        }
    }
}
