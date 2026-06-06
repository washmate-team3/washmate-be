package swp391.carwash.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.dto.GarageRequest;
import swp391.carwash.dto.GarageResponse;
import swp391.carwash.exception.ResourceNotFoundException;
import swp391.carwash.repository.Garage;
import swp391.carwash.repository.GarageRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GarageServiceImpl implements GarageService {

    private final GarageRepository garageRepository;

    @Autowired
    public GarageServiceImpl(GarageRepository garageRepository) {
        this.garageRepository = garageRepository;
    }

    @Override
    @Transactional
    public GarageResponse createGarage(GarageRequest request) {
        Garage garage = Garage.builder()
                .name(request.getName())
                .address(request.getAddress())
                .phone(request.getPhone())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .build();

        Garage savedGarage = garageRepository.save(garage);
        return mapToResponse(savedGarage);
    }

    @Override
    @Transactional(readOnly = true)
    public GarageResponse getGarageById(Integer id) {
        Garage garage = garageRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Garage not found with id: " + id));
        return mapToResponse(garage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GarageResponse> getAllActiveGarages() {
        return garageRepository.findAllActive().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public GarageResponse updateGarage(Integer id, GarageRequest request) {
        Garage garage = garageRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Garage not found with id: " + id));

        garage.setName(request.getName());
        garage.setAddress(request.getAddress());
        garage.setPhone(request.getPhone());
        if (request.getStatus() != null) {
            garage.setStatus(request.getStatus());
        }

        Garage updatedGarage = garageRepository.save(garage);
        return mapToResponse(updatedGarage);
    }

    @Override
    @Transactional
    public void softDeleteGarage(Integer id) {
        Garage garage = garageRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Garage not found with id: " + id));

        garage.setStatus("DELETED");
        garage.setDeletedAt(OffsetDateTime.now());
        garageRepository.save(garage);
    }

    private GarageResponse mapToResponse(Garage garage) {
        return GarageResponse.builder()
                .garageId(garage.getGarageId())
                .name(garage.getName())
                .address(garage.getAddress())
                .phone(garage.getPhone())
                .status(garage.getStatus())
                .createdAt(garage.getCreatedAt())
                .updatedAt(garage.getUpdatedAt())
                .build();
    }
}
