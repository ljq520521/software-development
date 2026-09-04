package hdu.ljq.common;

import java.util.*;

public class ApiException extends RuntimeException {
  public final int status;
  public final String code;
  public final Map<String, List<String>> fields;

  public ApiException(int status, String code, String message) {
    this(status, code, message, Map.of());
  }

  public ApiException(int status, String code, String message, Map<String, List<String>> fields) {
    super(message);
    this.status = status;
    this.code = code;
    this.fields = fields;
  }

  public static ApiException invalid(String field, String message) {
    return new ApiException(
        422,
        "VALIDATION_ERROR",
        "Please check the submitted fields.",
        Map.of(field, List.of(message)));
  }

  public static ApiException missing() {
    return new ApiException(404, "NOT_FOUND", "The requested resource was not found.");
  }

  public static ApiException conflict(String code, String message) {
    return new ApiException(409, code, message);
  }
}
