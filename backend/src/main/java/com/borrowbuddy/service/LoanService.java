package com.borrowbuddy.service;

import com.borrowbuddy.domain.*;
import com.borrowbuddy.repo.*;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LoanService {
  private static final List<LoanRequest.Status> LIVE =
      List.of(
          LoanRequest.Status.REQUESTED, LoanRequest.Status.APPROVED, LoanRequest.Status.BORROWED);
  private final LoanRequestRepository loans;
  private final ListingRepository listings;
  private final NotificationRepository notifications;
  private final LoanStateMachine machine = new LoanStateMachine();
  private final Clock clock;

  public LoanService(
      LoanRequestRepository loans,
      ListingRepository listings,
      NotificationRepository notifications,
      Clock clock) {
    this.loans = loans;
    this.listings = listings;
    this.notifications = notifications;
    this.clock = clock;
  }

  @Transactional
  public LoanRequest request(Long listingId, Long borrower, LocalDate start, LocalDate end) {
    LocalDate today = LocalDate.now(clock);
    if (!end.isAfter(start) || start.isBefore(today) || end.isAfter(today.plusDays(90)))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Choose a valid borrowing period up to 90 days");
    Listing l =
        listings
            .lockById(listingId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
    if (l.status != Listing.Status.ACTIVE || l.ownerId.equals(borrower))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "This listing is unavailable");
    if (loans.existsByListingIdAndBorrowerIdAndStatusIn(listingId, borrower, LIVE))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "You already have an active request for this listing");
    if (!loans.overlapping(listingId, start, end).isEmpty())
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Those dates are already reserved");
    LoanRequest saved = loans.save(new LoanRequest(listingId, borrower, start, end));
    notifications.save(
        new Notification(l.ownerId, saved.id, "A student requested to borrow " + l.title + "."));
    return saved;
  }

  @Transactional
  public LoanRequest decide(Long requestId, Long owner, boolean approve) {
    LoanRequest r =
        loans
            .findById(requestId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
    Listing l =
        listings
            .lockById(r.listingId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
    if (!l.ownerId.equals(owner))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the listing owner can decide");
    transition(r, l, owner, approve ? LoanRequest.Status.APPROVED : LoanRequest.Status.REJECTED);
    return r;
  }

  @Transactional
  public LoanRequest transition(Long id, Long actor, LoanRequest.Status next) {
    LoanRequest r =
        loans
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
    Listing l =
        listings
            .lockById(r.listingId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
    transition(r, l, actor, next);
    return r;
  }

  private void transition(LoanRequest r, Listing listing, Long actor, LoanRequest.Status next) {
    boolean owner = listing.ownerId.equals(actor);
    boolean borrower = r.borrowerId.equals(actor);
    if (!owner && !borrower)
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot update this request");
    if (next == LoanRequest.Status.APPROVED
        && !loans.overlapping(r.listingId, r.startDate, r.returnDate).isEmpty())
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Those dates were just reserved");
    try {
      machine.validate(r, next, owner, LocalDate.now(clock));
    } catch (SecurityException ex) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage());
    } catch (IllegalStateException ex) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
    }
    r.status = next;
    loans.save(r);
    notifications.save(
        new Notification(
            r.borrowerId,
            r.id,
            "Your request for " + listing.title + " is now " + next.name().toLowerCase() + "."));
  }

  @Transactional
  public void cancel(Long requestId, Long actor) {
    transition(requestId, actor, LoanRequest.Status.CANCELLED);
  }

  @Transactional
  public void markDeleted(Long listingId, Long owner) {
    Listing l =
        listings
            .lockById(listingId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
    if (!l.ownerId.equals(owner))
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Only the owner can remove this listing");
    if (loans.existsByListingIdAndStatusIn(
        listingId, List.of(LoanRequest.Status.APPROVED, LoanRequest.Status.BORROWED)))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "A listing with an active loan cannot be removed");
    l.status = Listing.Status.DELETED;
  }
}
