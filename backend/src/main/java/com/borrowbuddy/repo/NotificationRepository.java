package com.borrowbuddy.repo;
import com.borrowbuddy.domain.Notification; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface NotificationRepository extends JpaRepository<Notification,Long>{List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId); long countByUserIdAndReadFalse(Long userId);}
