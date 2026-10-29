package com.borrowbuddy.repo;
import com.borrowbuddy.domain.SavedListing; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface SavedListingRepository extends JpaRepository<SavedListing,SavedListing.Key>{List<SavedListing> findByUserId(Long userId);void deleteByUserIdAndListingId(Long userId,Long listingId);}
