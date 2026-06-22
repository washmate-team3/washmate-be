# Tài liệu Đặc tả Cơ sở Dữ liệu Auto Wash Pro (Phiên bản V4 hoàn chỉnh)

Tài liệu này mô tả chi tiết hệ thống cơ sở dữ liệu **Auto Wash Pro** chạy trên nền tảng **Supabase / PostgreSQL 14+**. Hệ thống quản lý toàn diện các nghiệp vụ: Đặt lịch (Booking), Công suất khung giờ (Slot Capacity), Khách hàng thân thiết (Loyalty & Reward), Khuyến mãi (Promotion), Thanh toán & Hóa đơn (Payment & Invoice), Phạt & Sự cố (Penalty & Incident), Thông báo (Notification), và Phân tích số liệu (Analytics).

---

## 1. Sơ đồ Quan hệ & Cấu trúc Tổng quan (ERD Logical Flow)
Các nhóm bảng dữ liệu được liên kết chặt chẽ qua cơ chế khóa ngoại và ràng buộc chặt:
1. **Master Data & Phân quyền**: `garage`, `app_user`, `role`, `user_role`
2. **Xe & Khung giờ đặt lịch**: `vehicle`, `booking_slot`, `service_package`
3. **Nghiệp vụ Đặt lịch Core**: `booking`, `booking_slot_capacity`
4. **Tích điểm & Hạng thành viên**: `loyalty_policy`, `membership_tier`, `loyalty_account`, `loyalty_transaction`, `loyalty_tier_history`
5. **Khuyến mãi & Phần thưởng**: `promotion`, `booking_promotion`, `reward`, `reward_redemption`
6. **Tài chính & Phát sinh**: `payment`, `payment_transaction`, `invoice`, `penalty_fee`, `service_incident`
7. **Thông báo & Phân tích định kỳ**: `notification`, `booking_behavior_monthly`, `customer_segment_monthly`

---

## 2. Chi tiết các Bảng Dữ liệu (Tables Specification)

### 2.1 Nhóm Master Data & Phân quyền (RBAC)

#### Bảng `garage` (Quản lý chi nhánh/garage)
* **garage_id** (`SERIAL`, PRIMARY KEY): Mã định danh garage.
* **name** (`VARCHAR(255)`, NOT NULL): Tên chi nhánh.
* **address** (`VARCHAR(500)`, NOT NULL): Địa chỉ chi nhánh.
* **phone** (`VARCHAR(20)`, NOT NULL): Số điện thoại liên hệ.
* **status** (`VARCHAR(20)`, DEFAULT 'ACTIVE'): Trạng thái chi nhánh (`ACTIVE`, `INACTIVE`, `DELETED`).
* **created_at** (`TIMESTAMPTZ`, DEFAULT NOW()): Thời gian tạo.
* **updated_at** / **deleted_at** (`TIMESTAMPTZ`): Thời gian cập nhật / xóa tạm.

#### Bảng `app_user` (Quản lý người dùng hệ thống)
* **user_id** (`SERIAL`, PRIMARY KEY): Mã định danh người dùng.
* **email** (`VARCHAR(255)`, UNIQUE): Địa chỉ email.
* **password_hash** (`VARCHAR(255)`): Chuỗi mã hóa mật khẩu.
* **full_name** (`VARCHAR(255)`, NOT NULL): Họ và tên đầy đủ.
* **phone** (`VARCHAR(20)`, NOT NULL, UNIQUE): Số điện thoại (dùng đăng nhập).
* **status** (`VARCHAR(20)`, DEFAULT 'ACTIVE'): Trạng thái tài khoản (`ACTIVE`, `INACTIVE`, `BLOCKED`, `DELETED`).
* **created_at** / **updated_at** / **deleted_at** (`TIMESTAMPTZ`).

#### Bảng `role` (Quản lý vai trò chính)
* **role_id** (`SERIAL`, PRIMARY KEY): Mã định danh vai trò.
* **role_name** (`VARCHAR(50)`, UNIQUE): Tên vai trò (`CUSTOMER`, `STAFF`, `MANAGER`, `OWNER`, `ADMIN`).
* **description** (`TEXT`): Mô tả nhiệm vụ vai trò.
* **status** (`VARCHAR(20)`, DEFAULT 'ACTIVE'): Trạng thái vai trò (`ACTIVE`, `INACTIVE`).

