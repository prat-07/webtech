package com.borrowbuddy.repo;

import com.borrowbuddy.domain.Listing;
import jakarta.persistence.criteria.Predicate;
import java.util.*;
import org.springframework.data.jpa.domain.Specification;

public final class ListingSpecs {
  private ListingSpecs() {}

  public static Specification<Listing> search(
      String q, String category, String campus, Long excludeOwnerId) {
    return (root, query, cb) -> {
      List<Predicate> p = new ArrayList<>();
      p.add(cb.equal(root.get("status"), Listing.Status.ACTIVE));
      p.add(cb.equal(root.get("campus"), campus));
      p.add(cb.notEqual(root.get("ownerId"), excludeOwnerId));
      if (q != null && !q.isBlank()) {
        String term = "%" + escape(q.trim().toLowerCase(Locale.ROOT)) + "%";
        p.add(
            cb.or(
                cb.like(cb.lower(root.get("title")), term, '\\'),
                cb.like(cb.lower(root.get("description")), term, '\\')));
      }
      if (category != null && !category.isBlank()) p.add(cb.equal(root.get("category"), category));
      return cb.and(p.toArray(Predicate[]::new));
    };
  }

  private static String escape(String s) {
    return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
