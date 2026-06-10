package swp391.carwash.service;

import org.springframework.stereotype.Service;
import swp391.carwash.entity.BookingSlot;

import java.time.LocalDate;
import java.util.List;

@Service
interface BookingSlotService {
    // 1. Cấu hình/Tạo mới một khung giờ làm việc cho Garage (Check trùng giờ/trùng ngày)
    BookingSlot createSlot(BookingSlot bookingSlot);

    // 2. Lấy danh sách các khung giờ của một Garage trong một ngày cụ thể (Dùng để hiển thị lịch đặt trên FE)
    List<BookingSlot> getSlotsByGarageAndDate(Long garageId, LocalDate date);

    // 3. Lấy toàn bộ danh sách slot của một Garage (Phục vụ trang quản lý ca của Gara)
    List<BookingSlot> getSlotsByGarageId(Long garageId);

    // 4. Cập nhật Sức chứa tối đa (maxCapacity) cho một Slot
    // NGHIỆP VỤ: Check không cho hạ maxCapacity thấp hơn số lượng bookedCount hiện tại
    BookingSlot updateMaxCapacity(Long slotId, Integer newMaxCapacity);

    // 5. Xóa hoặc đóng một khung giờ không phục vụ nữa
    void deleteSlot(Long slotId);
}
