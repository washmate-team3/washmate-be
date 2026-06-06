package swp391.carwash.service;

import swp391.carwash.dto.GarageRequest;
import swp391.carwash.dto.GarageResponse;

import java.util.List;

public interface GarageService {
    GarageResponse createGarage(GarageRequest request);
    GarageResponse getGarageById(Integer id);
    List<GarageResponse> getAllActiveGarages();
    GarageResponse updateGarage(Integer id, GarageRequest request);
    void softDeleteGarage(Integer id);
}
