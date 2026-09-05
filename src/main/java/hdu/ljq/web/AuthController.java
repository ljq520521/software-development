package hdu.ljq.web;

import com.fasterxml.jackson.databind.*;
import hdu.ljq.common.*;
import hdu.ljq.persistence.*;
import jakarta.servlet.http.*;
import java.time.Instant;
import java.util.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final Repository repo;
  private final Contract contract;
  private final PasswordEncoder encoder;
  private final HttpSessionCsrfTokenRepository csrf;
  private final SecurityContextRepository contexts;
  private final RateLimiter limits;

  public AuthController(
      Repository r,
      Contract c,
      PasswordEncoder e,
      HttpSessionCsrfTokenRepository csrf,
      SecurityContextRepository contexts,
      RateLimiter limits) {
    repo = r;
    contract = c;
    encoder = e;
    this.csrf = csrf;
    this.contexts = contexts;
    this.limits = limits;
  }

  @GetMapping("/csrf")
  public Object csrf(CsrfToken token, HttpServletRequest r) {
    return ApiResponses.ok(Map.of("csrf_token", token.getToken()), r);
  }

  @PostMapping(value = "/login", consumes = "application/json")
  public Object login(@RequestBody JsonNode body, HttpServletRequest r, HttpServletResponse s) {
    JsonNode d = contract.input("LoginRequest", body);
    String email = d.path("email").asText().strip().toLowerCase(Locale.ROOT),
        key = "login:" + email + ":" + r.getRemoteAddr();
    limits.check(key, 5, 900000, false);
    JsonNode user = repo.by(EntityType.ADMIN, "email", email);
    String hash =
        user == null
            ? "$2a$12$W6bx9tT/9BO9GCucUN.KPuqz.Q/qX8pjV6BO5vw3IuOVdiBuCUUsi"
            : user.path("password_hash").asText();
    boolean valid = encoder.matches(d.path("password").asText(), hash);
    if (user == null || !valid || !user.path("status").asText().equals("active")) {
      limits.check(key, 5, 900000, true);
      throw new ApiException(401, "INVALID_CREDENTIALS", "Incorrect email or password.");
    }
    limits.clear(key);
    r.getSession();
    r.changeSessionId();
    r.getSession().removeAttribute("dealerAccountId");
    r.getSession().removeAttribute("dealerLoginAt");
    r.getSession().setAttribute("actorId", user.path("id").asText());
    r.getSession().setAttribute("loginAt", System.currentTimeMillis());
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        new UsernamePasswordAuthenticationToken(
            email, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    SecurityContextHolder.setContext(context);
    contexts.saveContext(context, r, s);
    CsrfToken next = csrf.generateToken(r);
    csrf.saveToken(next, r, s);
    return ApiResponses.ok(
        Map.of(
            "user",
            user(user),
            "csrf_token",
            next.getToken(),
            "expires_at",
            Instant.now().plusSeconds(28800).toString()),
        r);
  }

  @GetMapping("/me")
  public Object me(HttpServletRequest r) {
    return ApiResponses.ok(
        user(repo.find(EntityType.ADMIN, r.getSession().getAttribute("actorId").toString())), r);
  }

  @PostMapping("/logout")
  public Object logout(HttpServletRequest r, HttpServletResponse s) {
    r.getSession().invalidate();
    SecurityContextHolder.clearContext();
    s.addHeader("Set-Cookie", "WMSESSION=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
    return ApiResponses.ok(Map.of("logged_out", true), r);
  }

  private Map<String, Object> user(JsonNode u) {
    return Map.of(
        "id",
        u.path("id").asText(),
        "email",
        u.path("email").asText(),
        "display_name",
        u.path("display_name").asText(),
        "role",
        "admin");
  }
}
