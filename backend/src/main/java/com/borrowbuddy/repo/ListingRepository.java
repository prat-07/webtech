package com.borrowbuddy.repo;
import jakarta.persistence.QueryHint;
import com.borrowbuddy.domain.Listing; import jakarta.persistence.LockModeType; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.util.*;
public interface ListingRepository extends JpaRepository<Listing,Long>, JpaSpecificationExecutor<Listing> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @QueryHints(@QueryHint(name="jakarta.persistence.lock.timeout", value="3000")) @Query("select l from Listing l where l.id=:id") Optional<Listing> lockById(@Param("id") Long id);
 List<Listing> findByOwnerIdAndStatusNotOrderByCreatedAtDesc(Long ownerId,Listing.Status status);
 List<Listing> findByCampusAndStatusOrderByCreatedAtDesc(String campus,Listing.Status status);
}
