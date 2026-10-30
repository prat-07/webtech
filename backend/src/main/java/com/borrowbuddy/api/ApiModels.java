package com.borrowbuddy.api;

import com.borrowbuddy.domain.*;

public final class ApiModels {
  private ApiModels() {}

  public record ListingResponse(
      Long id,
      String title,
      String description,
      String category,
      String imageUrl,
      String ownerName,
      Double ownerRating,
      Long ratingCount,
      String campus,
      Listing.Status status) {
    static ListingResponse from(Listing l, CampusUser owner, Double avg, Long count) {
      return new ListingResponse(
          l.id,
          l.title,
          l.description,
          l.category,
          l.imageUrl,
          owner.name,
          avg,
          count,
          l.campus,
          l.status);
    }
  }

  public record LoanResponse(
      Long id,
      Long listingId,
      String listingTitle,
      Long borrowerId,
      String borrowerName,
      java.time.LocalDate startDate,
      java.time.LocalDate returnDate,
      LoanRequest.Status status,
      java.time.Instant createdAt) {
    static LoanResponse from(LoanRequest r, Listing l, CampusUser borrower) {
      return new LoanResponse(
          r.id,
          l.id,
          l.title,
          borrower.id,
          borrower.name,
          r.startDate,
          r.returnDate,
          r.status,
          r.createdAt);
    }
  }

  public record NotificationResponse(
      Long id, String message, boolean read, java.time.Instant createdAt, Long loanRequestId) {
    static NotificationResponse from(Notification n) {
      return new NotificationResponse(n.id, n.message, n.read, n.createdAt, n.loanRequestId);
    }
  }

  public record ReviewInput(
      @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(5) int rating,
      @jakarta.validation.constraints.Size(max = 1000) String comment) {}

  public record ReviewResponse(Long id, int rating, String comment, java.time.Instant createdAt) {}
}
