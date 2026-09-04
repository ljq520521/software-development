package hdu.ljq.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class ApiResponses {
  private final ObjectMapper json;

  public ApiResponses(ObjectMapper json) {
    this.json = json;
  }

  public static String requestId(HttpServletRequest req) {
    Object id = req.getAttribute("requestId");
    if (id == null) {
      id = UUID.randomUUID().toString();
      req.setAttribute("requestId", id);
    }
    return id.toString();
  }

  public static Map<String, Object> ok(Object data, HttpServletRequest req) {
    return Map.of("code", "OK", "message", "Success", "data", data, "request_id", requestId(req));
  }

  public static Map<String, Object> error(ApiException e, HttpServletRequest req) {
    return Map.of(
        "code",
        e.code,
        "message",
        e.getMessage(),
        "field_errors",
        e.fields,
        "request_id",
        requestId(req));
  }

  public void write(HttpServletRequest req, HttpServletResponse res, ApiException e)
      throws IOException {
    res.setStatus(e.status);
    res.setContentType("application/json;charset=UTF-8");
    res.setHeader("X-Request-Id", requestId(req));
    json.writeValue(res.getOutputStream(), error(e, req));
  }
}
