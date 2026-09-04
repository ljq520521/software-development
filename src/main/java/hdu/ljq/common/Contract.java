package hdu.ljq.common;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class Contract {
  public final JsonNode schemas;
  private final ObjectMapper json;

  public Contract(ObjectMapper json) throws Exception {
    this.json = json;
    this.schemas =
        json.readTree(new ClassPathResource("api/openapi.json").getInputStream())
            .path("components")
            .path("schemas");
  }

  public ObjectNode input(String name, JsonNode input) {
    Map<String, List<String>> errors = new LinkedHashMap<>();
    validate(schemas.path(name), input, "", errors);
    if (!errors.isEmpty())
      throw new ApiException(422, "VALIDATION_ERROR", "Please check the submitted fields.", errors);
    return (ObjectNode) input.deepCopy();
  }

  private void error(Map<String, List<String>> out, String field, String message) {
    out.put(field.isEmpty() ? "body" : field, List.of(message));
  }

  private void validate(
      JsonNode schema, JsonNode value, String path, Map<String, List<String>> errors) {
    if (schema.has("$ref")) {
      validate(
          schemas.path(schema.path("$ref").asText().replace("#/components/schemas/", "")),
          value,
          path,
          errors);
      return;
    }
    if (value == null || value.isNull()) {
      if (!schema.path("nullable").asBoolean()) error(errors, path, "A value is required.");
      return;
    }
    if (schema.has("enum")) {
      boolean match = false;
      for (JsonNode e : schema.path("enum")) if (e.equals(value)) match = true;
      if (!match) error(errors, path, "Unsupported value.");
    }
    switch (schema.path("type").asText()) {
      case "object" -> {
        if (!value.isObject()) {
          error(errors, path, "Expected an object.");
          return;
        }
        for (JsonNode r : schema.path("required"))
          if (!value.has(r.asText()))
            error(errors, child(path, r.asText()), "This field is required.");
        value
            .fields()
            .forEachRemaining(
                e -> {
                  JsonNode p = schema.path("properties").get(e.getKey());
                  if (p == null) {
                    if (schema.path("additionalProperties").isBoolean()
                        && !schema.path("additionalProperties").asBoolean())
                      error(errors, child(path, e.getKey()), "Unknown field.");
                  } else validate(p, e.getValue(), child(path, e.getKey()), errors);
                });
        if (value.size() < schema.path("minProperties").asInt())
          error(errors, path, "Provide at least one field to update.");
      }
      case "array" -> {
        if (!value.isArray()) {
          error(errors, path, "Expected an array.");
          return;
        }
        if (value.size() < schema.path("minItems").asInt()
            || value.size() > schema.path("maxItems").asInt(Integer.MAX_VALUE))
          error(errors, path, "Invalid number of items.");
        Set<JsonNode> seen = new HashSet<>();
        int i = 0;
        for (JsonNode v : value) {
          validate(schema.path("items"), v, child(path, "" + i++), errors);
          if (schema.path("uniqueItems").asBoolean() && !seen.add(v))
            error(errors, path, "Duplicate items are not allowed.");
        }
      }
      case "string" -> {
        if (!value.isTextual()) {
          error(errors, path, "Expected text.");
          return;
        }
        String v = value.asText();
        if (v.length() < schema.path("minLength").asInt()
            || v.length() > schema.path("maxLength").asInt(Integer.MAX_VALUE))
          error(errors, path, "Text length is outside the allowed range.");
        if (schema.has("pattern")
            && !Pattern.compile(schema.path("pattern").asText()).matcher(v).find())
          error(errors, path, "Invalid format.");
        if (schema.path("format").asText().equals("email")
            && !v.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
          error(errors, path, "Enter a valid email address.");
        if (schema.path("minLength").asInt() > 0 && v.isBlank())
          error(errors, path, "This field must not be blank.");
      }
      case "integer" -> {
        if (!value.isIntegralNumber()
            || value.asLong() < schema.path("minimum").asLong(Long.MIN_VALUE)
            || value.asLong() > schema.path("maximum").asLong(Long.MAX_VALUE))
          error(errors, path, "Enter an integer in the allowed range.");
      }
      case "boolean" -> {
        if (!value.isBoolean()) error(errors, path, "Expected true or false.");
      }
    }
  }

  private String child(String p, String k) {
    return p.isEmpty() ? k : p + "." + k;
  }

  public JsonNode output(String model, JsonNode value) {
    return project(schemas.path(model), value);
  }

  private JsonNode project(JsonNode schema, JsonNode value) {
    if (value == null || value.isNull()) return NullNode.instance;
    if (schema.has("$ref"))
      return project(
          schemas.path(schema.path("$ref").asText().replace("#/components/schemas/", "")), value);
    if (schema.path("type").asText().equals("array")) {
      ArrayNode a = json.createArrayNode();
      for (JsonNode n : value) a.add(project(schema.path("items"), n));
      return a;
    }
    if (schema.path("type").asText().equals("object") && schema.has("properties")) {
      ObjectNode o = json.createObjectNode();
      schema
          .path("properties")
          .fields()
          .forEachRemaining(e -> o.set(e.getKey(), project(e.getValue(), value.get(e.getKey()))));
      return o;
    }
    return value.deepCopy();
  }
}
