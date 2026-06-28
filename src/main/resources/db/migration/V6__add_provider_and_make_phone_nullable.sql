-- Thêm cột provider vào bảng app_user
ALTER TABLE app_user ADD COLUMN provider VARCHAR(20) DEFAULT 'LOCAL' NOT NULL;

-- Cho phép cột phone nhận giá trị NULL để hỗ trợ user đăng nhập qua Google mà chưa có SĐT
ALTER TABLE app_user ALTER COLUMN phone DROP NOT NULL;
