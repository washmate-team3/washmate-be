package swp391.carwash.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.entity.ServicePackage;

import java.util.List;
import java.util.Optional;


@Service
@Transactional

public class ServicePackageServiceImpl implements ServicePackageService{
    @Override
    public ServicePackage createService(ServicePackage servicePackage) {
            return null;
    }

    @Override
    public List<ServicePackage> getServicesByGarageId(Long garageId) {
        return List.of();
    }

    @Override
    public Optional<ServicePackage> getServiceById(Long id) {
        return Optional.empty();
    }

    @Override
    public ServicePackage updateService(Long id, ServicePackage serviceDetails) {
        return null;
    }

    @Override
    public void updateServiceStatus(Long id, String status) {

    }

    @Override
    public void deleteService(Long id) {

    }
}
