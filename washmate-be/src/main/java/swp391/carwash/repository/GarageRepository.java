package swp391.carwash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GarageRepository extends JpaRepository<Garage, Integer> {

    @Query("SELECT g FROM Garage g WHERE g.status <> 'DELETED'")
    List<Garage> findAllActive();

    @Query("SELECT g FROM Garage g WHERE g.garageId = :id AND g.status <> 'DELETED'")
    Optional<Garage> findActiveById(Integer id);
}
