package swp391.carwash.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.entity.BookingSlot;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class BookingSlotServiceImpl implements BookingSlotService{

    @Override
    public BookingSlot createSlot(BookingSlot bookingSlot) {
        return null;
    }

    @Override
    public List<BookingSlot> getSlotsByGarageAndDate(Long garageId, LocalDate date) {
        return List.of();
    }

    @Override
    public List<BookingSlot> getSlotsByGarageId(Long garageId) {
        return List.of();
    }

    @Override
    public BookingSlot updateMaxCapacity(Long slotId, Integer newMaxCapacity) {
        return null;
    }

    @Override
    public void deleteSlot(Long slotId) {

    }
}
