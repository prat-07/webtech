package com.borrowbuddy.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "users")
public class CampusUser {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, length = 120)
  public String name;

  @Column(nullable = false, unique = true)
  public String email;

  @Column(nullable = false)
  public String password;

  @Column(nullable = false, length = 180)
  public String campus;

  @Column(nullable = false, precision = 3, scale = 2)
  public BigDecimal reputation = new BigDecimal("0.0");

  protected CampusUser() {}

  public CampusUser(String name, String email, String password, String campus) {
    this.name = name;
    this.email = email;
    this.password = password;
    this.campus = campus;
  }
}
