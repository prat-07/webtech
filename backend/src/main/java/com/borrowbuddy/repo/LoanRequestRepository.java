package com.borrowbuddy.repo;

import com.borrowbuddy.domain.LoanRequest;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface LoanRequestRepository extends JpaRepository<LoanRequest, Long> {
  List<LoanRequest> findByBorrowerIdOrderByCreatedAtDesc(Long borrowerId);

  @Query(
      "select r from LoanRequest r join Listing l on l.id=r.listingId where l.ownerId=:owner order by r.createdAt desc")
  List<LoanRequest> findForOwner(@Param("owner") Long ownerId);

  @Query(
      "select r from LoanRequest r where r.listingId=:listing and r.status in ('APPROVED','BORROWED') and r.startDate < :end and r.returnDate > :start")
  List<LoanRequest> overlapping(
      @Param("listing") Long listing, @Param("start") LocalDate start, @Param("end") LocalDate end);

  boolean existsByListingIdAndBorrowerIdAndStatusIn(
      Long listingId, Long borrowerId, Collection<LoanRequest.Status> statuses);

  boolean existsByListingIdAndStatusIn(Long listingId, Collection<LoanRequest.Status> statuses);
}
