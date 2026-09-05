package hdu.ljq.web;

import com.fasterxml.jackson.databind.JsonNode;
import hdu.ljq.common.ApiResponses;
import hdu.ljq.service.CommerceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class CommerceController {
  private final CommerceService commerce;

  public CommerceController(CommerceService commerce) {
    this.commerce = commerce;
  }

  @PostMapping(value = "/orders", consumes = "application/json")
  public ResponseEntity<?> create(
      @RequestBody JsonNode body,
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      HttpServletRequest request) {
    return ResponseEntity.status(201)
        .body(ApiResponses.ok(commerce.createOrder(body, key, request.getRemoteAddr()), request));
  }

  @GetMapping("/orders/{number}")
  public Object get(
      @PathVariable String number, @RequestParam String access_token, HttpServletRequest request) {
    return ApiResponses.ok(commerce.publicOrder(number, access_token), request);
  }

  @PostMapping(value = "/orders/{number}/payments", consumes = "application/json")
  public Object pay(
      @PathVariable String number, @RequestBody JsonNode body, HttpServletRequest request) {
    return ApiResponses.ok(commerce.pay(number, body, request.getRemoteAddr()), request);
  }
}