#### Bảng `user_role` (Bảng trung gian phân quyền người dùng)
* **user_role_id** (`SERIAL`, PRIMARY KEY): Mã bản ghi phân quyền.
* **user_id** (`INT`, REFERENCES `app_user`): Mã người dùng.
* **role_id** (`INT`, REFERENCES `role`): Mã vai trò.
* **garage_id** (`INT`, NULL, REFERENCES `garage`): Mã garage quản lý (nếu là NULL tức vai trò Global hệ thống).
* **status** (`VARCHAR(20)`, DEFAULT 'ACTIVE'): Trạng thái phân quyền (`ACTIVE`, `INACTIVE`).
* *Chỉ mục duy nhất (Unique Index)*:
    * `uq_user_role_per_garage`: Ràng buộc unique theo cặp (`user_id`, `role_id`, `garage_id`) khi `garage_id` không null.
    * `uq_user_role_global`: Ràng buộc unique theo cặp (`user_id`, `role_id`) khi `garage_id` là null.

---

### 2.2 Nhóm Quản lý Xe, Khung giờ và Gói dịch vụ

#### Bảng `vehicle` (Quản lý phương tiện xe của khách)
* **vehicle_id** (`SERIAL`, PRIMARY KEY).
* **user_id** (`INT`, REFERENCES `app_user`): Chủ sở hữu xe.
* **license_plate** (`VARCHAR(30)`, NOT NULL): Biển số xe.
* **brand** / **model** / **color** (`VARCHAR`): Thương hiệu, dòng xe, màu sắc.
* **status** (`VARCHAR(20)`, DEFAULT 'ACTIVE'): Trạng thái hoạt động (`ACTIVE`, `INACTIVE`, `DELETED`).
* *Ràng buộc đặc biệt*: Cặp (`vehicle_id`, `user_id`) là UNIQUE để phục vụ đối chiếu chủ xe tại bảng Booking. Chỉ cho phép duy nhất một biển số xe ở trạng thái `ACTIVE` (`uq_active_vehicle_plate`).

#### Bảng `booking_slot` (Quản lý các khung giờ sửa/rửa xe)
* **slot_id** (`SERIAL`, PRIMARY KEY).
* **garage_id** (`INT`, REFERENCES `garage`): Thuộc chi nhánh nào.
* **start_time** / **end_time** (`TIME`, NOT NULL): Giờ bắt đầu và kết thúc khung giờ (Ràng buộc: `end_time > start_time`).
* **max_capacity** (`INT`, DEFAULT 4): Số lượng xe tối đa phục vụ cùng lúc trong slot này (phải `> 0`).
* **status** (`VARCHAR(20)`, DEFAULT 'ACTIVE'): Trạng thái khung giờ (`ACTIVE`, `INACTIVE`).

#### Bảng `service_package` (Quản lý các gói dịch vụ)
* **service_id** (`SERIAL`, PRIMARY KEY).
* **garage_id** (`INT`, REFERENCES `garage`): Thuộc chi nhánh nào.
* **name** (`VARCHAR(255)`, NOT NULL): Tên gói dịch vụ (Không trùng lặp trong cùng một chi nhánh).
* **description** (`TEXT`): Mô tả quy trình thực hiện.
* **price** (`DECIMAL(10,2)`, NOT NULL): Giá tiền gói dịch vụ (phải `>= 0`).
* **duration** (`INT`): Thời gian thi công ước tính (phút, phải `> 0`).
* **status** (`VARCHAR(20)`, DEFAULT 'ACTIVE'): Trạng thái (`ACTIVE`, `INACTIVE`, `DELETED`).

---

### 2.3 Nhóm Đặt lịch lõi (Booking Core)

