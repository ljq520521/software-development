package hdu.ljq.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.*;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice(basePackages = "hdu.ljq.web")
public class Errors {
  private static final Logger log = LoggerFactory.getLogger(Errors.class);

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<?> api(ApiException e, HttpServletRequest r) {
    return ResponseEntity.status(e.status)
        .headers(
            h -> {
              if (e.status == 429) h.set("Retry-After", "60");
            })
        .body(ApiResponses.error(e, r));
  }

  @ExceptionHandler(DuplicateKeyException.class)
  public ResponseEntity<?> duplicate(Exception e, HttpServletRequest r) {
    return api(
        ApiException.conflict("UNIQUE_CONFLICT", "This SKU, slug or record already exists."), r);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<?> size(Exception e, HttpServletRequest r) {
    return api(new ApiException(413, "PAYLOAD_TOO_LARGE", "The file exceeds 5 MiB."), r);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<?> json(Exception e, HttpServletRequest r) {
    return api(new ApiException(400, "BAD_REQUEST", "Malformed JSON request."), r);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<?> mime(Exception e, HttpServletRequest r) {
    return api(new ApiException(415, "UNSUPPORTED_MEDIA_TYPE", "Unsupported request format."), r);
  }

  @ExceptionHandler({
    org.springframework.web.multipart.support.MissingServletRequestPartException.class,
    org.springframework.web.bind.MissingServletRequestParameterException.class
  })
  public ResponseEntity<?> missingField(Exception e, HttpServletRequest r) {
    return api(new ApiException(400, "BAD_REQUEST", "A required request field is missing."), r);
  }

  @ExceptionHandler(
      org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
  public ResponseEntity<?> mismatch(Exception e, HttpServletRequest r) {
    return api(ApiException.invalid("parameter", "Invalid request parameter."), r);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> other(Exception e, HttpServletRequest r) {
    log.error("Request {} failed", ApiResponses.requestId(r), e);
    return api(new ApiException(500, "INTERNAL_ERROR", "The request could not be completed."), r);
  }
}
