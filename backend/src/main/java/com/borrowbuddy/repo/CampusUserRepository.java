package com.borrowbuddy.repo;
import com.borrowbuddy.domain.CampusUser; import org.springframework.data.jpa.repository.JpaRepository; import java.util.Optional;
public interface CampusUserRepository extends JpaRepository<CampusUser,Long>{Optional<CampusUser> findByEmailIgnoreCase(String email);}
