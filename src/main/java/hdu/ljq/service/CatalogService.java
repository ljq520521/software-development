package hdu.ljq.service;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import hdu.ljq.common.*;
import hdu.ljq.persistence.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {
  private final Repository repo;
  private final ObjectMapper json;
  private final Contract contract;

  public Repository repository() {
    return repo;
  }

  public ObjectMapper json() {
    return json;
  }

  public Contract contract() {
    return contract;
  }

  public CatalogService(Repository repo, ObjectMapper json, Contract contract) {
    this.repo = repo;
    this.json = json;
    this.contract = contract;
  }

  public String model(EntityType t) {
    return switch (t) {
      case PRODUCT -> "Product";
      case CATEGORY -> "Category";
      case CONTENT -> "Content";
      case FAQ -> "Faq";
      case SITE -> "Site";
      case HOME -> "Home";
      case INQUIRY -> "Inquiry";
      case APPLICATION -> "DealerApplication";
      case MEDIA -> "Media";
      case AUDIT -> "AuditLog";
      default -> throw ApiException.missing();
    };
  }

  public ObjectNode list(EntityType t, Map<String, String> q, boolean admin) {
    Repository.Result r = repo.list(t, q, !admin);
    ArrayNode items = json.createArrayNode();
    String output =
        admin
            ? switch (t) {
              case INQUIRY -> "InquirySummary";
              case APPLICATION -> "DealerApplicationSummary";
              case MEDIA, AUDIT -> model(t);
              default -> model(t) + "Admin";
            }
            : switch (t) {
              case PRODUCT -> "ProductCard";
              case CONTENT -> "ContentCard";
              default -> model(t);
            };
    r.items().forEach(v -> items.add(contract.output(output, enrich(t, v))));
    return json.createObjectNode()
        .set("items", items)
        .<ObjectNode>deepCopy()
        .put("page", r.page())
        .put("page_size", r.size())
        .put("total", r.total())
        .put("total_pages", (r.total() + r.size() - 1) / r.size());
  }

  public JsonNode admin(EntityType t, String id) {
    return contract.output(model(t) + "Admin", enrich(t, repo.find(t, id)));
  }

  public JsonNode product(String slug) {
    ObjectNode p = repo.by(EntityType.PRODUCT, "slug", slug);
    if (p == null || !visible(p)) throw ApiException.missing();
    return contract.output("ProductDetail", enrich(EntityType.PRODUCT, p));
  }

  public JsonNode content(String slug) {
    ObjectNode c = repo.by(EntityType.CONTENT, "slug", slug);
    if (c == null || !c.path("status").asText().equals("published")) throw ApiException.missing();
    return contract.output("ContentDetail", enrich(EntityType.CONTENT, c));
  }

  public boolean visible(ObjectNode p) {
    return p.path("status").asText().equals("active")
        && repo.find(EntityType.CATEGORY, p.path("category_id").asText())
            .path("enabled")
            .asBoolean();
  }

  public JsonNode site() {
    ObjectNode site = repo.find(EntityType.SITE, "1");
    site.put("locale", "en").put("commerce_enabled", true).put("dealer_portal_enabled", true);
    return contract.output("Site", site);
  }

  public JsonNode home() {
    ObjectNode home = repo.find(EntityType.HOME, "1");
    ((ObjectNode) home.path("hero")).set("image", image(home.path("hero").path("image")));
    ArrayNode categories = json.createArrayNode();
    repo.all(EntityType.CATEGORY).stream()
        .filter(c -> c.path("enabled").asBoolean())
        .sorted(Comparator.comparingInt(c -> c.path("sort_order").asInt()))
        .forEach(c -> categories.add(contract.output("Category", c)));
    home.set("categories", categories);
    ArrayNode featured = json.createArrayNode();
    for (JsonNode id : home.path("featured_product_ids")) {
      try {
        ObjectNode p = repo.find(EntityType.PRODUCT, id.asText());
        if (visible(p)) featured.add(contract.output("ProductCard", enrich(EntityType.PRODUCT, p)));
      } catch (ApiException ignored) {
      }
    }
    home.set("featured_products", featured);
    home.set(
        "articles",
        list(EntityType.CONTENT, Map.of("type", "article", "page_size", "3"), false).path("items"));
    String href = home.path("hero").path("primary_cta").path("href").asText();
    if (!validLink(href))
      ((ObjectNode) home.path("hero").path("primary_cta"))
          .put("href", "/products")
          .put("label", "Explore products");
    return contract.output("Home", home);
  }

  public ObjectNode enrich(EntityType t, ObjectNode raw) {
    ObjectNode n = raw.deepCopy();
    if (t == EntityType.PRODUCT) {
      ArrayNode imgs = json.createArrayNode();
      n.path("images").forEach(im -> imgs.add(image(im)));
      n.set("images", imgs);
      n.set("cover", imgs.isEmpty() ? NullNode.instance : imgs.get(0));
      n.set("category", repo.find(EntityType.CATEGORY, n.path("category_id").asText()));
    }
    if (t == EntityType.CONTENT) {
      ArrayNode a = json.createArrayNode();
      n.path("cover").forEach(i -> a.add(image(i)));
      n.set("cover", a);
    }
    if (t == EntityType.HOME)
      ((ObjectNode) n.path("hero")).set("image", image(n.path("hero").path("image")));
    return n;
  }

  private ObjectNode image(JsonNode ref) {
    ObjectNode media = repo.find(EntityType.MEDIA, ref.path("media_id").asText());
    return json.createObjectNode()
        .put("media_id", media.path("id").asText())
        .put("url", media.path("url").asText())
        .put("alt", ref.path("alt").asText());
  }

  @Transactional
  public JsonNode create(EntityType t, JsonNode body, String actor, String requestId) {
    repo.mapper.lock("catalog");
    ObjectNode d = contract.input(model(t) + "Create", body);
    normalize(d);
    if (t == EntityType.PRODUCT || t == EntityType.CONTENT) {
      d.put("status", "draft");
      d.putNull("first_published_at");
    }
    if (t == EntityType.CONTENT) d.put("is_system", false);
    if (t == EntityType.CATEGORY && repo.count(t) >= 1000)
      throw ApiException.invalid("name", "Maximum 1000 categories.");
    validate(t, d, null);
    ObjectNode out = repo.create(t, d);
    audit(t, actor, "create", null, out, requestId);
    return contract.output(model(t) + "Admin", enrich(t, out));
  }

  @Transactional
  public JsonNode update(EntityType t, String id, JsonNode body, String actor, String requestId) {
    repo.mapper.lock(t == EntityType.INQUIRY || t == EntityType.APPLICATION ? "leads" : "catalog");
    String m = t == EntityType.HOME ? "HomeWrite" : model(t) + "Patch";
    ObjectNode patch = contract.input(m, body);
    normalize(patch);
    ObjectNode before = repo.find(t, id);
    if (before.path("version").asInt() != patch.path("version").asInt())
      throw ApiException.conflict(
          "VERSION_CONFLICT", "This record changed. Refresh before saving.");
    ObjectNode merged = before.deepCopy();
    patch.fields().forEachRemaining(e -> merged.set(e.getKey(), e.getValue()));
    validate(t, merged, before);
    ObjectNode out = repo.update(t, id, merged, patch.path("version").asInt());
    audit(t, actor, "update", before, out, requestId);
    return contract.output(model(t) + "Admin", enrich(t, out));
  }

  public void normalize(ObjectNode d) {
    d.fields()
        .forEachRemaining(
            e -> {
              if (e.getValue().isTextual() && !e.getKey().equals("password"))
                d.put(e.getKey(), e.getValue().asText().strip());
            });
    if (d.has("email")) d.put("email", d.path("email").asText().toLowerCase(Locale.ROOT));
    if (d.has("sku")) d.put("sku", d.path("sku").asText().toUpperCase(Locale.ROOT));
  }

  public void validate(EntityType t, ObjectNode d, ObjectNode old) {
    if (t == EntityType.PRODUCT) {
      ObjectNode cat = repo.find(EntityType.CATEGORY, d.path("category_id").asText());
      if (d.path("price_cents").asLong() < 1 || d.path("price_cents").asLong() > 99_999_999)
        throw ApiException.invalid("price_cents", "Enter a product price between ¥0.01 and ¥999,999.99.");
      if (!d.path("currency").asText().equals("CNY"))
        throw ApiException.invalid("currency", "Only CNY is supported in this version.");
      if (d.path("age_min").asInt() > d.path("age_max").asInt())
        throw ApiException.invalid("age_max", "Must be at least the minimum age.");
      boolean active = d.path("status").asText().equals("active");
      images(d.path("images"), active);
      if (active) {
        if (!cat.path("enabled").asBoolean())
          throw ApiException.invalid("category_id", "Choose an enabled category.");
        for (String k : List.of("short_description", "description_markdown")) required(d, k);
        if (d.path("features").size() < 3)
          throw ApiException.invalid(
              "features", "At least three features are required to publish.");
        if (d.path("specifications").isEmpty())
          throw ApiException.invalid("specifications", "At least one specification is required.");
        seo(d);
      }
      if (old != null)
        transition(
            old,
            d,
            Map.of(
                "draft",
                "active archived",
                "active",
                "hidden archived",
                "hidden",
                "active archived",
                "archived",
                "draft"));
      firstPublished(d, old, active);
    }
    if (t == EntityType.CATEGORY && !d.path("enabled").asBoolean()) {
      boolean inUse =
          repo.all(EntityType.PRODUCT).stream()
              .anyMatch(
                  p ->
                      p.path("category_id").asText().equals(d.path("id").asText())
                          && !p.path("status").asText().equals("archived"));
      if (inUse)
        throw ApiException.conflict(
            "RESOURCE_IN_USE", "Move or archive the category's products first.");
    }
    if (t == EntityType.CONTENT) {
      boolean published = d.path("status").asText().equals("published");
      images(d.path("cover"), published && d.path("type").asText().equals("article"));
      if (published) {
        required(d, "excerpt");
        required(d, "body_markdown");
        seo(d);
      }
      if (old != null) {
        if (old.path("is_system").asBoolean()
            && (!d.path("status").asText().equals("published")
                || !d.path("slug").equals(old.path("slug"))
                || !d.path("type").equals(old.path("type"))))
          throw ApiException.conflict(
              "INVALID_STATE", "System pages must remain published at their fixed route.");
        transition(
            old,
            d,
            Map.of(
                "draft", "published archived", "published", "draft archived", "archived", "draft"));
        if (!old.path("first_published_at").isNull() && !old.path("type").equals(d.path("type")))
          throw ApiException.conflict("SLUG_LOCKED", "Published content type cannot change.");
      }
      firstPublished(d, old, published);
    }
    if (t == EntityType.HOME) {
      if (!new HashSet<>(
              List.of("hero", "categories", "featured_products", "articles", "dealer_cta"))
          .equals(json.convertValue(d.path("section_order"), Set.class)))
        throw ApiException.invalid("section_order", "Include every homepage section exactly once.");
      images(json.createArrayNode().add(d.path("hero").path("image")), true);
      String href = d.path("hero").path("primary_cta").path("href").asText();
      if (!validLink(href))
        throw ApiException.invalid("hero.primary_cta.href", "Choose an available website page.");
      for (JsonNode id : d.path("featured_product_ids"))
        if (!visible(repo.find(EntityType.PRODUCT, id.asText())))
          throw ApiException.invalid(
              "featured_product_ids", "Featured products must be published.");
    }
    if (t == EntityType.INQUIRY) {
      transition(
          old,
          d,
          Map.of(
              "new",
              "in_progress closed",
              "in_progress",
              "resolved closed",
              "resolved",
              "in_progress closed",
              "closed",
              ""));
      if (List.of("resolved", "closed").contains(d.path("status").asText()))
        required(d, "internal_note");
    }
    if (t == EntityType.APPLICATION) {
      transition(
          old,
          d,
          Map.of("submitted", "under_review closed", "under_review", "closed", "closed", ""));
      if (d.path("status").asText().equals("closed")) {
        required(d, "internal_note");
        required(d, "outcome");
        d.putNull("open_dedupe_key");
      } else if (!d.path("outcome").asText().isEmpty())
        throw ApiException.invalid("outcome", "Outcome is set only when closing an application.");
    }
  }

  private void firstPublished(ObjectNode d, ObjectNode old, boolean published) {
    if (old != null
        && !old.path("first_published_at").isNull()
        && !old.path("slug").equals(d.path("slug")))
      throw ApiException.conflict("SLUG_LOCKED", "Published URLs cannot be changed.");
    if (published && d.path("first_published_at").isNull())
      d.put("first_published_at", Instant.now().toString());
  }

  public static void required(JsonNode d, String k) {
    if (d.path(k).asText().isBlank()) throw ApiException.invalid(k, "This field is required.");
  }

  private void seo(ObjectNode d) {
    required(d.path("seo"), "title");
    required(d.path("seo"), "description");
  }

  private void images(JsonNode images, boolean required) {
    if (required && images.isEmpty())
      throw ApiException.invalid("images", "Add a photo before publishing.");
    for (JsonNode im : images) {
      repo.find(EntityType.MEDIA, im.path("media_id").asText());
      if (required) required(im, "alt");
    }
  }

  private void transition(ObjectNode old, ObjectNode d, Map<String, String> allowed) {
    String from = old.path("status").asText(), to = d.path("status").asText();
    if (!from.equals(to) && !List.of(allowed.getOrDefault(from, "").split(" ")).contains(to))
      throw ApiException.conflict("INVALID_STATE", "This status transition is not allowed.");
  }

  public boolean validLink(String href) {
    if (href == null
        || href.contains("\\")
        || href.contains("%")
        || href.chars().anyMatch(c -> c < 32)
        || href.startsWith("//")) return false;
    String path = href.split("\\?", 2)[0];
    if (Set.of(
            "/",
            "/products",
            "/play",
            "/about",
            "/quality-safety",
            "/support",
            "/support/faq",
            "/privacy",
            "/terms",
            "/contact",
            "/dealers/apply")
        .contains(path)) return true;
    try {
      if (path.startsWith("/products/")) {
        product(path.substring(10));
        return true;
      }
      if (path.startsWith("/play/") || path.startsWith("/pages/")) {
        String prefix = path.startsWith("/play/") ? "/play/" : "/pages/";
        return content(path.substring(prefix.length()))
            .path("type")
            .asText()
            .equals(prefix.equals("/play/") ? "article" : "page");
      }
    } catch (ApiException ignored) {
    }
    return false;
  }

  public void audit(
      EntityType t,
      String actor,
      String action,
      ObjectNode before,
      ObjectNode after,
      String requestId) {
    ObjectNode a =
        json.createObjectNode()
            .put("actor_id", actor)
            .put("action", action)
            .put(
                "entity_type",
                switch (t) {
                  case SITE -> "site";
                  case HOME -> "home";
                  case INQUIRY -> "inquiry";
                  case APPLICATION -> "dealer_application";
                  default -> t.name().toLowerCase(Locale.ROOT);
                })
            .put("entity_id", after.path("id").asText())
            .put("request_id", requestId);
    a.set("before_data", auditFields(t, before));
    a.set("after_data", auditFields(t, after));
    repo.create(EntityType.AUDIT, a);
  }

  private JsonNode auditFields(EntityType t, ObjectNode d) {
    if (d == null) return json.createObjectNode();
    if (t != EntityType.INQUIRY && t != EntityType.APPLICATION) return d.deepCopy();
    ObjectNode out = json.createObjectNode();
    for (String k : List.of("status", "outcome", "internal_note"))
      if (d.has(k)) out.set(k, d.get(k));
    return out;
  }
}
