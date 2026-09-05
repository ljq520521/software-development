package hdu.ljq.web;

import com.fasterxml.jackson.databind.JsonNode;
import hdu.ljq.common.*;
import hdu.ljq.service.DealerAccountService;
import jakarta.servlet.http.*;
import java.time.Instant;
import java.util.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dealer/auth")
public class DealerAuthController {
  private final DealerAccountService dealers;
  private final SecurityContextRepository contexts;
  private final HttpSessionCsrfTokenRepository csrf;
  private final RateLimiter limits;

  public DealerAuthController(
      DealerAccountService dealers,
      SecurityContextRepository contexts,
      HttpSessionCsrfTokenRepository csrf,
      RateLimiter limits) {
    this.dealers = dealers;
    this.contexts = contexts;
    this.csrf = csrf;
    this.limits = limits;
  }

  @PostMapping(value = "/activate", consumes = "application/json")
  public Object activate(@RequestBody JsonNode body, HttpServletRequest request) {
    return ApiResponses.ok(dealers.activate(body), request);
  }

  @PostMapping(value = "/login", consumes = "application/json")
  public Object login(
      @RequestBody JsonNode body, HttpServletRequest request, HttpServletResponse response) {
    if (!body.isObject() || body.size() != 2 || !body.path("email").isTextual()
        || !body.path("password").isTextual())
      throw ApiException.invalid("body", "Provide email and password.");
    String email = body.path("email").asText().strip().toLowerCase(Locale.ROOT);
    String key = "dealer-login:" + email + ":" + request.getRemoteAddr();
    limits.check(key, 5, 900000, false);
    JsonNode account;
    try {
      account = dealers.authenticate(email, body.path("password").asText());
    } catch (ApiException e) {
      limits.check(key, 5, 900000, true);
      throw e;
    }
    limits.clear(key);
    request.getSession();
    request.changeSessionId();
    request.getSession().removeAttribute("actorId");
    request.getSession().removeAttribute("loginAt");
    request.getSession().setAttribute("dealerAccountId", account.path("id").asText());
    request.getSession().setAttribute("dealerLoginAt", System.currentTimeMillis());
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        new UsernamePasswordAuthenticationToken(
            email, null, List.of(new SimpleGrantedAuthority("ROLE_DEALER"))));
    SecurityContextHolder.setContext(context);
    contexts.saveContext(context, request, response);
    CsrfToken next = csrf.generateToken(request);
    csrf.saveToken(next, request, response);
    return ApiResponses.ok(
        Map.of(
            "user", account,
            "csrf_token", next.getToken(),
            "expires_at", Instant.now().plusSeconds(28800).toString()),
        request);
  }

  @GetMapping("/me")
  public Object me(HttpServletRequest request) {
    Object id = request.getSession().getAttribute("dealerAccountId");
    if (id == null) throw new ApiException(401, "UNAUTHENTICATED", "Please sign in.");
    return ApiResponses.ok(dealers.account(id.toString()), request);
  }

  @PostMapping("/logout")
  public Object logout(HttpServletRequest request, HttpServletResponse response) {
    request.getSession().invalidate();
    SecurityContextHolder.clearContext();
    response.addHeader("Set-Cookie", "WMSESSION=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
    return ApiResponses.ok(Map.of("logged_out", true), request);
  }
}
