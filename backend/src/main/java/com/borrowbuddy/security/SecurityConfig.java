package com.borrowbuddy.security;

import com.borrowbuddy.repo.CampusUserRepository;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.*;

@Configuration
public class SecurityConfig {
  @Bean
  UserDetailsService users(CampusUserRepository repo) {
    return email ->
        repo.findByEmailIgnoreCase(email)
            .map(CampusPrincipal::new)
            .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
  }

  @Bean
  CookieCsrfTokenRepository csrfRepository(
      @Value("${server.servlet.session.cookie.secure:false}") boolean secure) {
    CookieCsrfTokenRepository repo = CookieCsrfTokenRepository.withHttpOnlyFalse();
    repo.setCookieCustomizer(c -> c.secure(secure).sameSite("Lax").path("/"));
    return repo;
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http, CookieCsrfTokenRepository csrfRepository)
      throws Exception {
    return http.cors(Customizer.withDefaults())
        .csrf(
            csrf ->
                csrf.csrfTokenRepository(csrfRepository)
                    .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/api/health",
                        "/api/auth/register",
                        "/api/auth/csrf",
                        "/actuator/health/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .formLogin(
            f ->
                f.loginProcessingUrl("/api/auth/login")
                    .usernameParameter("email")
                    .passwordParameter("password")
                    .successHandler((req, res, auth) -> res.setStatus(204))
                    .failureHandler(
                        (req, res, ex) -> {
                          res.setStatus(HttpStatus.UNAUTHORIZED.value());
                          res.setContentType("application/problem+json");
                          res.setCharacterEncoding("UTF-8");
                          res.getWriter()
                              .write(
                                  "{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"Invalid email or password\"}");
                        }))
        .logout(
            l ->
                l.logoutUrl("/api/auth/logout")
                    .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)))
        .exceptionHandling(
            e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .build();
  }

  @Bean
  CorsConfigurationSource cors(
      @Value("${app.cors.allowed-origins:http://localhost:5173}") String origins) {
    CorsConfiguration c = new CorsConfiguration();
    c.setAllowedOrigins(
        Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
    c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    c.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
    c.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", c);
    return source;
  }
}
