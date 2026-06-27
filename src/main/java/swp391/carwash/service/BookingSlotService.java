package swp391.carwash.service;

import java.time.LocalDate;
import java.util.List;
import swp391.carwash.dto.request.BookingSlotCreateRequest;
import swp391.carwash.dto.respone.BookingSlotResponse;
import swp391.carwash.entity.BookingSlot;

public interface BookingSlotService {

    BookingSlotResponse createSlot(Integer garageId, BookingSlotCreateRequest request);

    List<BookingSlotResponse> getSlotsByGarageAndDate(Integer garageId, LocalDate date);

    List<BookingSlot> getSlotsByGarageId(Integer garageId);

    BookingSlotResponse updateMaxCapacity(Integer slotId, Integer newMaxCapacity);

    void deleteSlot(Integer slotId);
}
