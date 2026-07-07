# Code Review — WashMate BE (Senior → Intern)

> Review 4 chức năng: Auth/RBAC/JWT, Booking Core, Payment/Invoice, AutoWash Insight + đánh giá CI/CD.
> Mức độ: 🔴 phải sửa (bug/lỗ hổng) · 🟡 nên sửa (chất lượng) · 🟢 gợi ý (học hỏi thêm)

## Nhận xét tổng quan

Trước tiên phải nói thẳng: với level intern, đây là codebase **trên mức mong đợi**. Những thứ nhiều dev 2-3 năm kinh nghiệm vẫn làm sai thì em làm đúng: dùng `BigDecimal` cho tiền, pessimistic lock chống race condition, hash refresh token trước khi lưu DB, idempotency cho payment transaction, state machine rõ ràng cho booking, IPN verify chữ ký đầy đủ. Kiến trúc phân lớp Controller → Service → Repository nhất quán, DTO tách khỏi entity đàng hoàng.

Điểm yếu chung không nằm ở "không biết làm" mà ở **độ nhất quán và những góc khuất vận hành thực tế**: timezone, chuẩn hóa API, error contract, và đặc biệt là chưa có CI/CD. Đó là những thứ phân biệt "code chạy được" với "hệ thống chạy được trong production" — và là đúng những gì em nên tập trung học ở giai đoạn này.

---

## 1. Auth / RBAC / JWT — Điểm: 8/10

### Điểm tốt (giữ vững)
- Refresh token: hash SHA-256, rotation, revoke chain, reuse detection thu hồi toàn bộ phiên — đây là mức "đúng chuẩn OAuth server", nhiều hệ thống thật còn thiếu.
- Account lockout + OTP max attempts + rate limit IP: 3 lớp chống brute-force bổ trợ nhau đúng cách.
- Reuse detection chạy trong `REQUIRES_NEW` transaction để không bị rollback — em hiểu transaction semantics, tốt.
- `PublicPaths` tập trung 1 nơi cho cả filter và config — tránh drift, đúng nguyên tắc single source of truth.

### 🔴 Phải sửa
1. **Lỗ hổng `/api/v1/admin/**`** (đã ghi trong CHECKLIST): SecurityConfig chỉ match `/api/admin/**`. Hai controller loyalty mới dùng `/api/v1/admin/**` → CUSTOMER gọi được. Dù controller đó của teammate, **SecurityConfig là của em** — người giữ security config phải enforce quy ước path. Fix: thêm matcher `/api/v1/admin/**` và ra quy ước với team: "mọi endpoint admin phải nằm dưới 1 prefix duy nhất".
2. **`InvoiceService` vẫn còn bản copy `canOperateGarage` riêng** — em đã tách `GarageAccessEvaluator` nhưng sót file này. Copy-paste authorization logic là loại nợ kỹ thuật nguy hiểm nhất: sau này sửa rule ở 1 chỗ, quên chỗ kia là thành lỗ hổng.

### 🟡 Nên sửa
3. **Error message lẫn lộn 2 ngôn ngữ**: "Tài khoản hoặc mật khẩu không chính xác" (VN) vs "Invalid refresh token" (EN). Client không thể hiển thị nhất quán. Chuẩn thực tế: API trả về `errorCode` (machine-readable, ví dụ `AUTH_INVALID_CREDENTIALS`) + message; FE tự quyết định ngôn ngữ hiển thị. Học từ khóa: *error code contract, i18n*.
4. **JWT chứa `garageIds`** → STAFF bị đổi phân công vẫn giữ quyền cũ tới khi token hết hạn. Access token 60 phút là dài cho rủi ro này. Đề xuất: giảm còn 15 phút, hoặc load garageIds từ DB trong filter (em đã load user từ DB mỗi request rồi — thêm garageIds gần như miễn phí).
5. **Login không phân biệt "sai mật khẩu" và "user không tồn tại" trong timing**: `findUserByIdentifier` fail thì trả 401 ngay, còn user tồn tại thì phải chạy BCrypt (~100ms). Attacker đo thời gian phản hồi đoán được email nào tồn tại (*timing attack / user enumeration*). Mức đồ án chấp nhận được, nhưng nên biết khái niệm này.

