package com.borrowbuddy.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "loan_requests")
public class LoanRequest {
    public enum Status { REQUESTED, APPROVED, REJECTED, BORROWED, RETURNED, CANCELLED, EXPIRED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "listing_id", nullable = false)
    public Long listingId;
    @Column(name = "borrower_id", nullable = false)
    public Long borrowerId;
    @Column(name = "start_date", nullable = false)
    public LocalDate startDate;
    @Column(name = "return_date", nullable = false)
    public LocalDate returnDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24)
    public Status status = Status.REQUESTED;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
    @Version
    public Long version;

    protected LoanRequest() { }
    public LoanRequest(Long listingId, Long borrowerId, LocalDate start, LocalDate end) {
        this.listingId = listingId; this.borrowerId = borrowerId; this.startDate = start; this.returnDate = end;
    }
}
