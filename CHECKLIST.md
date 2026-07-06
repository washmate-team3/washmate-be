# WashMate BE — Checklist chức năng chi tiết

> Cập nhật 07/2026 — sau đợt fix bảo mật/logic.
> Scope hệ thống: **1 chủ (OWNER) quản lý nhiều garage**. Role sử dụng thực tế: **CUSTOMER, STAFF, OWNER**.
> Promotion nằm ngoài scope phân công — không đánh giá ở đây.
> ✅ = đã có/đã fix, ⚠️ = nên cải thiện, ❌ = chưa có.

---

## 1. Auth / RBAC / JWT

### Đăng ký & xác thực
- [x] ✅ Đăng ký CUSTOMER + OTP verify (email), chặn đăng ký role khác qua public API
- [x] ✅ Re-register cho tài khoản PENDING_VERIFY (ghi đè info, gửi lại OTP)
- [x] ✅ Check trùng email + phone (kể cả phone thuộc tài khoản pending khác)
- [x] ✅ Login bằng email hoặc phone
- [x] ✅ Google OAuth login (verify idToken, auto-create user ACTIVE)
- [x] ✅ Forgot/reset password qua OTP, revoke toàn bộ refresh token sau reset
- [x] ✅ Change password (check old password, chặn tài khoản Google không có password)
- [x] ✅ Account lockout: khóa 15 phút sau 5 lần login sai (configurable)
- [x] ✅ OTP: resend cooldown 60s, secure random, **max 5 lần verify sai → xóa OTP** (đã có sẵn trong OtpService)
- [x] ✅ **[FIXED]** Rate limit theo IP cho login/register/OTP/forgot-reset password — `AuthRateLimitFilter` mới, 10 req/60s/IP (config `washmate.security.ratelimit.*`), tắt được trong test

### JWT & Token
- [x] ✅ Access + refresh token tách biệt bằng claim `typ`, validate loại token
- [x] ✅ Secret ≥ 32 ký tự, TTL validate lúc khởi động
- [x] ✅ Refresh token: hash SHA-256 lưu DB, rotation + revoke chain
- [x] ✅ Revoke all tokens khi đổi/reset password; scheduler dọn token hết hạn
- [x] ✅ Claims chứa `roles` + `garageIds` phục vụ phân quyền STAFF theo garage
- [x] ✅ **[FIXED]** Reuse detection: dùng lại refresh token đã revoke → **thu hồi toàn bộ phiên của user** (chạy trong transaction riêng REQUIRES_NEW để không bị rollback) + test mới
- [ ] ⚠️ `garageIds` trong JWT → đổi phân công garage của STAFF chỉ có hiệu lực khi token hết hạn (chấp nhận được với access token 60 phút; giảm TTL nếu cần)

### RBAC (3 role: CUSTOMER / STAFF / OWNER)
- [x] ✅ OWNER: toàn quyền mọi garage; STAFF: chỉ garage được phân công; CUSTOMER: chỉ tài nguyên của mình
- [x] ✅ **[FIXED]** Logic phân quyền garage gom về 1 nơi: `GarageAccessEvaluator` (bỏ duplicate giữa BookingService/PaymentService)
- [x] ✅ `@EnableMethodSecurity` + `@PreAuthorize` cho insight/analytics/reward/vehicle/slot
- [x] ✅ CORS whitelist origin, stateless session, error 401/403 JSON chuẩn
- [ ] ⚠️ Enum còn ADMIN/MANAGER (giữ tương thích dữ liệu) — nếu chắc chắn không dùng, dọn dần ở DB + code
- [ ] ❌ Audit log cho hành động quản trị (khóa user, đổi trạng thái…)

### Test
- [x] ✅ JwtService, JwtAuthenticationFilter, AuthService, TokenService (2 file), OAuth/abuse, AuthenticationAuthorization integration test
- [x] ✅ **[NEW]** Test reuse detection refresh token

---

## 2. Booking Core

### Tạo booking
- [x] ✅ Chỉ CUSTOMER tạo; validate garage/slot/service/vehicle ACTIVE + thuộc đúng garage/user
- [x] ✅ Pessimistic lock slot + check capacity theo trạng thái chiếm chỗ
- [x] ✅ Tự tạo Payment PENDING kèm expiresAt; booking code unique; notification in-app
- [x] ✅ **[FIXED]** Validate `bookingDate`: chặn ngày quá khứ, chặn đặt xa quá `washmate.booking.max-advance-days` (mặc định 30 ngày)
- [x] ✅ **[FIXED]** Chặn double booking: 1 user không thể có 2 booking đang hoạt động trên cùng slot + ngày (`existsActiveBookingForUserAndSlot`, áp cho cả create và update)

### Vòng đời booking (state machine)
- [x] ✅ PENDING → CONFIRMED → CHECKED_IN → WASHING → COMPLETED; REJECTED/CANCELLED/NO_SHOW
- [x] ✅ Mỗi transition validate trạng thái nguồn + phân quyền garage
- [x] ✅ COMPLETED yêu cầu payment PAID; tích điểm loyalty khi hoàn tất
- [x] ✅ Reject/Cancel tự cancel payment PENDING + ghi PaymentTransaction; chặn cancel booking đã PAID
- [x] ✅ **[FIXED]** NO_SHOW giờ cũng cancel payment PENDING kèm theo + ghi transaction
- [x] ✅ **[FIXED]** Notification khi update booking: sửa text sai ("đã hủy bỏ" → "đã cập nhật")
- [x] ✅ Timestamps đầy đủ; BookingReminderScheduler nhắc lịch

