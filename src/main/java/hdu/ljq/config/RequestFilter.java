package hdu.ljq.config;

import hdu.ljq.common.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.net.URI;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(-110)
public class RequestFilter extends OncePerRequestFilter {
  private final ApiResponses responses;

  public RequestFilter(ApiResponses responses) {
    this.responses = responses;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest r, HttpServletResponse s, FilterChain chain)
      throws ServletException, IOException {
    s.setHeader("X-Request-Id", ApiResponses.requestId(r));
    if (r.getRequestURI().startsWith("/api/v1/")) s.setHeader("Cache-Control", "no-store");
    if (!Set.of("GET", "HEAD", "OPTIONS").contains(r.getMethod())) {
      String origin = r.getHeader("Origin");
      if (origin != null) {
        try {
          URI u = URI.create(origin);
          int port = u.getPort() == -1 ? (u.getScheme().equals("https") ? 443 : 80) : u.getPort();
          if (!u.getHost().equals(r.getServerName())
              || port != r.getServerPort()
              || !u.getScheme().equals(r.getScheme())) throw new Exception();
        } catch (Exception e) {
          responses.write(
              r,
              s,
              new ApiException(403, "CSRF_INVALID", "Cross-origin requests are not allowed."));
          return;
        }
      }
      if (r.getContentType() != null
          && r.getContentType().contains("application/json")
          && r.getContentLengthLong() > 262144) {
        responses.write(
            r, s, new ApiException(413, "PAYLOAD_TOO_LARGE", "JSON request exceeds 256 KiB."));
        return;
      }
    }
    chain.doFilter(r, s);
  }
}
