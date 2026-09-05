package hdu.ljq.web;

import com.fasterxml.jackson.databind.JsonNode;
import hdu.ljq.common.*;
import hdu.ljq.persistence.*;
import hdu.ljq.service.*;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class ApiController {
  private final CatalogService c;
  private final LeadService leads;
  private final MediaService media;
  private final CommerceService commerce;

  public ApiController(CatalogService c, LeadService l, MediaService m, CommerceService commerce) {
    this.c = c;
    leads = l;
    media = m;
    this.commerce = commerce;
  }

  private Object ok(Object d, HttpServletRequest r) {
    return ApiResponses.ok(d, r);
  }

  @GetMapping("/health")
  public Object health(HttpServletRequest r) {
    try {
      c.repository().mapper.ping();
      return ok(Map.of("status", "up"), r);
    } catch (Exception e) {
      throw new ApiException(503, "SERVICE_UNAVAILABLE", "Database unavailable.");
    }
  }

  @GetMapping("/site")
  public Object site(HttpServletRequest r) {
    return ok(c.site(), r);
  }

  @GetMapping("/home")
  public Object home(HttpServletRequest r) {
    return ok(c.home(), r);
  }

  @GetMapping({"/products", "/categories", "/content", "/faqs"})
  public Object list(@RequestParam Map<String, String> q, HttpServletRequest r) {
    return ok(
        c.list(type(r.getRequestURI().substring(r.getRequestURI().lastIndexOf('/') + 1)), q, false),
        r);
  }

  @GetMapping("/products/{slug}")
  public Object product(@PathVariable String slug, HttpServletRequest r) {
    return ok(c.product(slug), r);
  }

  @GetMapping("/content/{slug}")
  public Object content(@PathVariable String slug, HttpServletRequest r) {
    return ok(c.content(slug), r);
  }

  @PostMapping(
      value = {"/forms/contact", "/dealer/applications"},
      consumes = "application/json")
  public ResponseEntity<?> form(
      @RequestBody JsonNode d,
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      HttpServletRequest r) {
    return ResponseEntity.status(201)
        .body(ok(leads.submit(r.getRequestURI().contains("dealer"), d, key, r.getRemoteAddr()), r));
  }

  @GetMapping("/admin/dashboard")
  public Object dashboard(HttpServletRequest r) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("active_products", c.repository().list(EntityType.PRODUCT, Map.of("status", "active"), false).total());
    data.put("published_articles", c.repository().list(EntityType.CONTENT, Map.of("type", "article", "status", "published"), false).total());
    data.put("new_inquiries", c.repository().list(EntityType.INQUIRY, Map.of("status", "new"), false).total());
    data.put("open_dealer_applications", c.repository().list(EntityType.APPLICATION, Map.of("status", "submitted"), false).total() + c.repository().list(EntityType.APPLICATION, Map.of("status", "under_review"), false).total());
    data.putAll(commerce.metrics());
    data.put("generated_at", Instant.now().toString());
    return ok(data, r);
  }

  @GetMapping("/admin/{resource}")
  public Object adminList(
      @PathVariable String resource, @RequestParam Map<String, String> q, HttpServletRequest r) {
    if (resource.equals("orders") || resource.equals("payments"))
      return ok(commerce.adminList(resource.equals("payments"), q), r);
    EntityType t = type(resource);
    return ok(
        t == EntityType.HOME || t == EntityType.SITE ? c.admin(t, "1") : c.list(t, q, true), r);
  }

  @GetMapping("/admin/{resource}/{id}")
  public Object adminOne(
      @PathVariable String resource, @PathVariable String id, HttpServletRequest r) {
    if (resource.equals("orders")) return ok(commerce.adminOrder(id), r);
    EntityType t = type(resource);
    if (!Set.of(EntityType.PRODUCT, EntityType.CONTENT, EntityType.INQUIRY, EntityType.APPLICATION)
        .contains(t)) throw ApiException.missing();
    return ok(c.admin(t, id), r);
  }

  @PostMapping(value = "/admin/{resource}", consumes = "application/json")
  public ResponseEntity<?> create(
      @PathVariable String resource, @RequestBody JsonNode d, HttpServletRequest r) {
    EntityType t = type(resource);
    if (!Set.of(EntityType.PRODUCT, EntityType.CATEGORY, EntityType.CONTENT, EntityType.FAQ)
        .contains(t)) throw ApiException.missing();
    return ResponseEntity.status(201)
        .body(ok(c.create(t, d, actor(r), ApiResponses.requestId(r)), r));
  }

  @PatchMapping(value = "/admin/{resource}/{id}", consumes = "application/json")
  public Object update(
      @PathVariable String resource,
      @PathVariable String id,
      @RequestBody JsonNode d,
      HttpServletRequest r) {
    if (resource.equals("orders"))
      return ok(commerce.updateOrder(id, d, actor(r), ApiResponses.requestId(r)), r);
    EntityType t = type(resource);
    if (!Set.of(
            EntityType.PRODUCT,
            EntityType.CATEGORY,
            EntityType.CONTENT,
            EntityType.FAQ,
            EntityType.INQUIRY,
            EntityType.APPLICATION)
        .contains(t)) throw ApiException.missing();
    return ok(c.update(t, id, d, actor(r), ApiResponses.requestId(r)), r);
  }

  @PatchMapping(value = "/admin/site", consumes = "application/json")
  public Object siteUpdate(@RequestBody JsonNode d, HttpServletRequest r) {
    return ok(c.update(EntityType.SITE, "1", d, actor(r), ApiResponses.requestId(r)), r);
  }

  @PutMapping(value = "/admin/home", consumes = "application/json")
  public Object homeUpdate(@RequestBody JsonNode d, HttpServletRequest r) {
    return ok(c.update(EntityType.HOME, "1", d, actor(r), ApiResponses.requestId(r)), r);
  }

  @PostMapping(value = "/admin/media", consumes = "multipart/form-data")
  public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file, HttpServletRequest r) {
    return ResponseEntity.status(201)
        .body(ok(media.upload(file, actor(r), ApiResponses.requestId(r)), r));
  }

  private String actor(HttpServletRequest r) {
    return r.getSession().getAttribute("actorId").toString();
  }

  private EntityType type(String s) {
    return switch (s) {
      case "products" -> EntityType.PRODUCT;
      case "categories" -> EntityType.CATEGORY;
      case "content" -> EntityType.CONTENT;
      case "faqs" -> EntityType.FAQ;
      case "site" -> EntityType.SITE;
      case "home" -> EntityType.HOME;
      case "inquiries" -> EntityType.INQUIRY;
      case "dealer-applications" -> EntityType.APPLICATION;
      case "media" -> EntityType.MEDIA;
      case "audit-logs" -> EntityType.AUDIT;
      default -> throw ApiException.missing();
    };
  }
}