### Truy vấn & phân quyền đọc
- [x] ✅ `/api/bookings/me`; `/api/bookings` phân trang + filter, tự scope theo garageIds với STAFF
- [x] ✅ Batch load payment/invoice tránh N+1; read chỉ cho chủ booking hoặc nhân sự garage

### Test
- [x] ✅ BookingServiceTest, BookingControllerTest, OwnershipAuthorization test (đã cập nhật theo constructor mới)
- [ ] ⚠️ Bổ sung test concurrency slot capacity (2 request tranh slot cuối)

---

## 3. Payment

### Payment nội bộ (CASH/manual)
- [x] ✅ Confirm: chỉ nhân sự garage, chỉ PENDING, amount khớp finalAmount, chặn confirm tay VNPAY
- [x] ✅ Fail/Cancel payment PENDING; Refund chỉ PAID → invoice REFUNDED + rollback loyalty
- [x] ✅ Idempotency provider+providerTxnId (pre-check + DB constraint); pessimistic lock
- [x] ✅ PaymentTransaction audit trail; settlement tập trung (`PaymentSettlementService`) + event gửi email invoice

### VNPAY
- [x] ✅ Config validate lúc startup; tạo URL chỉ cho chủ booking; reuse attempt PENDING; merchantTxnRef unique
- [x] ✅ IPN: verify HMAC + tmnCode + amount, chống double-confirm, duplicate providerTxnId, raw response sanitized, transaction riêng, trả đúng mã VNPAY
- [x] ✅ Timeout scheduler; cancel attempt thừa khi 1 attempt thành công
- [x] ✅ **[FIXED]** Fallback settle qua return URL giờ nằm sau config `washmate.payment.vnpay.return-fallback-enabled` — **mặc định TẮT ở production**, chỉ bật ở `application-local.properties`; log chuẩn thay System.err
- [x] ✅ **[FIXED]** `VnpayTestController` gắn `@Profile("local")` — không còn expose ở production
- [ ] ❌ VNPAY refund API chưa implement (đang chặn bằng CONFLICT — refund tay)
- [ ] ❌ Reconciliation job đối soát VNPAY (querydr) cho case IPN không tới

### Test
- [x] ✅ Đã có sẵn: PaymentServiceTest (11 test), VnpayServiceTest (11 test: IPN sai chữ ký/sai amount/double IPN/duplicate txn), VnpaySignerTest, VnpayPaymentTimeoutSchedulerTest — các file khởi tạo service đã cập nhật theo constructor mới

---

## 4. AutoWash Insight

### Rule engine
- [x] ✅ 5 nhóm rule (Revenue/Order/Customer/Loyalty/Service) + so sánh kỳ trước
- [x] ✅ Ngưỡng rule configurable + API update cho OWNER
- [x] ✅ Upsert theo (ruleCode, fromDate, toDate) — chạy lại không trùng
- [x] ✅ Severity/priority sort, status lifecycle, xử lý INSUFFICIENT_DATA
- [x] ✅ Scheduler daily 00:30 + monthly ngày 1, có try/catch + log
- [x] ✅ Insight toàn hệ thống là **đúng thiết kế** (1 owner nhiều garage — không phải multi-tenant)

### AI enrichment (Gemini)
- [x] ✅ Enrich + cache + regenerate riêng; lưu model/prompt version/confidence
- [x] ✅ AI chat + health check; toàn bộ endpoint `@PreAuthorize` OWNER
- [ ] ⚠️ Gemini call: cân nhắc timeout/retry + giới hạn tần suất regenerate (quota)
- [ ] ⚠️ `InsightAIController` có 4 alias path — chốt 1 path chuẩn
- [ ] ⚠️ Scheduler chạy multi-instance sẽ trùng — thêm ShedLock nếu deploy > 1 node

### Test
- [x] ✅ Coverage tốt: RuleEngine + 5 rule tests, AIInsightService, AIChat, Validator, GeminiClient, OwnerInsightController, InsightService, InsightRuleConfig

---

## Tổng kết đợt fix (07/2026)

| # | Fix | File chính |
|---|-----|-----------|
| 1 | Guard VNPAY test endpoint bằng `@Profile("local")` | `VnpayTestController` |
| 2 | Fallback IPN qua return URL sau config flag, mặc định tắt | `VnpayService`, `VnpayProperties`, `application*.properties` |
| 3 | Rate limit IP cho auth endpoints (10 req/60s) | `AuthRateLimitFilter` (mới) |
| 4 | Refresh token reuse detection → revoke toàn bộ phiên | `TokenService` + test mới |
| 5 | Validate bookingDate (quá khứ / >30 ngày) | `BookingService` |
| 6 | Chặn double booking cùng user + slot + ngày | `BookingService`, `BookingRepository` |
| 7 | NO_SHOW cancel payment PENDING | `BookingService` |
| 8 | Sửa text notification update booking | `BookingService` |
| 9 | Gom phân quyền garage về `GarageAccessEvaluator` | mới + `BookingService`, `PaymentService` |
| 10 | Tắt rate limit trong test profile | `src/test/resources/application.properties` |

**Lưu ý verify:** sandbox không truy cập được Maven Central nên chưa chạy được `mvnw test` ở đây.
Chạy trên máy bạn để xác nhận: `./mvnw test`
Các test đã được cập nhật theo constructor mới: `BookingServiceTest`, `PaymentServiceTest`, `TokenServiceTest`, `TokenServiceSecurityLifecycleTest`, `OwnershipAuthorizationServiceSecurityTest`.