### 🟢 Gợi ý học thêm
- Đọc về **OWASP ASVS** chương Authentication — đối chiếu từng mục với code của mình là bài tập rất tốt.
- Audit log cho hành động nhạy cảm (đổi password, khóa user) — thực tế mọi hệ thống có compliance đều bắt buộc.

---

## 2. Booking Core — Điểm: 7.5/10

### Điểm tốt
- State machine chặt: mỗi transition validate trạng thái nguồn + quyền, không cho nhảy cóc. `requireStatus` helper gọn.
- Chống race condition slot bằng `findByIdForUpdate` + count trong cùng transaction — đúng bài.
- Validate quan hệ chéo đầy đủ (slot thuộc garage, vehicle thuộc user...) — nhiều người quên loại check này và bị IDOR.
- Defense in depth: `@FutureOrPresent` ở DTO + `validateBookingDate` ở service.

### 🔴 Phải sửa
1. **`BookingService` hiện không compile** (`calculateDiscount` chưa định nghĩa — code teammate). Không phải lỗi của em, nhưng bài học cho cả team: **không push code không compile lên branch chung**. Đây chính là lý do cần CI (xem phần cuối).

### 🟡 Nên sửa
2. **`BookingService` đang ~570 dòng, làm quá nhiều việc**: booking lifecycle + tạo payment + soạn notification + (giờ thêm) promotion. Vi phạm Single Responsibility. Đề xuất tách dần:
   - `BookingNotificationComposer` — gom 5 đoạn `notificationRepository.save(...)` lặp gần giống nhau thành 1 method `notify(booking, NotificationKind kind)`.
   - Việc tạo Payment khi createBooking → chuyển sang `PaymentService.createPendingFor(booking)`.
   Nguyên tắc: service dài quá ~300 dòng là tín hiệu nên tách.
3. **Notification `type` luôn là `"BOOKING_CONFIRMATION"`** kể cả khi hủy, từ chối, đang rửa — field `type` mất ý nghĩa, FE không filter được. Dùng enum: `BOOKING_CREATED / CONFIRMED / REJECTED / CANCELLED / WASHING / ...`. Và các magic string `"IN_APP"`, `"PENDING"` cũng nên là enum — em đã có thói quen enum tốt ở chỗ khác (BookingStatus...) mà chỗ này lại buông.
4. **`getMyBookings` trả toàn bộ không phân trang** — khách đặt 200 lần là payload phình. `/api/bookings` đã có `Pageable` rồi, làm tương tự.
5. **`updateBooking` quá tham vọng**: cho đổi cả garage + slot + service + vehicle + date trong 1 PUT ~90 dòng. Thực tế nghiệp vụ rửa xe: đổi garage gần như = hủy đặt lại. Đề xuất thu hẹp: chỉ cho đổi slot/date (reschedule), còn lại bắt hủy + tạo mới. Ít code hơn, ít bug hơn, nghiệp vụ rõ hơn. *Bài học: API surface càng hẹp càng dễ đúng.*
6. **Double-booking check chỉ chặn cùng slot** — user vẫn đặt được 2 slot khác nhau cùng giờ ở 2 garage. Cân nhắc mức nghiệp vụ có cần chặn không (có thể không — 1 người có thể đặt cho 2 xe). Quan trọng là *quyết định có chủ đích và ghi lại*, thay vì để ngỏ.

### 🟢 Gợi ý
- Viết **state diagram** cho booking lifecycle vào README/docs — reviewer và FE đọc 1 hình hiểu ngay, đỡ đọc 570 dòng code.
- Test concurrency: 2 thread cùng đặt slot cuối. Học từ khóa: *Testcontainers + parallel test*.

---

## 3. Payment / Invoice — Điểm: 8.5/10 (phần tốt nhất của em)