#### Bảng `booking` (Thông tin đơn đặt lịch)
* **booking_id** (`SERIAL`, PRIMARY KEY).
* **booking_code** (`VARCHAR(50)`, NOT NULL, UNIQUE): Mã code đơn đặt lịch hiển thị ứng dụng.
* **user_id** (`INT`), **garage_id** (`INT`), **slot_id** (`INT`), **service_id** (`INT`), **vehicle_id** (`INT`): Các khóa ngoại tham chiếu thông tin liên quan (Có kèm ràng buộc phức hợp chéo đảm bảo tính nhất quán dữ liệu chi nhánh).
* **booking_date** (`DATE`, NOT NULL): Ngày đặt lịch hẹn.
* **total_amount** (`DECIMAL(10,2)`): Tổng số tiền gốc dịch vụ.
* **discount_amount** (`DECIMAL(10,2)`, DEFAULT 0.00): Số tiền được giảm giảm trừ.
* **final_amount** (`DECIMAL(10,2)`): Số tiền thực tế phải trả (Ràng buộc: `final_amount = total_amount - discount_amount`).
* **status** (`VARCHAR(30)`, DEFAULT 'PENDING'): Trạng thái đơn đặt lịch (`PENDING`, `CONFIRMED`, `CHECKED_IN`, `WASHING`, `COMPLETED`, `CANCELLED`, `NO_SHOW`).
* **assigned_staff_user_id** (`INT`, NULL, REFERENCES `app_user`): Nhân viên được chỉ định xử lý rửa xe.
* **Dòng thời gian kiểm soát**: `confirmed_at`, `checkin_time`, `service_start_time`, `completed_time`, `cancelled_at`, `no_show_at`.
* *Ràng buộc*: Luồng thời gian logic phải đúng trình tự: `checkin_time >= confirmed_at`, `service_start_time >= checkin_time`, `completed_time >= service_start_time`. Một xe chỉ được đặt tối đa 1 slot hoạt động trong cùng 1 thời điểm ngày (`uq_active_vehicle_booking`).

#### Bảng `booking_slot_capacity` (Theo dõi công suất thực tế theo ngày)
* **slot_id** (`INT`), **booking_date** (`DATE`): Khóa chính phức hợp.
* **garage_id** (`INT`, REFERENCES `garage`).
* **current_capacity** (`INT`, DEFAULT 0): Số lượng slot đã bị đặt trong ngày đó (không được phép `< 0`).
* **updated_at** (`TIMESTAMPTZ`).

---

### 2.4 Nhóm Thành viên thân thiết (Loyalty & Rewards)

#### Bảng `loyalty_policy` (Chính sách quy đổi điểm thưởng từng garage)
* **policy_id** (`SERIAL`, PRIMARY KEY).
* **garage_id** (`INT`, UNIQUE, REFERENCES `garage`): Mỗi chi nhánh cấu hình một chính sách riêng.
* **amount_per_point** (`DECIMAL(10,2)`, DEFAULT 10000.00): Số tiền tiêu dùng để đổi lấy 1 điểm thưởng.
* **point_expiry_months** (`INT`, DEFAULT 12): Thời hạn hết hạn của điểm (tháng).
* **auto_enroll** (`BOOLEAN`, DEFAULT TRUE): Tự động mở tài khoản điểm khi giao dịch.
* **status** (`VARCHAR(20)`, DEFAULT 'ACTIVE').

#### Bảng `membership_tier` (Cấp bậc danh hiệu thành viên)
* **tier_id** (`SERIAL`, PRIMARY KEY).
* **garage_id** (`INT`, REFERENCES `garage`).
* **tier_name** (`VARCHAR(50)`): Tên hạng thẻ (Ví dụ: Bạc, Vàng, Kim Cương).
* **min_points** (`INT`, DEFAULT 0): Điểm tối thiểu để đạt hạng này.
* **discount_percentage** (`DECIMAL(5,2)`, DEFAULT 0.00): Phần trăm giảm giá đặc quyền cho hạng thẻ này (0 đến 100).
* **status** (`VARCHAR(20)`, DEFAULT 'ACTIVE').

#### Bảng `loyalty_account` (Tài khoản điểm của khách hàng)
* **account_id** (`SERIAL`, PRIMARY KEY).
* **user_id** (`INT`), **garage_id** (`INT`): Định danh duy nhất tài khoản điểm của khách tại garage đó.
* **tier_id** (`INT`): Khóa ngoại liên kết chéo cấp bậc thành viên hợp lệ tại chi nhánh.
* **total_points** (`INT`): Tổng điểm tích lũy trọn đời.
* **available_points** (`INT`): Điểm khả dụng hiện tại có thể dùng tiêu tài nguyên (Ràng buộc: `0 <= available_points <= total_points`).

