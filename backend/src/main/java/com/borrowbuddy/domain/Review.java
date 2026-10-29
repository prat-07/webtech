package com.borrowbuddy.domain;
import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="reviews") public class Review {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(name="loan_request_id",nullable=false,unique=true) public Long loanRequestId;
 @Column(name="reviewer_id",nullable=false) public Long reviewerId;
 @Column(name="reviewee_id",nullable=false) public Long revieweeId;
 @Column(nullable=false) public int rating;
 @Column(length=1000) public String comment;
 @Column(name="created_at",insertable=false,updatable=false) public Instant createdAt;
 protected Review(){} public Review(Long req,Long reviewer,Long reviewee,int rating,String comment){loanRequestId=req;reviewerId=reviewer;revieweeId=reviewee;this.rating=rating;this.comment=comment;}
}