### Điểm tốt
- IPN handling gần như textbook: verify HMAC → verify tmnCode → verify amount → chống double-confirm → duplicate providerTxnId → transaction riêng → trả đúng mã VNPAY. Thêm sanitize raw response trước khi lưu. Rất ít đồ án làm được mức này.
- `PaymentSettlementService` tách riêng để cả manual confirm lẫn IPN dùng chung — đúng nguyên tắc DRY chỗ *cần* DRY (logic tiền bạc).
- Idempotency 2 lớp: pre-check + catch `DataIntegrityViolationException` từ unique constraint — hiểu rằng check-then-act không đủ.
- Refund rollback loyalty + cập nhật invoice + xử lý booking theo trạng thái — nghiệp vụ nghĩ đủ chiều.

### 🔴 Phải sửa
1. **Timezone bug tiềm ẩn trong `InvoiceService.startOfDay`**: dùng `ZoneId.systemDefault()` trong khi JPA config là UTC và VNPAY dùng `Asia/Ho_Chi_Minh`. Server deploy ở region khác (Supabase/cloud thường UTC) → filter invoice theo ngày bị lệch 7 tiếng: hóa đơn 1h sáng ngày 2 rơi vào báo cáo ngày 1. **Fix: dùng constant `ZoneId.of("Asia/Ho_Chi_Minh")` thống nhất toàn hệ thống** (VnpayService đã có sẵn constant này — extract ra `common`). Đây là loại bug production kinh điển: không test nào bắt được trên máy dev vì máy dev ở VN.

### 🟡 Nên sửa
2. **`GlobalExceptionHandler` map `DataAccessException` → 409 CONFLICT** cho *mọi* lỗi DB. Connection timeout, deadlock... cũng thành 409 "Database operation failed" — sai ngữ nghĩa HTTP và che mất lỗi hạ tầng. Đúng: chỉ `DataIntegrityViolationException` → 409, còn lại → 500 + log error (hiện đang log warn — lỗi DB phải là error).
3. **Validation error chỉ trả field lỗi đầu tiên** (`findFirst`). Form 5 field sai thì user phải submit 5 lần. Trả về mảng `errors: [{field, message}]`.
4. **VNPAY refund chưa có + không có reconciliation**: hiện chấp nhận được (đã chặn bằng CONFLICT rõ ràng), nhưng em nên viết 1 đoạn docs "quy trình refund tay" — vận hành thật cần biết làm gì khi khách đòi tiền. *Bài học: khi không làm feature, hãy làm quy trình thay thế.*
5. **Payment timeout scheduler + rate limiter đều là in-memory/single-instance** — scale ngang là hỏng (2 node cùng quét timeout, rate limit tính riêng mỗi node). Chưa cần fix cho đồ án, nhưng thêm comment `// NOTE: single-instance only` vào 2 chỗ đó để người sau không ngã.

### 🟢 Gợi ý
- Học khái niệm **outbox pattern** — email invoice hiện publish event async, mail fail là mất luôn. Outbox là câu trả lời chuẩn công nghiệp.
- Đọc thêm về **reconciliation job** (đối soát cuối ngày với querydr API của VNPAY) — phỏng vấn fintech chắc chắn hỏi.

---

## 4. AutoWash Insight — Điểm: 8/10

### Điểm tốt
- Rule engine thiết kế mở rộng được: interface `InsightRule`, 5 rule độc lập, ngưỡng configurable qua DB, context so sánh kỳ trước — đây là design pattern thật (Strategy), không phải if-else dài.
- Upsert theo `(ruleCode, fromDate, toDate)` → scheduler chạy lại an toàn (idempotent) — em chú thích rõ điều này trong Javadoc của scheduler, rất tốt.
- AI enrichment có validator riêng cho response Gemini + lưu `promptVersion`/`confidenceScore` — hiểu rằng LLM output không tin được và cần traceability.
- Test coverage phần này tốt nhất repo.

