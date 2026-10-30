package com.borrowbuddy.repo;

import com.borrowbuddy.domain.CampusUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampusUserRepository extends JpaRepository<CampusUser, Long> {
  Optional<CampusUser> findByEmailIgnoreCase(String email);
}
