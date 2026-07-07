package swp391.carwash.common.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String message) {
        this(status, null, message);
    }

    /**
     * @param code mã lỗi machine-readable cho FE phân nhánh (vd: AUTH_ACCOUNT_LOCKED,
     *             BOOKING_SLOT_FULL). Null thì handler tự sinh từ HTTP status.
     */
    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