### 🟡 Nên sửa
1. **4 alias path cho cùng 1 endpoint AI chat** (`/ai-chat`, `/ai/chat`, `/chat`, `/api/owner/ai-chat`) — dấu hiệu "FE đổi ý thì BE thêm path". Chốt 1 path, giữ 1 alias deprecated nếu cần, xóa phần còn lại. API là contract, không phải menu.
2. **`enrichInsight`/`regenerateInsight` không giới hạn tần suất** — nút "regenerate" bị spam là đốt quota Gemini (tiền thật). Thêm cooldown đơn giản: check `updatedAt` của enrichment < X phút thì trả 429.
3. **Gemini call chưa thấy timeout/retry rõ ràng** — external API phải luôn có timeout. Nếu `GeminiClient` dùng RestTemplate/WebClient mặc định, connection treo là request treo. Set connect/read timeout 10-30s.
4. **Scheduler không có lock đa instance** — như payment scheduler, thêm comment hoặc ShedLock.

### 🟢 Gợi ý
- Thêm **cost tracking**: đếm số call + token vào bảng nhỏ. Chủ garage (và giảng viên chấm điểm) sẽ ấn tượng với "chúng em kiểm soát chi phí AI".

---

## 5. Những vấn đề CẮT NGANG toàn hệ thống (quan trọng hơn từng feature)

