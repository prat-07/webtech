package com.borrowbuddy;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BorrowBuddyApplication {
  public static void main(String[] args) {
    SpringApplication.run(BorrowBuddyApplication.class, args);
  }

  @Bean
  Clock campusClock() {
    return Clock.system(ZoneId.of("Asia/Kolkata"));
  }
}