#### Bảng `loyalty_transaction` (Nhật ký giao dịch biến động điểm)
* **transaction_id** (`SERIAL`, PRIMARY KEY).
* **account_id** (`INT`, REFERENCES `loyalty_account`).
* **booking_id** (`INT`, NULL, REFERENCES `booking`): Liên kết nếu điểm sinh ra từ đơn đặt lịch.
* **redemption_id** (`INT`, NULL): Liên kết nếu điểm bị trừ do đổi quà.
* **source_transaction_id** (`INT`, NULL): Gốc giao dịch (dùng cho tính năng xử lý điểm hết hạn).
* **points** (`INT`, NOT NULL): Số điểm biến động (Cộng hoặc trừ, không được phép bằng 0).
* **transaction_type** (`VARCHAR(20)`): Loại biến động (`EARN`, `REDEEM`, `REFUND`, `EXPIRE`, `ADJUSTMENT`, `ROLLBACK`).

#### Bảng `loyalty_tier_history` (Lịch sử thay đổi hạng thành viên)
* Ghi vết chi tiết mọi sự kiện nâng/hạ hạng thẻ của khách hàng (`UPGRADE`, `DOWNGRADE`, `ADJUSTMENT`) cùng lý do hệ thống hoặc người thực hiện điều chỉnh.

---

### 2.5 Nhóm Khuyến mãi & Phần thưởng (Promotions & Rewards)

#### Bảng `promotion` (Quản lý mã giảm giá/Chiến dịch ưu đãi)
* **promotion_id** (`SERIAL`, PRIMARY KEY).
* **garage_id** (`INT`): Chi nhánh áp dụng mã.
* **promo_code** (`VARCHAR(50)`): Mã code giảm giá.
* **discount_value** (`DECIMAL(10,2)`): Giá trị giảm.
* **discount_type** (`VARCHAR(20)`): Kiểu áp dụng (`PERCENTAGE` - phần trăm hoặc `FIXED_AMOUNT` - số tiền cố định).
* **max_discount** / **min_order_value** (`DECIMAL(10,2)`): Mức giảm tối đa và Giá trị đơn hàng tối thiểu để áp dụng.
* **usage_limit** / **used_count** (`INT`): Giới hạn số lần dùng mã và số lần đã dùng thực tế.
* **start_date** / **end_date** (`TIMESTAMPTZ`): Thời gian hiệu lực chiến dịch (`end_date > start_date`).
* **status** (`VARCHAR(20)`): Trạng thái chiến dịch (`ACTIVE`, `INACTIVE`, `EXPIRED`, `DELETED`).

#### Bảng `booking_promotion` (Bảng trung gian lưu vết áp dụng khuyến mãi cho đơn hàng)
* Lưu trữ thông tin chi tiết số tiền được áp dụng giảm trừ cụ thể của từng promo code cho mỗi đơn đặt lịch (`booking_id`, `promotion_id`).

#### Bảng `reward` (Kho quà tặng đổi điểm)
* Quản lý danh sách các vật phẩm hoặc Voucher quà tặng đổi bằng điểm tích lũy tại từng garage. Bao gồm số điểm yêu cầu (`points_required`), số lượng tồn kho (`stock`), và trạng thái (`ACTIVE`, `INACTIVE`, `OUT_OF_STOCK`, `DELETED`).

#### Bảng `reward_redemption` (Tiến trình yêu cầu đổi quà)
* Theo dõi quy trình đổi điểm lấy quà của khách hàng từ trạng thái `PENDING` -> `APPROVED` -> `COMPLETED`, hoặc bị hủy/từ chối (`REJECTED`, `CANCELLED`).

---

### 2.6 Nhóm Tài chính, Hóa đơn, Phạt & Sự cố

#### Bảng `payment` (Yêu cầu thanh toán đơn đặt lịch)
* **payment_id** (`SERIAL`, PRIMARY KEY).
* **booking_id** (`INT`, UNIQUE): Một booking gắn liền với duy nhất một giao dịch thanh toán tổng thể.
* **amount** (`DECIMAL(10,2)`): Số tiền thanh toán.
* **method** (`VARCHAR(30)`): Phương thức (`CASH`, `VNPAY`, `MOMO`, `BANK_TRANSFER`, `CARD`).
* **status** (`VARCHAR(30)`): Trạng thái (`PENDING`, `PAID`, `FAILED`, `CANCELLED`, `REFUNDED`).

#### Bảng `payment_transaction` (Chi tiết lịch sử cổng thanh toán trực tuyến)
* Lưu thông tin log chi tiết phản hồi từ các webhook hoặc IPN cổng trung gian (VNPAY, MoMo,...) qua trường `raw_response` (`JSONB`) phục vụ việc đối soát tài chính tự động.

