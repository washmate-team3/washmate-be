package swp391.carwash.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.dto.request.vehicles.CreateVehicleRequest;
import swp391.carwash.dto.request.vehicles.UpdateVehicleRequest;
import swp391.carwash.dto.response.vehicles.VehicleResponse;

import java.util.List;

@Service
@Transactional
public interface VehicleService {

    VehicleResponse create(CreateVehicleRequest request);

    List<VehicleResponse> getAll();

    List<VehicleResponse> getByUserId(Integer userId);

    VehicleResponse update(Integer vehicleId,UpdateVehicleRequest request);

    void delete(Integer vehicleId);

    List<VehicleResponse> getByEmail(String email);

    VehicleResponse createVehicleForUser(Integer userId, CreateVehicleRequest request);


}
