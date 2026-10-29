package com.borrowbuddy.domain;
import jakarta.persistence.*; import java.io.Serializable; import java.time.Instant; import java.util.Objects;
@Entity @Table(name="saved_listings") @IdClass(SavedListing.Key.class) public class SavedListing {
 @Id @Column(name="user_id") public Long userId;
 @Id @Column(name="listing_id") public Long listingId;
 @Column(name="created_at",insertable=false,updatable=false) public Instant createdAt;
 protected SavedListing(){} public SavedListing(Long userId,Long listingId){this.userId=userId;this.listingId=listingId;}
 public static class Key implements Serializable { public Long userId; public Long listingId; public Key(){} public Key(Long u,Long l){userId=u;listingId=l;} @Override public boolean equals(Object o){return o instanceof Key k&&Objects.equals(userId,k.userId)&&Objects.equals(listingId,k.listingId);} @Override public int hashCode(){return Objects.hash(userId,listingId);} }
}