#### Bảng `invoice` (Hóa đơn tài chính chính thức)
* **invoice_id** / **invoice_code** (`VARCHAR(50)`, UNIQUE).
* **subtotal** (Tiền gói dịch vụ gốc) + **penalty_total** (Tổng tiền phạt phát sinh) - **discount** (Tổng giảm giá) = **total_amount** (Tổng tiền hóa đơn cuối cùng).
* **status** (`VARCHAR(30)`): Trạng thái hóa đơn (`ISSUED`, `PAID`, `CANCELLED`, `REFUNDED`).

#### Bảng `penalty_fee` (Quản lý các khoản phí phạt phát sinh)
* **penalty_type** (`VARCHAR(50)`): Loại hình phạt phát sinh như khách đến muộn quá giờ (`LATE_ARRIVAL`), không đến hẹn (`NO_SHOW`), phát sinh yêu cầu thêm ngoài luồng (`EXTRA_SERVICE`), làm hư hại tài sản trang thiết bị (`DAMAGE`), hoặc lý do khác (`OTHER`).
* **status**: Trạng thái khoản phạt (`ACTIVE`, `WAIVED` - được miễn giảm, `PAID`, `CANCELLED`).

#### Bảng `service_incident` (Báo cáo sự cố hoặc khiếu nại tại garage)
* Ghi nhận toàn bộ các vấn đề phát sinh trong quá trình thi công phục vụ xe tại xưởng: Gồm loại sự cố (`SERVICE_DELAY`, `CUSTOMER_COMPLAINT`, `EQUIPMENT_ISSUE`, `PAYMENT_ISSUE`, `OTHER`) và trạng thái giải quyết (`OPEN`, `IN_PROGRESS`, `RESOLVED`, `CANCELLED`).

---

### 2.7 Nhóm Thông báo & Phân tích nghiệp vụ dữ liệu lớn (Analytics)

#### Bảng `notification` (Quản lý hệ thống thông báo đa kênh)
* Hỗ trợ gửi tin nhắn cho khách hàng qua các kênh chỉ định (`IN_APP`, `EMAIL`, `SMS`, `ZALO`) thuộc các nhóm nội dung (`BOOKING_CONFIRMATION`, `BOOKING_REMINDER`, `LOYALTY_UPDATE`, `PROMOTION`, `SYSTEM`) kèm cờ kiểm tra trạng thái và tính nhất quán thời gian đọc tin.

#### Bảng `booking_behavior_monthly` (Phân tích hành vi người dùng định kỳ theo tháng)
* Tổng hợp định kỳ hàng tháng số liệu của từng người dùng tại mỗi garage: Tổng số đơn đặt, số đơn hoàn thành, số đơn hủy, số lần bùng lịch (no-show), tổng tiền chi tiêu, và khung giờ ưa thích nhất (`preferred_slot_id`).

#### Bảng `customer_segment_monthly` (Phân hạng tệp khách hàng định kỳ bằng thuật toán)
* Lưu kết quả chạy phân tích định kỳ hàng tháng phục vụ phòng Marketing: Tính toán điểm số RFM (`rfm_score` từ 0 đến 555), gán nhãn phân khúc khách hàng (`segment_name`), và đưa ra cảnh báo nguy cơ khách hàng rời bỏ dịch vụ (`churn_risk`: `LOW`, `MEDIUM`, `HIGH`).

---

## 3. Danh sách các Chỉ mục Tối ưu hóa (Indexes)
Hệ thống được cấu hình sẵn các B-Tree Index nhằm tăng tốc độ truy vấn tìm kiếm, lọc dữ liệu lớn và tối ưu hóa hiệu năng các lệnh `JOIN` thông qua các khóa ngoại:
* `idx_user_role_user_garage`: Tối ưu hóa xác thực quyền truy cập tài khoản theo garage.
* `idx_vehicle_user`, `idx_vehicle_license_plate_status`: Tăng tốc tra cứu phương tiện xe.
* `idx_booking_slot_date_status`, `idx_booking_garage_date`: Tối ưu tìm kiếm lịch hẹn trống và báo cáo doanh thu theo ngày.
* `idx_booking_user`, `idx_booking_staff`, `idx_booking_code_status`: Hỗ trợ lọc danh sách lịch hẹn phía Client App và Dashboard Admin.
* `idx_payment_booking`, `idx_invoice_booking`, `idx_penalty_booking`, `idx_incident_booking`: Gia tốc quá trình xử lý đối soát và in ấn tài chính.
* `idx_loyalty_account_user_garage`, `idx_loyalty_txn_account`, `idx_reward_redemption_account`: Tăng tốc hiển thị màn hình tích điểm đổi thưởng.
* `idx_notification_user_status`: Tối ưu hóa tải danh sách thông báo chưa đọc trên thiết bị di động.
* `idx_behavior_garage_month`, `idx_segment_garage_month`: Tăng tốc kết xuất báo cáo Dashboard BI định kỳ hàng tháng.

