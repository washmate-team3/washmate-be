package swp391.carwash.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.entity.Garage;

import java.util.List;
import java.util.Optional;

@Service
@Transactional

public class GarageServiceImpl implements GarageService{
    @Override
    public Garage createGarage(Garage garage) {
        return null;
    }

    @Override
    public List<Garage> getAllGarages() {
        return List.of();
    }

    @Override
    public Optional<Garage> getGarageById(Long id) {
        return Optional.empty();
    }

    @Override
    public Garage updateGarage(Long id, Garage garageDetails) {
        return null;
    }

    @Override
    public void deleteGarage(Long id) {

    }
}
