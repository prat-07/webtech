package com.borrowbuddy.api;

import com.borrowbuddy.domain.CampusUser;
import com.borrowbuddy.repo.CampusUserRepository;
import com.borrowbuddy.repo.ReviewRepository;
import com.borrowbuddy.security.CampusPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final CampusUserRepository users;
  private final ReviewRepository reviews;
  private final PasswordEncoder encoder;

  public AuthController(
      CampusUserRepository users, ReviewRepository reviews, PasswordEncoder encoder) {
    this.users = users;
    this.reviews = reviews;
    this.encoder = encoder;
  }

  public record RegisterInput(
      @NotBlank @Size(max = 120) String name,
      @NotBlank @Email @Size(max = 255) String email,
      @NotBlank @Size(min = 12, max = 72) String password,
      @NotBlank @Size(max = 180) String campus) {}

  public record Profile(
      Long id, String name, String email, String campus, Double reputation, Long reviewCount) {}

  @GetMapping("/csrf")
  public Map<String, String> csrf(CsrfToken token) {
    return Map.of("token", token.getToken());
  }

  @GetMapping("/me")
  public Profile me(@AuthenticationPrincipal CampusPrincipal p) {
    CampusUser u =
        users
            .findById(p.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    return new Profile(
        u.id, u.name, u.email, u.campus, reviews.averageFor(u.id), reviews.countByRevieweeId(u.id));
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, String> register(@Valid @RequestBody RegisterInput in) {
    String email = in.email().trim().toLowerCase(Locale.ROOT);
    if (in.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Password is too long in UTF-8 bytes");
    if (users.findByEmailIgnoreCase(email).isPresent())
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "An account with this email already exists");
    users.save(
        new CampusUser(
            java.text.Normalizer.normalize(
                in.name().trim().replaceAll("\\s+", " "), java.text.Normalizer.Form.NFC),
            email,
            encoder.encode(in.password()),
            java.text.Normalizer.normalize(
                in.campus().trim().replaceAll("\\s+", " "), java.text.Normalizer.Form.NFC)));
    return Map.of("status", "created", "next", "/api/auth/login");
  }
}
