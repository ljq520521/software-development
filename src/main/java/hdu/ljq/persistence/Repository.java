package hdu.ljq.persistence;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import hdu.ljq.common.ApiException;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class Repository {
  public final RecordMapper mapper;
  private final ObjectMapper json;

  public Repository(RecordMapper mapper, ObjectMapper json) {
    this.mapper = mapper;
    this.json = json;
  }

  public record Result(List<ObjectNode> items, int page, int size, long total) {}

  public ObjectNode find(EntityType t, String id) {
    Map<String, Object> r = mapper.find(t.table, id(id));
    if (r == null) throw ApiException.missing();
    return decode(t, r);
  }

  public ObjectNode by(EntityType t, String key, Object value) {
    if (!t.fields.containsKey(key)) throw new IllegalArgumentException();
    List<Map<String, Object>> rs =
        mapper.select(t.table, "`" + key + "`=#{p.value}", Map.of("value", value), "id DESC", 1, 0);
    return rs.isEmpty() ? null : decode(t, rs.getFirst());
  }

  public List<ObjectNode> all(EntityType t) {
    return mapper.select(t.table, "1=1", Map.of(), "id ASC", 10000, 0).stream()
        .map(r -> decode(t, r))
        .toList();
  }

  public long count(EntityType t) {
    return mapper.count(t.table, "1=1", Map.of());
  }

  public ObjectNode create(EntityType t, ObjectNode data) {
    Map<String, Object> row = encode(t, data);
    Timestamp now = Timestamp.from(Instant.now());
    row.put("created_at", now);
    row.put("updated_at", now);
    row.put("version", 1);
    mapper.insert(t.table, row);
    return find(t, row.get("id").toString());
  }

  public ObjectNode update(EntityType t, String id, ObjectNode values, int version) {
    Map<String, Object> row = encode(t, values);
    if (mapper.update(t.table, row, id(id), version) != 1) {
      find(t, id);
      throw ApiException.conflict(
          "VERSION_CONFLICT", "This record changed. Refresh before saving.");
    }
    return find(t, id);
  }

  public Result list(EntityType t, Map<String, String> query, boolean publicOnly) {
    Set<String> allowed = new HashSet<>(List.of("page", "page_size"));
    if (t != EntityType.CATEGORY && t != EntityType.AUDIT) allowed.add("q");
    switch (t) {
      case PRODUCT ->
          allowed.addAll(
              publicOnly
                  ? List.of("category", "age", "environment", "sort")
                  : List.of("status", "category_id"));
      case CATEGORY -> {
        if (!publicOnly) allowed.add("enabled");
      }
      case CONTENT -> {
        allowed.add("type");
        if (!publicOnly) allowed.add("status");
      }
      case FAQ -> allowed.add(publicOnly ? "group_name" : "enabled");
      case INQUIRY -> allowed.addAll(List.of("status", "type"));
      case APPLICATION -> allowed.addAll(List.of("status", "country", "business_type"));
      case AUDIT -> allowed.addAll(List.of("entity_type", "entity_id"));
      default -> {}
    }
    for (String k : query.keySet())
      if (!allowed.contains(k)) throw ApiException.invalid(k, "Unknown query parameter.");
    int page = number(query, "page", 1, 1, Integer.MAX_VALUE),
        size = number(query, "page_size", 12, 1, 50);
    List<String> w = new ArrayList<>(List.of("1=1"));
    Map<String, Object> p = new HashMap<>();
    if (publicOnly) {
      if (t == EntityType.PRODUCT)
        w.add("status='active' AND category_id IN (SELECT id FROM category WHERE enabled=1)");
      if (t == EntityType.CONTENT) w.add("status='published'");
      if (t == EntityType.FAQ || t == EntityType.CATEGORY) w.add("enabled=1");
    }
    String search = query.getOrDefault("q", "").trim();
    if (search.length() > 100) throw ApiException.invalid("q", "Maximum 100 characters.");
    if (!search.isEmpty()) {
      String[] cols =
          switch (t) {
            case PRODUCT -> new String[] {"name", "sku", "short_description"};
            case CONTENT -> new String[] {"title", "excerpt"};
            case FAQ -> new String[] {"question", "answer"};
            case INQUIRY -> new String[] {"reference", "name", "email", "subject"};
            case APPLICATION -> new String[] {"reference", "company_name", "contact_name", "email"};
            case MEDIA -> new String[] {"original_name"};
            default -> new String[] {"id"};
          };
      p.put("q", "%" + search.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%");
      w.add(
          "("
              + String.join(
                  " OR ",
                  Arrays.stream(cols).map(c -> "`" + c + "` LIKE #{p.q} ESCAPE '!' ").toList())
              + ")");
    }
    for (String key :
        List.of(
            "status",
            "type",
            "country",
            "business_type",
            "group_name",
            "entity_type",
            "entity_id",
            "category_id"))
      if (query.containsKey(key)) {
        String value = query.get(key);
        if (key.endsWith("_id")) id(value);
        validateEnum(t, key, value);
        p.put(key, value);
        w.add("`" + key + "`=#{p." + key + "}");
      }
    if (t == EntityType.CONTENT && publicOnly && !query.containsKey("type"))
      w.add("type='article'");
    if (query.containsKey("enabled")) {
      if (!List.of("true", "false").contains(query.get("enabled")))
        throw ApiException.invalid("enabled", "Expected true or false.");
      p.put("enabled", Boolean.valueOf(query.get("enabled")));
      w.add("enabled=#{p.enabled}");
    }
    if (query.containsKey("category")) {
      String v = query.get("category");
      if (!v.matches("[a-z0-9]+(?:-[a-z0-9]+)*"))
        throw ApiException.invalid("category", "Invalid slug.");
      p.put("category", v);
      w.add("category_id IN (SELECT id FROM category WHERE slug=#{p.category})");
    }
    if (query.containsKey("category_id")) find(EntityType.CATEGORY, query.get("category_id"));
    if (query.containsKey("age")) {
      p.put("age", number(query, "age", 0, 0, 99));
      w.add("age_min<=#{p.age} AND age_max>=#{p.age}");
    }
    if (query.containsKey("environment")) {
      String v = query.get("environment");
      if (!List.of("indoor", "outdoor").contains(v))
        throw ApiException.invalid("environment", "Invalid environment.");
      p.put("environment", v);
      w.add("JSON_CONTAINS(environments,JSON_QUOTE(#{p.environment}))");
    }
    if (query.containsKey("entity_id") && !query.containsKey("entity_type"))
      throw ApiException.invalid("entity_type", "Required with entity_id.");
    String order =
        (t == EntityType.CATEGORY || t == EntityType.FAQ)
            ? "sort_order ASC,id DESC"
            : t == EntityType.CONTENT && publicOnly
                ? "first_published_at DESC,id DESC"
                : "created_at DESC,id DESC";
    if (t == EntityType.PRODUCT && publicOnly) {
      order =
          switch (query.getOrDefault("sort", "featured")) {
            case "featured" -> "featured DESC,created_at DESC,id DESC";
            case "newest" -> "created_at DESC,id DESC";
            case "name_asc" -> "name ASC,id DESC";
            default -> throw ApiException.invalid("sort", "Unsupported sort.");
          };
    }
    String where = String.join(" AND ", w);
    long total = mapper.count(t.table, where, p);
    return new Result(
        mapper.select(t.table, where, p, order, size, (long) (page - 1) * size).stream()
            .map(r -> decode(t, r))
            .toList(),
        page,
        size,
        total);
  }

  private void validateEnum(EntityType t, String key, String v) {
    String choices =
        switch (key) {
          case "status" ->
              switch (t) {
                case PRODUCT -> "draft active hidden archived";
                case CONTENT -> "draft published archived";
                case INQUIRY -> "new in_progress resolved closed";
                case APPLICATION -> "submitted under_review closed";
                default -> "";
              };
          case "type" ->
              t == EntityType.CONTENT
                  ? "page article"
                  : "general product_question dealer_inquiry media_business";
          case "business_type" -> "retailer wholesaler distributor institution other";
          case "entity_type" ->
              "product category content faq home site inquiry dealer_application dealer_account media order payment";
          default -> "";
        };
    if (!choices.isEmpty() && !List.of(choices.split(" ")).contains(v))
      throw ApiException.invalid(key, "Unsupported value.");
    if (key.equals("country") && !List.of(Locale.getISOCountries()).contains(v))
      throw ApiException.invalid(key, "Invalid country code.");
  }

  public static int number(Map<String, String> q, String key, int def, int min, int max) {
    try {
      String s = q.getOrDefault(key, "" + def);
      if (!s.matches("[0-9]+")) throw new Exception();
      int n = Integer.parseInt(s);
      if (n < min || n > max) throw new Exception();
      return n;
    } catch (Exception e) {
      throw ApiException.invalid(key, "Invalid integer or range.");
    }
  }

  public static long id(String value) {
    try {
      if (!value.matches("[1-9][0-9]{0,18}")) throw new Exception();
      return Long.parseLong(value);
    } catch (Exception e) {
      throw ApiException.invalid("id", "Invalid identifier.");
    }
  }

  private Map<String, Object> encode(EntityType t, ObjectNode d) {
    Map<String, Object> row = new LinkedHashMap<>();
    t.fields.forEach(
        (k, type) -> {
          if (d.has(k)) {
            JsonNode v = d.get(k);
            Object out = null;
            if (!v.isNull())
              out =
                  switch (type) {
                    case "JSON" -> v.toString();
                    case "NUMBER" -> v.asLong();
                    case "BOOLEAN" -> v.asBoolean();
                    case "ID" -> id(v.asText());
                    case "DATE" -> Timestamp.from(Instant.parse(v.asText()));
                    default -> v.asText();
                  };
            row.put(k, out);
          }
        });
    return row;
  }

  private ObjectNode decode(EntityType t, Map<String, Object> row) {
    ObjectNode d = json.createObjectNode();
    row.forEach(
        (k, v) -> {
          if (v == null) {
            d.putNull(k);
            return;
          }
          String type =
              k.equals("id")
                  ? "ID"
                  : k.equals("created_at") || k.equals("updated_at")
                      ? "DATE"
                      : t.fields.getOrDefault(k, "NUMBER");
          try {
            switch (type) {
              case "JSON" ->
                  d.set(
                      k,
                      json.readTree(
                          v instanceof byte[] bytes
                              ? new String(bytes, java.nio.charset.StandardCharsets.UTF_8)
                              : v.toString()));
              case "DATE" ->
                  d.put(
                      k,
                      (v instanceof Timestamp ts
                              ? ts.toInstant()
                              : ((LocalDateTime) v).toInstant(ZoneOffset.UTC))
                          .toString());
              case "ID", "STRING" -> d.put(k, v.toString());
              case "BOOLEAN" ->
                  d.put(
                      k,
                      v instanceof Boolean bv ? bv.booleanValue() : ((Number) v).intValue() != 0);
              default -> d.put(k, ((Number) v).longValue());
            }
          } catch (Exception e) {
            throw new IllegalStateException("Unable to decode " + t + "." + k, e);
          }
        });
    t.fields
        .keySet()
        .forEach(
            k -> {
              if (!d.has(k)) d.putNull(k);
            });
    return d;
  }
}
