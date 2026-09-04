package hdu.ljq.service;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import hdu.ljq.common.*;
import hdu.ljq.persistence.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeadService {
  private final CatalogService catalog;
  private final RateLimiter limits;

  public LeadService(CatalogService c, RateLimiter limits) {
    this.catalog = c;
    this.limits = limits;
  }

  @Transactional
  public JsonNode submit(boolean dealer, JsonNode body, String key, String ip) {
    if (key == null) throw new ApiException(400, "BAD_REQUEST", "Idempotency-Key is required.");
    try {
      if (!UUID.fromString(key).toString().equalsIgnoreCase(key)) throw new Exception();
    } catch (Exception e) {
      throw new ApiException(400, "BAD_REQUEST", "Idempotency-Key must be a UUID.");
    }
    ObjectNode d =
        catalog.contract().input(dealer ? "DealerApplicationCreate" : "ContactCreate", body);
    catalog.normalize(d);
    if (dealer) {
      if (!d.has("website")) d.put("website", "");
      if (!d.has("interested_product_ids"))
        d.set("interested_product_ids", catalog.json().createArrayNode());
    }
    String endpoint = dealer ? "/dealer/applications" : "/forms/contact";
    String hash = hash(canonical(d).toString());
    catalog.repository().mapper.lock("leads");
    Map<String, Object> previous = catalog.repository().mapper.receipt(endpoint, key);
    if (previous != null) {
      if (!hash.equals(previous.get("request_hash").toString()))
        throw ApiException.conflict(
            "IDEMPOTENCY_CONFLICT", "The submission key was already used for different data.");
      try {
        return catalog.json().readTree(previous.get("response_data").toString());
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    }
    catalog.repository().mapper.expireReceipt(endpoint, key);
    limits.check("forms:" + ip, 5, 60000, true);
    if (!catalog.site().path("privacy_version").equals(d.path("privacy_version")))
      throw ApiException.conflict(
          "PRIVACY_VERSION_CHANGED",
          "Privacy information changed. Please review it and submit again.");
    if (!List.of(Locale.getISOCountries()).contains(d.path("country").asText()))
      throw ApiException.invalid("country", "Choose a valid country.");
    if (dealer) {
      String dedupe =
          hash(
              d.path("email").asText()
                  + "\n"
                  + d.path("company_name")
                      .asText()
                      .replaceAll("\\s+", " ")
                      .toLowerCase(Locale.ROOT));
      if (catalog.repository().by(EntityType.APPLICATION, "open_dedupe_key", dedupe) != null)
        throw ApiException.conflict(
            "APPLICATION_ALREADY_OPEN",
            "An application for this company and email is already being reviewed.");
      d.put("open_dedupe_key", dedupe).put("outcome", "");
      for (JsonNode id : d.path("interested_product_ids")) product(id.asText());
    } else {
      if (d.path("type").asText().equals("product_question") && !d.has("product_id"))
        throw ApiException.invalid("product_id", "Choose a product.");
      if (d.has("product_id")) product(d.path("product_id").asText());
      else d.putNull("product_id");
    }
    String state = dealer ? "submitted" : "new",
        reference = (dealer ? "DA-" : "CT-") + UUID.randomUUID(),
        now = Instant.now().toString();
    d.put("reference", reference)
        .put("status", state)
        .put("internal_note", "")
        .put("consent_at", now);
    d.remove("privacy_consent");
    catalog.repository().create(dealer ? EntityType.APPLICATION : EntityType.INQUIRY, d);
    ObjectNode receipt =
        catalog
            .json()
            .createObjectNode()
            .put("reference", reference)
            .put("status", state)
            .put("received_at", now);
    catalog.repository().mapper.saveReceipt(endpoint, key, hash, receipt.toString());
    return receipt;
  }

  private void product(String id) {
    try {
      if (!catalog.visible(catalog.repository().find(EntityType.PRODUCT, id)))
        throw ApiException.missing();
    } catch (ApiException e) {
      throw ApiException.invalid("product_id", "This product is not available.");
    }
  }

  private JsonNode canonical(JsonNode n) {
    if (n.isObject()) {
      ObjectNode o = catalog.json().createObjectNode();
      TreeSet<String> keys = new TreeSet<>();
      n.fieldNames().forEachRemaining(keys::add);
      keys.forEach(k -> o.set(k, canonical(n.get(k))));
      return o;
    }
    if (n.isArray()) {
      ArrayNode a = catalog.json().createArrayNode();
      n.forEach(x -> a.add(canonical(x)));
      return a;
    }
    return n;
  }

  private String hash(String text) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
