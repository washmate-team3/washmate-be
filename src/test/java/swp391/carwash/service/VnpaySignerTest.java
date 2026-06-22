package swp391.carwash.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VnpaySignerTest {
    private final VnpaySigner signer = new VnpaySigner();

    @Test
    void canonicalizeSortsAndEncodesParameters() {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("vnp_TxnRef", "P100");
        parameters.put("vnp_OrderInfo", "Thanh toan booking BKG-1");
        parameters.put("vnp_Amount", "5000000");

        assertEquals(
                "vnp_Amount=5000000&vnp_OrderInfo=Thanh+toan+booking+BKG-1&vnp_TxnRef=P100",
                signer.canonicalize(parameters));
    }

    @Test
    void verifyAcceptsOnlyMatchingHmacSha512() {
        Map<String, String> parameters = Map.of("vnp_Amount", "5000000", "vnp_TxnRef", "P100");
        String signature = signer.sign(signer.canonicalize(parameters), "test-secret");

        assertTrue(signer.verify(parameters, signature, "test-secret"));
        assertFalse(signer.verify(parameters, signature, "wrong-secret"));
        assertFalse(signer.verify(parameters, "", "test-secret"));
    }
}
