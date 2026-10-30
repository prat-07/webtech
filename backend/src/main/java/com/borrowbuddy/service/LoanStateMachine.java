package com.borrowbuddy.service;

import com.borrowbuddy.domain.LoanRequest;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

public final class LoanStateMachine {
  private static final Map<LoanRequest.Status, Set<LoanRequest.Status>> ALLOWED =
      Map.of(
          LoanRequest.Status.REQUESTED,
          Set.of(
              LoanRequest.Status.APPROVED,
              LoanRequest.Status.REJECTED,
              LoanRequest.Status.CANCELLED,
              LoanRequest.Status.EXPIRED),
          LoanRequest.Status.APPROVED,
          Set.of(LoanRequest.Status.CANCELLED, LoanRequest.Status.BORROWED),
          LoanRequest.Status.BORROWED,
          Set.of(LoanRequest.Status.RETURNED),
          LoanRequest.Status.REJECTED,
          Set.of(),
          LoanRequest.Status.RETURNED,
          Set.of(),
          LoanRequest.Status.CANCELLED,
          Set.of(),
          LoanRequest.Status.EXPIRED,
          Set.of());

  public void validate(
      LoanRequest request, LoanRequest.Status next, boolean owner, LocalDate today) {
    if (!ALLOWED.getOrDefault(request.status, Set.of()).contains(next))
      throw new IllegalStateException("Invalid loan status transition");
    if ((next == LoanRequest.Status.APPROVED
            || next == LoanRequest.Status.REJECTED
            || next == LoanRequest.Status.BORROWED
            || next == LoanRequest.Status.RETURNED)
        && !owner) throw new SecurityException("Only the listing owner can perform this action");
    if (next == LoanRequest.Status.CANCELLED
        && request.status == LoanRequest.Status.REQUESTED
        && owner) throw new SecurityException("Only the borrower can cancel a pending request");
    if (next == LoanRequest.Status.CANCELLED
        && request.status == LoanRequest.Status.APPROVED
        && today.isAfter(request.startDate))
      throw new IllegalStateException("An approved loan cannot be cancelled after it starts");
    if (next == LoanRequest.Status.BORROWED && today.isBefore(request.startDate))
      throw new IllegalStateException("A loan cannot start before its borrowing date");
  }
}
