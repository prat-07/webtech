package com.borrowbuddy.repo;

import com.borrowbuddy.domain.Review;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {
  boolean existsByLoanRequestId(Long id);

  @Query("select avg(r.rating) from Review r where r.revieweeId=:user")
  Double averageFor(@Param("user") Long user);

  long countByRevieweeId(Long user);
}
