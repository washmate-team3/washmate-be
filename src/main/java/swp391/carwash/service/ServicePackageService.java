package swp391.carwash.service;

import swp391.carwash.dto.request.ServicePackageRequest.CreateServicePackageRequest;
import swp391.carwash.dto.request.ServicePackageRequest.UpdateServicePackageRequest;
import swp391.carwash.dto.response.ServicePackage.ServicePackageResponse;

import java.util.List;

public interface ServicePackageService {
    ServicePackageResponse createService(CreateServicePackageRequest request);
    List<ServicePackageResponse> getServicesByGarageId(Long garageId);
    ServicePackageResponse getServiceById(Long id);
    ServicePackageResponse updateService(Long id, UpdateServicePackageRequest request);
    void deleteService(Long id);
}