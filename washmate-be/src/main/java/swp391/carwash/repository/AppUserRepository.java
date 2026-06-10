package swp391.carwash.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.carwash.entity.AppUser;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Integer> {


}