1. 🟡 **API path không nhất quán**: `/api/bookings`, `/api/v1/garages`, `/api/v1/promotion`, `/api/auth`, `/api/owner/insights`, `/api/v1/admin/...`. Có version và không version lẫn lộn; số ít/số nhiều lẫn lộn (`promotion` vs `bookings`). Đây là thứ lộ ra ngay khi giảng viên mở Swagger. Chốt quy ước: `/api/v1/<resource-số-nhiều>` cho tất cả — làm 1 buổi với cả team.
2. 🟡 **Error response chưa có `errorCode`** — chỉ có message. FE phải string-match message để phân nhánh xử lý (giòn, vỡ khi đổi text). Thêm field `code` vào error body của `GlobalExceptionHandler` + `SecurityErrorResponseWriter`.
3. 🟡 **Timezone chưa có chiến lược thống nhất** (như mục Payment #1): quy ước đề xuất — lưu UTC, hiển thị và tính "ngày" theo `Asia/Ho_Chi_Minh`, cấm `systemDefault()` (thêm rule vào code review checklist của team).
4. 🟢 **Logging thiếu correlation id** — khi 2 request lỗi cùng lúc, log trộn lẫn không tách được. Spring có sẵn giải pháp: filter gán `X-Request-Id` vào MDC. ~20 dòng code, giá trị debug rất lớn.

---

## 6. CI/CD — Điểm hiện tại: 0/10, và đó là cơ hội

### Hiện trạng
Repo **chưa có gì**: không GitHub Actions, không Dockerfile, không branch protection. Hậu quả đã thấy ngay hôm nay: teammate push code không compile (`calculateDiscount`) lên branch chung và không ai biết cho đến khi người khác pull về. CI tồn tại chính xác để chặn việc này.

### Lộ trình đề xuất — làm theo 3 giai đoạn, đừng nhảy cóc

**Giai đoạn 1 — CI cơ bản (làm ngay, ~1 giờ):** build + test mỗi PR.

Tạo `.github/workflows/ci.yml`:

```yaml
name: CI
on:
  pull_request:
  push:
    branches: [main, "integration/**"]

jobs:
  build-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21
          cache: maven
      - name: Build & test
        run: ./mvnw -B verify
      - name: Upload test report khi fail
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: surefire-reports
          path: target/surefire-reports/
```

Lưu ý: test profile của em đã dùng H2 + tắt rate limit nên chạy được trên CI không cần DB thật — em đã vô tình chuẩn bị tốt cho CI mà không biết. `PostgresMigrationTest` dùng Testcontainers thì CI Ubuntu có sẵn Docker, chạy được luôn.

Kèm theo: bật **branch protection** trên GitHub (Settings → Branches): require PR + require CI pass trước khi merge vào `main`. Từ đó "code không compile trên branch chung" không thể xảy ra nữa.

**Giai đoạn 2 — Chất lượng (tuần sau đó):**
- Thêm bước `mvn spotless:check` hoặc Checkstyle — chặn tranh cãi format code.
- Thêm **gitleaks** action — chặn commit credential (bài học Resend key vừa rồi).
- Coverage report bằng JaCoCo, để badge trong README (nhìn số % tăng dần cũng là động lực).

**Giai đoạn 3 — CD (khi cần demo liên tục):**
- Viết `Dockerfile` multi-stage (build bằng Maven image → run bằng JRE 21 slim, ~15 dòng).
- Deploy tự động lên Render/Railway/Fly.io (free tier đủ cho đồ án) mỗi khi merge `main`; secrets đặt trong GitHub Environments, khớp với các biến `.env` em đã chuẩn hóa.
- Health check endpoint em đã có (`/actuator/health`) — dùng làm readiness probe luôn.

### Học gì từ đây
Thứ tự học: *GitHub Actions cơ bản → Docker → deploy 1 platform PaaS → (sau này) Kubernetes*. Đừng học ngược. Mỗi giai đoạn ở trên là 1 dòng trong CV: "Set up CI pipeline with automated testing, secret scanning, and containerized deployment" — với intern, câu đó có trọng lượng thật sự.

---

## Tổng kết — việc cần làm theo thứ tự

| # | Việc | Mức | Thuộc | Trạng thái |
|---|------|-----|-------|-----------|
| 1 | Chặn `/api/v1/admin/**` trong SecurityConfig | 🔴 | Auth | ✅ Fixed — matcher `/api/*/admin/**` bắt mọi version prefix |
| 2 | Sửa `InvoiceService` dùng `GarageAccessEvaluator` | 🔴 | Payment/Invoice | ✅ Fixed — kèm cập nhật 2 test |
| 3 | Fix timezone `systemDefault()` → `Asia/Ho_Chi_Minh` constant | 🔴 | Payment/Invoice | ✅ Fixed — thêm `common/TimeZones`, dùng ở InvoiceService + VnpayService |
| 4 | Dựng CI giai đoạn 1 + branch protection | 🔴 | CI/CD | ⏸ Để sau theo yêu cầu |
| 5 | `DataAccessException` → 500 (chỉ integrity → 409); trả đủ validation errors | 🟡 | Cross-cutting | ✅ Fixed — handler riêng cho `DataIntegrityViolationException`, validation trả mảng `errors` |
| 6 | Thêm `errorCode` vào error contract | 🟡 | Cross-cutting | ✅ Fixed — field `code` (additive) ở cả GlobalExceptionHandler + SecurityErrorResponseWriter; `ApiException` có overload nhận code |
| 7 | Tách notification composer + enum type khỏi BookingService | 🟡 | Booking | ⏸ Hoãn — BookingService đang bị teammate sửa dở (promotion WIP), tránh conflict |
| 8 | Phân trang `getMyBookings`; thu hẹp `updateBooking` | 🟡 | Booking | ⏸ Hoãn — breaking change với FE + cùng lý do #7 |
| 9 | Cooldown regenerate AI + timeout Gemini client | 🟡 | Insight | ✅ Fixed cooldown 60s (429) — timeout Gemini hóa ra ĐÃ CÓ SẴN (connect 10s/read 30s), điểm cộng |
| 10 | Chốt quy ước API path `/api/v1/...` với team | 🟡 | Cross-cutting | ⏳ Việc của cả team — cần 1 buổi thống nhất |
| 11 | Request-Id logging, outbox email, reconciliation VNPAY | 🟢 | Học thêm | ⏳ |

**Lời cuối:** em đang làm đúng những phần khó nhất của hệ thống (tiền, bảo mật, state machine) và làm khá chắc tay. Khoảng cách còn lại giữa em và mid-level không phải là viết thêm feature — mà là những mục 🟡 phía trên: nhất quán, error contract, vận hành, và CI/CD. Ưu tiên mục 1-4 tuần này.
