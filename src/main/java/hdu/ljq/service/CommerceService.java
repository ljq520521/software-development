package hdu.ljq.service;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import hdu.ljq.common.*;
import hdu.ljq.persistence.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommerceService {
  private static final Set<String> ORDER_FIELDS =
      Set.of(
          "product_id",
          "quantity",
          "customer_name",
          "email",
          "phone",
          "address_line1",
          "address_line2",
          "city",
          "region",
          "postal_code",
          "country",
          "privacy_consent",
          "privacy_version");
  private static final Set<String> ORDER_STATUSES =
      Set.of("pending_payment", "paid", "processing", "shipped", "completed", "cancelled", "refunded");
  private static final Set<String> PAYMENT_METHODS =
      Set.of("demo_card", "demo_alipay", "demo_wechat");

  private final CommerceMapper mapper;
  private final CatalogService catalog;
  private final RateLimiter limits;
  private final ObjectMapper json;

  public CommerceService(
      CommerceMapper mapper, CatalogService catalog, RateLimiter limits, ObjectMapper json) {
    this.mapper = mapper;
    this.catalog = catalog;
    this.limits = limits;
    this.json = json;
  }

  public Map<String, Object> checkoutProduct(String value) {
    long id = Repository.id(value);
    Map<String, Object> product = mapper.purchasableProduct(id);
    if (product == null) throw ApiException.missing();
    return clean(product);
  }

  @Transactional
  public JsonNode createOrder(JsonNode input, String key, String ip) {
    requireUuidKey(key);
    if (!input.isObject()) throw ApiException.invalid("body", "Expected an object.");
    input.fieldNames().forEachRemaining(k -> {
      if (!ORDER_FIELDS.contains(k)) throw ApiException.invalid(k, "Unknown field.");
    });
    ObjectNode d = (ObjectNode) input.deepCopy();
    String productId = text(d, "product_id", 1, 19);
    long id = Repository.id(productId);
    int quantity = integer(d, "quantity", 1, 20);
    String customerName = text(d, "customer_name", 2, 120);
    String email = text(d, "email", 5, 191).toLowerCase(Locale.ROOT);
    if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
      throw ApiException.invalid("email", "Enter a valid email address.");
    String phone = text(d, "phone", 6, 40);
    String line1 = text(d, "address_line1", 3, 200);
    String line2 = optionalText(d, "address_line2", 200);
    String city = text(d, "city", 1, 100);
    String region = optionalText(d, "region", 100);
    String postalCode = text(d, "postal_code", 2, 20);
    String country = text(d, "country", 2, 2).toUpperCase(Locale.ROOT);
    if (!List.of(Locale.getISOCountries()).contains(country))
      throw ApiException.invalid("country", "Choose a valid country.");
    if (!d.path("privacy_consent").isBoolean() || !d.path("privacy_consent").asBoolean())
      throw ApiException.invalid("privacy_consent", "Consent is required.");
    String privacyVersion = text(d, "privacy_version", 1, 40);
    if (!catalog.site().path("privacy_version").asText().equals(privacyVersion))
      throw ApiException.conflict(
          "PRIVACY_VERSION_CHANGED", "Privacy information changed. Please review it and submit again.");

    ObjectNode canonical = json.createObjectNode();
    new TreeSet<>(ORDER_FIELDS).forEach(k -> canonical.set(k, d.get(k)));
    String requestHash = hash(canonical.toString());
    catalog.repository().mapper.lock("commerce");
    Map<String, Object> previous = catalog.repository().mapper.receipt("/orders", key);
    if (previous != null) {
      if (!requestHash.equals(previous.get("request_hash").toString()))
        throw ApiException.conflict(
            "IDEMPOTENCY_CONFLICT", "The order key was already used for different data.");
      try {
        return json.readTree(previous.get("response_data").toString());
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    }
    catalog.repository().mapper.expireReceipt("/orders", key);
    limits.check("orders:" + ip, 10, 60000, true);

    Map<String, Object> product = mapper.purchasableProduct(id);
    if (product == null) throw ApiException.invalid("product_id", "This product is not available.");
    long price = ((Number) product.get("price_cents")).longValue();
    long total = Math.multiplyExact(price, quantity);
    String token = UUID.randomUUID().toString();
    Map<String, Object> order = new LinkedHashMap<>();
    order.put("order_number", "WM-" + Instant.now().toEpochMilli() + "-" + randomCode(6));
    order.put("access_token_hash", hash(token));
    order.put("customer_name", customerName);
    order.put("email", email);
    order.put("phone", phone);
    ObjectNode address = json.createObjectNode();
    address.put("address_line1", line1);
    address.put("address_line2", line2);
    address.put("city", city);
    address.put("region", region);
    address.put("postal_code", postalCode);
    address.put("country", country);
    order.put("shipping_address", address.toString());
    order.put("currency", product.get("currency").toString());
    order.put("subtotal_cents", total);
    order.put("shipping_cents", 0L);
    order.put("total_cents", total);
    mapper.insertOrder(order);
    Map<String, Object> item = new LinkedHashMap<>();
    item.put("order_id", order.get("id"));
    item.put("product_id", product.get("id"));
    item.put("product_name", product.get("name"));
    item.put("sku", product.get("sku"));
    item.put("unit_price_cents", price);
    item.put("quantity", quantity);
    item.put("line_total_cents", total);
    mapper.insertItem(item);
    ObjectNode result = orderJson(mapper.orderByNumber(order.get("order_number").toString()), true);
    result.put("access_token", token);
    catalog.repository().mapper.saveReceipt("/orders", key, requestHash, result.toString());
    return result;
  }

  public ObjectNode publicOrder(String number, String token) {
    Map<String, Object> order = mapper.orderByNumber(validNumber(number));
    verify(order, token);
    return orderJson(order, true);
  }

  @Transactional
  public ObjectNode pay(String number, JsonNode input, String ip) {
    if (!input.isObject() || input.size() != 2 || !input.has("access_token") || !input.has("method"))
      throw ApiException.invalid("body", "Provide access_token and method.");
    String token = text((ObjectNode) input, "access_token", 36, 36);
    String method = text((ObjectNode) input, "method", 1, 40);
    if (!PAYMENT_METHODS.contains(method)) throw ApiException.invalid("method", "Choose a payment method.");
    limits.check("payments:" + ip, 10, 60000, true);
    Map<String, Object> order = mapper.lockedOrderByNumber(validNumber(number));
    verify(order, token);
    String state = order.get("status").toString();
    if (state.equals("paid") || !mapper.payments(((Number) order.get("id")).longValue()).isEmpty())
      return orderJson(order, true);
    if (!state.equals("pending_payment"))
      throw ApiException.conflict("ORDER_NOT_PAYABLE", "This order can no longer be paid.");
    if (!instant(order.get("expires_at")).isAfter(Instant.now()))
      throw ApiException.conflict("ORDER_EXPIRED", "This order has expired. Please place it again.");
    Map<String, Object> payment = new LinkedHashMap<>();
    payment.put("order_id", order.get("id"));
    payment.put("payment_number", "PAY-" + Instant.now().toEpochMilli() + "-" + randomCode(6));
    payment.put("method", method);
    payment.put("amount_cents", order.get("total_cents"));
    payment.put("currency", order.get("currency"));
    payment.put("provider_reference", "DEMO-" + UUID.randomUUID());
    mapper.insertPayment(payment);
    if (mapper.markPaid(((Number) order.get("id")).longValue(), Timestamp.from(Instant.now())) != 1)
      throw ApiException.conflict("ORDER_NOT_PAYABLE", "This order can no longer be paid.");
    return orderJson(mapper.orderByNumber(number), true);
  }

  public ObjectNode adminList(boolean payments, Map<String, String> query) {
    Set<String> allowed = Set.of("page", "page_size", "q", "status");
    for (String key : query.keySet())
      if (!allowed.contains(key)) throw ApiException.invalid(key, "Unknown query parameter.");
    int page = Repository.number(query, "page", 1, 1, Integer.MAX_VALUE);
    int size = Repository.number(query, "page_size", 12, 1, 50);
    String q = query.getOrDefault("q", "").strip();
    if (q.length() > 100) throw ApiException.invalid("q", "Maximum 100 characters.");
    if (!q.isEmpty()) q = "%" + q.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    String status = query.getOrDefault("status", "");
    if (payments) {
      if (!status.isEmpty() && !Set.of("succeeded", "refunded").contains(status))
        throw ApiException.invalid("status", "Unsupported value.");
    } else if (!status.isEmpty() && !ORDER_STATUSES.contains(status))
      throw ApiException.invalid("status", "Unsupported value.");
    long total = payments ? mapper.countPayments(q, status) : mapper.countOrders(q, status);
    List<Map<String, Object>> rows =
        payments
            ? mapper.listPayments(q, status, size, (long) (page - 1) * size)
            : mapper.listOrders(q, status, size, (long) (page - 1) * size);
    ArrayNode items = json.createArrayNode();
    rows.forEach(row -> items.add(json.valueToTree(clean(row))));
    return json.createObjectNode()
        .set("items", items)
        .<ObjectNode>deepCopy()
        .put("page", page)
        .put("page_size", size)
        .put("total", total)
        .put("total_pages", (total + size - 1) / size);
  }

  public ObjectNode adminOrder(String id) {
    Map<String, Object> order = mapper.orderById(Repository.id(id));
    if (order == null) throw ApiException.missing();
    return orderJson(order, false);
  }

  @Transactional
  public ObjectNode updateOrder(String id, JsonNode input, String actor, String requestId) {
    if (!input.isObject()) throw ApiException.invalid("body", "Expected an object.");
    input.fieldNames().forEachRemaining(k -> {
      if (!Set.of("version", "status", "internal_note").contains(k))
        throw ApiException.invalid(k, "Unknown field.");
    });
    ObjectNode d = (ObjectNode) input;
    int version = integer(d, "version", 1, Integer.MAX_VALUE);
    String requested = text(d, "status", 1, 40);
    String note = optionalText(d, "internal_note", 5000);
    Map<String, Object> before = mapper.orderById(Repository.id(id));
    if (before == null) throw ApiException.missing();
    String current = before.get("status").toString();
    Map<String, Set<String>> transitions = Map.of(
        "pending_payment", Set.of("pending_payment", "cancelled"),
        "paid", Set.of("paid", "processing", "refunded"),
        "processing", Set.of("processing", "shipped", "refunded"),
        "shipped", Set.of("shipped", "completed", "refunded"),
        "completed", Set.of("completed", "refunded"),
        "cancelled", Set.of("cancelled"),
        "refunded", Set.of("refunded"));
    if (!transitions.getOrDefault(current, Set.of()).contains(requested))
      throw ApiException.conflict("INVALID_STATE", "This order status transition is not allowed.");
    String paymentStatus = before.get("payment_status").toString();
    if (requested.equals("refunded")) {
      if (!paymentStatus.equals("paid"))
        throw ApiException.conflict("INVALID_STATE", "Only a paid order can be refunded.");
      mapper.refundPayments(((Number) before.get("id")).longValue());
      paymentStatus = "refunded";
    }
    if (mapper.updateOrder(Repository.id(id), version, requested, paymentStatus, note) != 1)
      throw ApiException.conflict("VERSION_CONFLICT", "This record changed. Refresh before saving.");
    ObjectNode result = adminOrder(id);
    ObjectNode beforeData = json.createObjectNode()
        .put("status", current)
        .put("payment_status", before.get("payment_status").toString())
        .put("internal_note", Objects.toString(before.get("internal_note"), ""));
    ObjectNode afterData = json.createObjectNode()
        .put("status", requested)
        .put("payment_status", paymentStatus)
        .put("internal_note", note);
    ObjectNode audit = json.createObjectNode()
        .put("actor_id", actor)
        .put("action", requested.equals("refunded") ? "refund" : "update")
        .put("entity_type", "order")
        .put("entity_id", id)
        .put("request_id", requestId);
    audit.set("before_data", beforeData);
    audit.set("after_data", afterData);
    catalog.repository().create(EntityType.AUDIT, audit);
    return result;
  }

  public Map<String, Object> metrics() {
    return Map.of("pending_orders", mapper.pendingOrders(), "paid_revenue_cents", mapper.paidRevenue());
  }

  private ObjectNode orderJson(Map<String, Object> order, boolean customer) {
    if (order == null) throw ApiException.missing();
    long id = ((Number) order.get("id")).longValue();
    ObjectNode out = json.valueToTree(clean(order));
    out.remove("access_token_hash");
    if (order.get("shipping_address") != null) {
      try {
        Object raw = order.get("shipping_address");
        out.set("shipping_address", json.readTree(raw instanceof byte[] b ? new String(b, StandardCharsets.UTF_8) : raw.toString()));
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    }
    out.set("items", json.valueToTree(mapper.items(id).stream().map(this::clean).toList()));
    out.set("payments", json.valueToTree(mapper.payments(id).stream().map(this::clean).toList()));
    if (customer) out.remove("internal_note");
    return out;
  }

  private void verify(Map<String, Object> order, String token) {
    if (order == null || token == null || !MessageDigest.isEqual(
        hash(token).getBytes(StandardCharsets.UTF_8),
        String.valueOf(order.get("access_token_hash")).getBytes(StandardCharsets.UTF_8)))
      throw ApiException.missing();
  }

  private String validNumber(String number) {
    if (number == null || !number.matches("^WM-[0-9]{13}-[A-Z0-9]{6}$")) throw ApiException.missing();
    return number;
  }

  private String text(ObjectNode d, String key, int min, int max) {
    if (!d.path(key).isTextual()) throw ApiException.invalid(key, "This field is required.");
    String value = d.path(key).asText().strip();
    if (value.length() < min || value.length() > max) throw ApiException.invalid(key, "Text length is outside the allowed range.");
    return value;
  }

  private String optionalText(ObjectNode d, String key, int max) {
    if (!d.has(key) || d.path(key).isNull()) return "";
    if (!d.path(key).isTextual()) throw ApiException.invalid(key, "Expected text.");
    String value = d.path(key).asText().strip();
    if (value.length() > max) throw ApiException.invalid(key, "Text is too long.");
    return value;
  }

  private int integer(ObjectNode d, String key, int min, int max) {
    if (!d.path(key).isIntegralNumber()) throw ApiException.invalid(key, "Enter an integer.");
    int value = d.path(key).asInt();
    if (value < min || value > max) throw ApiException.invalid(key, "Enter an integer in the allowed range.");
    return value;
  }

  private void requireUuidKey(String key) {
    try {
      if (key == null || !UUID.fromString(key).toString().equalsIgnoreCase(key)) throw new Exception();
    } catch (Exception e) {
      throw new ApiException(400, "BAD_REQUEST", "Idempotency-Key must be a UUID.");
    }
  }

  private Map<String, Object> clean(Map<String, Object> source) {
    Map<String, Object> out = new LinkedHashMap<>();
    source.forEach((key, value) -> {
      if (key.equals("access_token_hash")) return;
      if (value instanceof Timestamp t) out.put(key, t.toInstant().toString());
      else if (value instanceof LocalDateTime t) out.put(key, t.toInstant(ZoneOffset.UTC).toString());
      else if (value instanceof byte[] b) out.put(key, new String(b, StandardCharsets.UTF_8));
      else out.put(key, value);
    });
    return out;
  }

  private String randomCode(int length) {
    String value = UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    return value.substring(0, length);
  }

  private Instant instant(Object value) {
    if (value instanceof Timestamp timestamp) return timestamp.toInstant();
    if (value instanceof LocalDateTime local) return local.toInstant(ZoneOffset.UTC);
    return Instant.parse(value.toString());
  }

  private String hash(String text) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
