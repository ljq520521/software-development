package hdu.ljq.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import hdu.ljq.common.ApiException;
import hdu.ljq.persistence.*;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
  private static final Logger log = LoggerFactory.getLogger(EmailService.class);
  private final EmailMapper mapper;
  private final ObjectMapper json;
  private final String host;
  private final int port;
  private final String username;
  private final String password;
  private final String from;
  private final boolean starttls;
  private final String baseUrl;

  public EmailService(
      EmailMapper mapper,
      ObjectMapper json,
      @Value("${app.mail.smtp-host:}") String host,
      @Value("${app.mail.smtp-port:587}") int port,
      @Value("${app.mail.username:}") String username,
      @Value("${app.mail.password:}") String password,
      @Value("${app.mail.from:no-reply@wemove.local}") String from,
      @Value("${app.mail.starttls:true}") boolean starttls,
      @Value("${app.public-base-url:http://127.0.0.1:8080}") String baseUrl) {
    this.mapper = mapper;
    this.json = json;
    this.host = host.strip();
    this.port = port;
    this.username = username.strip();
    this.password = password;
    this.from = from.strip();
    this.starttls = starttls;
    this.baseUrl = baseUrl.replaceAll("/+$", "");
  }

  public void contactReceipt(String email, String name, String reference, long id) {
    queue(
        email,
        "We received your WEMOVE message",
        "contact_receipt",
        "contact_inquiry",
        id,
        "Hello " + name + ",\n\nWe received your message.\nReference: " + reference
            + "\n\nOur team will follow up as soon as possible.\n\nWEMOVE SPORTS");
  }

  public void dealerReceipt(String email, String name, String reference, long id) {
    queue(
        email,
        "We received your WEMOVE partner application",
        "dealer_application_receipt",
        "dealer_application",
        id,
        "Hello " + name + ",\n\nYour partner application has been received.\nReference: "
            + reference + "\n\nWe will email you after the review.\n\nWEMOVE SPORTS");
  }

  public void dealerActivation(
      String email, String name, String company, String token, long accountId) {
    String link = baseUrl + "/dealers/activate?token=" + token;
    queue(
        email,
        "Activate your WEMOVE dealer account",
        "dealer_account_activation",
        "dealer_account",
        accountId,
        "Hello " + name + ",\n\nYour application for " + company
            + " has been approved. Set your password within 48 hours:\n" + link
            + "\n\nThis link can be used once.\n\nWEMOVE SPORTS");
  }

  public void dealerRejected(String email, String name, String reference, long applicationId) {
    queue(
        email,
        "Update on your WEMOVE partner application",
        "dealer_application_rejected",
        "dealer_application",
        applicationId,
        "Hello " + name + ",\n\nWe have completed the review of application " + reference
            + ". We are unable to open a dealer account for this application.\n\nWEMOVE SPORTS");
  }

  public void orderCreated(
      String email, String name, String orderNumber, String token, long orderId, long totalCents) {
    String link = baseUrl + "/orders/" + orderNumber + "?token=" + token;
    queue(
        email,
        "Your WEMOVE order " + orderNumber,
        "order_created",
        "order",
        orderId,
        "Hello " + name + ",\n\nYour order has been created and is awaiting payment.\nOrder: "
            + orderNumber + "\nTotal: CNY " + money(totalCents) + "\nOpen order: " + link
            + "\n\nThe payment link expires in 30 minutes.\n\nWEMOVE SPORTS");
  }

  public void paymentReceived(
      String email, String name, String orderNumber, long orderId, long totalCents) {
    queue(
        email,
        "Payment received for " + orderNumber,
        "payment_received",
        "order",
        orderId,
        "Hello " + name + ",\n\nPayment was recorded for order " + orderNumber
            + ".\nAmount: CNY " + money(totalCents)
            + "\n\nThis project uses a demonstration payment gateway.\n\nWEMOVE SPORTS");
  }

  public void queue(
      String recipient,
      String subject,
      String template,
      String relatedType,
      Long relatedId,
      String body) {
    Map<String, Object> mail = new LinkedHashMap<>();
    mail.put("recipient_email", recipient.strip().toLowerCase(Locale.ROOT));
    mail.put("subject", subject);
    mail.put("body_text", body);
    mail.put("template_name", template);
    mail.put("related_type", relatedType);
    mail.put("related_id", relatedId);
    mapper.insert(mail);
  }

  @Scheduled(fixedDelayString = "${app.mail.poll-interval-ms:5000}")
  public void deliverPending() {
    if (!configured()) return;
    for (Map<String, Object> row : mapper.deliverable()) {
      long id = ((Number) row.get("id")).longValue();
      try {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(row.get("recipient_email").toString());
        message.setSubject(row.get("subject").toString());
        message.setText(row.get("body_text").toString());
        sender().send(message);
        mapper.markSent(id);
      } catch (Exception e) {
        String error = Optional.ofNullable(e.getMessage()).orElse(e.getClass().getSimpleName());
        mapper.markFailed(id, error.substring(0, Math.min(1000, error.length())));
        log.warn("Email delivery failed for outbox record {}: {}", id, error);
      }
    }
  }

  public ObjectNode adminList(Map<String, String> query) {
    for (String key : query.keySet())
      if (!Set.of("page", "page_size", "q", "status").contains(key))
        throw ApiException.invalid(key, "Unknown query parameter.");
    int page = Repository.number(query, "page", 1, 1, Integer.MAX_VALUE);
    int size = Repository.number(query, "page_size", 12, 1, 50);
    String search = query.getOrDefault("q", "").strip();
    if (search.length() > 100) throw ApiException.invalid("q", "Maximum 100 characters.");
    String status = query.getOrDefault("status", "");
    if (!status.isEmpty() && !Set.of("pending", "sent", "failed").contains(status))
      throw ApiException.invalid("status", "Unsupported value.");
    String q = search.isEmpty() ? "" : "%" + search.replace("!", "!!").replace("%", "!%")
        .replace("_", "!_") + "%";
    long total = mapper.count(q, status);
    ArrayNode items = json.createArrayNode();
    mapper.list(q, status, size, (long) (page - 1) * size)
        .forEach(row -> items.add(json.valueToTree(clean(row))));
    return json.createObjectNode()
        .set("items", items)
        .<ObjectNode>deepCopy()
        .put("page", page)
        .put("page_size", size)
        .put("total", total)
        .put("total_pages", (total + size - 1) / size)
        .put("smtp_configured", configured());
  }

  public boolean configured() {
    return !host.isBlank();
  }

  private JavaMailSenderImpl sender() {
    JavaMailSenderImpl sender = new JavaMailSenderImpl();
    sender.setHost(host);
    sender.setPort(port);
    sender.setUsername(username);
    sender.setPassword(password);
    sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
    Properties properties = sender.getJavaMailProperties();
    properties.put("mail.smtp.auth", String.valueOf(!username.isBlank()));
    properties.put("mail.smtp.starttls.enable", String.valueOf(starttls));
    properties.put("mail.smtp.connectiontimeout", "10000");
    properties.put("mail.smtp.timeout", "10000");
    properties.put("mail.smtp.writetimeout", "10000");
    return sender;
  }

  private Map<String, Object> clean(Map<String, Object> source) {
    Map<String, Object> result = new LinkedHashMap<>();
    source.forEach(
        (key, value) -> {
          if (value instanceof Timestamp timestamp) result.put(key, timestamp.toInstant().toString());
          else if (value instanceof LocalDateTime local)
            result.put(key, local.toInstant(ZoneOffset.UTC).toString());
          else result.put(key, value);
        });
    return result;
  }

  private String money(long cents) {
    return String.format(Locale.ROOT, "%.2f", cents / 100.0);
  }
}
