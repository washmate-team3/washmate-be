package swp391.carwash.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VnpayIpnResponse(
        @JsonProperty("RspCode") String rspCode,
        @JsonProperty("Message") String message
) {
    public static VnpayIpnResponse of(String rspCode, String message) {
        return new VnpayIpnResponse(rspCode, message);
    }
}
