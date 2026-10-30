package com.borrowbuddy.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "user_id", nullable = false)
  public Long userId;

  @Column(name = "loan_request_id")
  public Long loanRequestId;

  @Column(nullable = false)
  public String message;

  @Column(nullable = false)
  public boolean read;

  @Column(name = "created_at", insertable = false, updatable = false)
  public Instant createdAt;

  protected Notification() {}

  public Notification(Long userId, Long requestId, String message) {
    this.userId = userId;
    this.loanRequestId = requestId;
    this.message = message;
  }
}