---

## 4. Đặc tả Hàm Hệ thống & Kịch bản Tự động (Functions & Triggers)

### 4.1 Cập nhật thời gian tự động (`set_updated_at`)
* **Hàm liên kết**: Trả về `TRIGGER`, tự động gán giá trị trường `updated_at = NOW()` bất cứ khi nào bản ghi được cập nhật.
* **Các bảng áp dụng**: Gồm 9 bảng cốt lõi hệ thống: `garage`, `app_user`, `role`, `vehicle`, `service_package`, `loyalty_policy`, `loyalty_account`, `booking`, `payment`.

### 4.2 Bảo vệ tính hợp lệ ngày hẹn (`validate_booking_date_guard`)
* **Logic nghiệp vụ**: Ngăn chặn tuyệt đối việc tạo mới hoặc chuyển đổi ngày hẹn đặt lịch về thời điểm quá khứ đối với các đơn hàng đang có trạng thái hoạt động tích cực (`PENDING`, `CONFIRMED`, `CHECKED_IN`, `WASHING`). Đơn hàng cũ mang tính chất lịch sử đã hoàn thành hoặc hủy thì bỏ qua bộ lọc này.

### 4.3 Xác thực chéo vai trò nhân viên xử lý (`validate_assigned_staff_role`)
* **Logic nghiệp vụ**: Bất cứ khi nào gán nhân viên vào vị trí xử lý đơn rửa xe (`assigned_staff_user_id`), trigger sẽ thực hiện kiểm tra chéo điều kiện: Người dùng được chọn bắt buộc phải đang ở trạng thái kích hoạt (`ACTIVE`), thuộc quyền quản lý trực tiếp của chính chi nhánh (`garage_id`) đó, và phải được phân vai trò (`role_name`) chính xác là `STAFF` đang hoạt động. Nếu không thỏa mãn, hệ thống lập tức báo lỗi cấu trúc ngăn chặn lệnh lưu.

### 4.4 Kiểm tra giới hạn công suất đặt lịch (`check_booking_slot_capacity`)
* **Logic nghiệp vụ**: Trước khi một đơn đặt lịch mới ở trạng thái chờ/hoạt động được đưa vào bảng `booking`, hàm sẽ thực hiện cơ chế khóa dòng (`FOR UPDATE`) trên bảng cấu hình khung giờ `booking_slot` để lấy ra công suất tối đa phục vụ cùng lúc (`max_capacity`). Sau đó, tiến hành đếm số lượng xe thực tế đang xếp lịch tại khung giờ đó trong ngày chỉ định. Nếu vượt quá ngưỡng cho phép, hệ thống sẽ chặn giao dịch và trả ra thông báo lỗi chi tiết: *"Khung giờ này đã đạt giới hạn công suất tối đa cho ngày đã chọn!"*.

---

## 5. Các thành phần nâng cao (Phần tiếp theo của File SQL gốc)

⚠️ **[PING] @NghiaPT**: Phần code logic SQL chi tiết từ mục 8.6 trở đi (Hàm xử lý tự động cộng điểm thưởng Khách hàng thân thiết `process_loyalty_for_completed_paid_booking` khi đơn hoàn tất & thanh toán xong, hàm tự động nâng/hạ hạng thẻ thành viên, đồng bộ cập nhật bảng dung lượng công suất `booking_slot_capacity`, triggers xử lý đồng bộ trạng thái tài chính...) bị cắt ngắn/chưa hiển thị đầy đủ ở file văn bản đầu vào.

**👉 Hành động yêu cầu từ NghiaPT**: Bạn hãy note thêm logic Java backend hoặc bổ sung đoạn script SQL còn thiếu của các hàm Loyalty/Capacity này vào đây nếu cần tớ phân tích cấu trúc sâu hơn nhé!
