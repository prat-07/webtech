package com.borrowbuddy.domain;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "listings")
public class Listing {
  public enum Status {
    ACTIVE,
    PAUSED,
    DELETED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "owner_id", nullable = false)
  public Long ownerId;

  @Column(nullable = false, length = 180)
  public String title;

  @Column(nullable = false, length = 2000)
  public String description;

  @Column(nullable = false, length = 80)
  public String category;

  @Column(name = "image_url", length = 2048)
  public String imageUrl;

  @Column(nullable = false, length = 180)
  public String campus;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  public Status status = Status.ACTIVE;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  public Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  public Instant updatedAt;

  @Version public Long version;

  protected Listing() {}

  public Listing(
      Long ownerId,
      String title,
      String description,
      String category,
      String imageUrl,
      String campus) {
    this.ownerId = ownerId;
    this.title = title;
    this.description = description;
    this.category = category;
    this.imageUrl = imageUrl;
    this.campus = campus;
  }
}
