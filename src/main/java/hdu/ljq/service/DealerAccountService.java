package hdu.ljq.service;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import hdu.ljq.common.ApiException;
import hdu.ljq.persistence.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DealerAccountService {
  private static final String DUMMY_HASH =
      "$2a$12$W6bx9tT/9BO9GCucUN.KPuqz.Q/qX8pjV6BO5vw3IuOVdiBuCUUsi";
  private final CatalogService catalog;
  private final Repository repository;
  private final ObjectMapper json;
  private final PasswordEncoder encoder;
  private final EmailService emails;
  private final SecureRandom random = new SecureRandom();

  public DealerAccountService(
      CatalogService catalog,
      Repository repository,
      ObjectMapper json,
      PasswordEncoder encoder,
      EmailService emails) {
    this.catalog = catalog;
    this.repository = repository;
    this.json = json;
    this.encoder = encoder;
    this.emails = emails;
  }

  @Transactional
  public JsonNode reviewApplication(
      String id, JsonNode body, String actor, String requestId) {
    JsonNode result = catalog.update(EntityType.APPLICATION, id, body, actor, requestId);
    ObjectNode application = repository.find(EntityType.APPLICATION, id);
    if (application.path("status").asText().equals("closed")) {
      if (application.path("outcome").asText().equals("follow_up")) {
        provision(application, actor, requestId);
      } else if (application.path("outcome").asText().equals("not_fit")) {
        emails.dealerRejected(
            application.path("email").asText(),
            application.path("contact_name").asText(),
            application.path("reference").asText(),
            application.path("id").asLong());
      }
    }
    return result;
  }

  @Transactional
  public ObjectNode activate(JsonNode body) {
    ObjectNode input = object(body);
    exactFields(input, Set.of("token", "password"));
    String token = text(input, "token", 32, 128);
    String password = text(input, "password", 12, 72);
    if (password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw ApiException.invalid("password", "Password must be at most 72 UTF-8 bytes.");
    repository.mapper.lock("dealers");
    ObjectNode account = repository.by(EntityType.DEALER, "activation_token_hash", hash(token));
    if (account == null)
      throw new ApiException(400, "ACTIVATION_INVALID", "The activation link is invalid or has already been used.");
    if (!account.path("status").asText().equals("pending_activation"))
      throw ApiException.conflict("ACCOUNT_ALREADY_ACTIVE", "This account is already active.");
    if (!Instant.parse(account.path("activation_expires_at").asText()).isAfter(Instant.now()))
      throw new ApiException(410, "ACTIVATION_EXPIRED", "The activation link has expired. Contact support for a new link.");
    ObjectNode patch = json.createObjectNode()
        .put("password_hash", encoder.encode(password))
        .put("status", "active")
        .put("activated_at", Instant.now().toString());
    patch.putNull("activation_token_hash");
    patch.putNull("activation_expires_at");
    return publicAccount(
        repository.update(
            EntityType.DEALER, account.path("id").asText(), patch, account.path("version").asInt()));
  }

  @Transactional
  public ObjectNode authenticate(String email, String password) {
    String normalized = email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    ObjectNode account = normalized.isEmpty() ? null : repository.by(EntityType.DEALER, "email", normalized);
    String hash = account == null || account.path("password_hash").isNull()
        ? DUMMY_HASH
        : account.path("password_hash").asText();
    boolean valid = password != null && encoder.matches(password, hash);
    if (account == null || !valid || !account.path("status").asText().equals("active"))
      throw new ApiException(401, "INVALID_CREDENTIALS", "Incorrect email or password.");
    ObjectNode updated = repository.update(
        EntityType.DEALER,
        account.path("id").asText(),
        json.createObjectNode().put("last_login_at", Instant.now().toString()),
        account.path("version").asInt());
    return publicAccount(updated);
  }

  public ObjectNode account(String id) {
    ObjectNode account = repository.find(EntityType.DEALER, id);
    if (!account.path("status").asText().equals("active"))
      throw new ApiException(401, "UNAUTHENTICATED", "Please sign in.");
    return publicAccount(account);
  }

  public ObjectNode publicAccount(ObjectNode account) {
    return json.createObjectNode()
        .put("id", account.path("id").asText())
        .put("email", account.path("email").asText())
        .put("company_name", account.path("company_name").asText())
        .put("contact_name", account.path("contact_name").asText())
        .put("status", account.path("status").asText())
        .put("activated_at", account.path("activated_at").isNull() ? "" : account.path("activated_at").asText())
        .put("last_login_at", account.path("last_login_at").isNull() ? "" : account.path("last_login_at").asText());
  }

  private ObjectNode provision(ObjectNode application, String actor, String requestId) {
    ObjectNode existing = repository.by(EntityType.DEALER, "application_id", application.path("id").asText());
    if (existing != null) return existing;
    existing = repository.by(EntityType.DEALER, "email", application.path("email").asText());
    if (existing != null) {
      if (existing.path("status").asText().equals("active")) return existing;
      throw ApiException.conflict(
          "DEALER_ACCOUNT_EXISTS", "A dealer account already exists for this email.");
    }
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    ObjectNode data = json.createObjectNode()
        .put("application_id", application.path("id").asText())
        .put("email", application.path("email").asText())
        .put("company_name", application.path("company_name").asText())
        .put("contact_name", application.path("contact_name").asText())
        .put("status", "pending_activation")
        .put("activation_token_hash", hash(token))
        .put("activation_expires_at", Instant.now().plusSeconds(48 * 3600).toString());
    data.putNull("password_hash");
    data.putNull("activated_at");
    data.putNull("last_login_at");
    ObjectNode account = repository.create(EntityType.DEALER, data);
    emails.dealerActivation(
        account.path("email").asText(),
        account.path("contact_name").asText(),
        account.path("company_name").asText(),
        token,
        account.path("id").asLong());
    audit(account, actor, requestId);
    return account;
  }

  private void audit(ObjectNode account, String actor, String requestId) {
    ObjectNode visible = json.createObjectNode()
        .put("email", account.path("email").asText())
        .put("status", account.path("status").asText())
        .put("application_id", account.path("application_id").asText());
    ObjectNode audit = json.createObjectNode()
        .put("actor_id", actor)
        .put("action", "create")
        .put("entity_type", "dealer_account")
        .put("entity_id", account.path("id").asText())
        .put("request_id", requestId);
    audit.set("before_data", json.createObjectNode());
    audit.set("after_data", visible);
    repository.create(EntityType.AUDIT, audit);
  }

  private ObjectNode object(JsonNode input) {
    if (!input.isObject()) throw ApiException.invalid("body", "Expected an object.");
    return (ObjectNode) input;
  }

  private void exactFields(ObjectNode input, Set<String> fields) {
    input.fieldNames().forEachRemaining(
        key -> {
          if (!fields.contains(key)) throw ApiException.invalid(key, "Unknown field.");
        });
    for (String field : fields)
      if (!input.has(field)) throw ApiException.invalid(field, "This field is required.");
  }

  private String text(ObjectNode input, String field, int min, int max) {
    if (!input.path(field).isTextual())
      throw ApiException.invalid(field, "This field is required.");
    String value = input.path(field).asText();
    if (field.equals("password") ? value.length() < min || value.length() > max
        : value.strip().length() < min || value.strip().length() > max)
      throw ApiException.invalid(field, "Text length is outside the allowed range.");
    return field.equals("password") ? value : value.strip();
  }

  private String hash(String value) {
    try {
      return HexFormat.of().formatHex(
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }
}
