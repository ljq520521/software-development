package hdu.ljq;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import hdu.ljq.common.*;
import hdu.ljq.persistence.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;

/** Runs against an isolated real MySQL database. Each test rolls back its business changes. */
@SpringBootTest(
    properties = {
      "spring.datasource.url=${TEST_DB_URL:jdbc:mysql://127.0.0.1:3306/wemove_sports_test?createDatabaseIfNotExist=true&connectionTimeZone=UTC}",
      "app.upload-dir=./target/test-uploads"
    })
@AutoConfigureMockMvc
@Transactional
class BuildingBlockWebApplicationTests {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired Repository repo;
  @Autowired PasswordEncoder encoder;
  @Autowired Contract contract;
  @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
  MockHttpSession session;
  String csrf;
  final String testPassword = UUID.randomUUID().toString();

  @BeforeEach
  void login() throws Exception {
    ObjectNode admin = repo.find(EntityType.ADMIN, "1");
    repo.update(
        EntityType.ADMIN,
        "1",
        json.createObjectNode().put("password_hash", encoder.encode(testPassword)),
        admin.path("version").asInt());
    MvcResult token = mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();
    session = (MockHttpSession) token.getRequest().getSession();
    csrf = read(token).path("data").path("csrf_token").asText();
    MvcResult auth =
        mvc.perform(
                post("/api/v1/auth/login")
                    .session(session)
                    .header("X-CSRF-Token", csrf)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.createObjectNode()
                            .put("email", admin.path("email").asText())
                            .put("password", testPassword)
                            .toString()))
            .andExpect(status().isOk())
            .andReturn();
    csrf = read(auth).path("data").path("csrf_token").asText();
  }

  @Test
  void publicPagesAndAuthenticationBoundaries() throws Exception {
    for (String path :
        List.of(
            "/",
            "/products",
            "/products/mini-bowling-play-set",
            "/play",
            "/play/five-ways-to-play-together",
            "/about",
            "/support",
            "/contact",
            "/dealers/apply",
            "/admin")) mvc.perform(get(path)).andExpect(status().isOk());
    mvc.perform(get("/api/v1/admin/products")).andExpect(status().isUnauthorized());
    mvc.perform(
            patch("/api/v1/admin/site")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":1,\"brand_name\":\"bad\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/products").param("page_size", "51"))
        .andExpect(status().isUnprocessableEntity());
    mvc.perform(get("/api/v1/products").param("sort", "name;DROP TABLE product"))
        .andExpect(status().isUnprocessableEntity());
    mvc.perform(get("/api/v1/products").param("category", "no-such-category"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.total").value(0));
    mvc.perform(get("/products/no-such-product")).andExpect(status().isNotFound());
  }

  @Test
  void productPublishOptimisticLockAndPublicVisibility() throws Exception {
    ObjectNode original = repo.all(EntityType.PRODUCT).getFirst();
    ObjectNode input = (ObjectNode) contract.output("ProductCreate", original);
    input
        .put("slug", "integration-product")
        .put("sku", "INTEGRATION-PRODUCT")
        .put("name", "Integration product");
    JsonNode created = write("POST", "/api/v1/admin/products", input, 201).path("data");
    String id = created.path("id").asText();
    mvc.perform(get("/api/v1/products/integration-product")).andExpect(status().isNotFound());
    write(
        "PATCH",
        "/api/v1/admin/products/" + id,
        json.createObjectNode().put("version", 1).put("status", "active"),
        200);
    mvc.perform(get("/api/v1/products/integration-product")).andExpect(status().isOk());
    JsonNode stale =
        write(
            "PATCH",
            "/api/v1/admin/products/" + id,
            json.createObjectNode().put("version", 1).put("name", "Stale"),
            409);
    assertEquals("VERSION_CONFLICT", stale.path("code").asText());
    write(
        "PATCH",
        "/api/v1/admin/products/" + id,
        json.createObjectNode().put("version", 2).put("slug", "changed-url"),
        409);
    write(
        "PATCH",
        "/api/v1/admin/products/" + id,
        json.createObjectNode().put("version", 2).put("status", "hidden"),
        200);
    mvc.perform(get("/api/v1/products/integration-product")).andExpect(status().isNotFound());
    JsonNode log =
        read(
            mvc.perform(
                    get("/api/v1/admin/audit-logs")
                        .session(session)
                        .param("entity_type", "product")
                        .param("entity_id", id))
                .andExpect(status().isOk())
                .andReturn());
    assertEquals(3, log.path("data").path("total").asInt());
  }

  @Test
  void contactReceiptIdempotencyAndResolution() throws Exception {
    ObjectNode contact = contact();
    String key = UUID.randomUUID().toString();
    long before = repo.count(EntityType.INQUIRY);
    JsonNode first = form("/api/v1/forms/contact", contact, key, 201);
    JsonNode replay = form("/api/v1/forms/contact", contact, key, 201);
    assertEquals(first.path("data"), replay.path("data"));
    assertEquals(before + 1, repo.count(EntityType.INQUIRY));
    contact.put("message", "Changed content for the same key.");
    assertEquals(
        "IDEMPOTENCY_CONFLICT",
        form("/api/v1/forms/contact", contact, key, 409).path("code").asText());
    String id =
        repo.by(EntityType.INQUIRY, "reference", first.path("data").path("reference").asText())
            .path("id")
            .asText();
    write(
        "PATCH",
        "/api/v1/admin/inquiries/" + id,
        json.createObjectNode().put("version", 1).put("status", "in_progress"),
        200);
    write(
        "PATCH",
        "/api/v1/admin/inquiries/" + id,
        json.createObjectNode()
            .put("version", 2)
            .put("status", "resolved")
            .put("internal_note", "已完成跟进"),
        200);
  }

  @Test
  void dealerDedupeAndTerminalState() throws Exception {
    ObjectNode app =
        json.createObjectNode()
            .put("company_name", "Integration Partner")
            .put("contact_name", "Test Contact")
            .put("email", "test@example.com")
            .put("phone", "123456789")
            .put("country", "CN")
            .put("business_type", "retailer")
            .put("message", "We would like to discuss a partnership.")
            .put("privacy_consent", true)
            .put("privacy_version", "2026-09-04");
    JsonNode receipt = form("/api/v1/dealer/applications", app, UUID.randomUUID().toString(), 201);
    assertEquals(
        "APPLICATION_ALREADY_OPEN",
        form("/api/v1/dealer/applications", app, UUID.randomUUID().toString(), 409)
            .path("code")
            .asText());
    String id =
        repo.by(
                EntityType.APPLICATION,
                "reference",
                receipt.path("data").path("reference").asText())
            .path("id")
            .asText();
    write(
        "PATCH",
        "/api/v1/admin/dealer-applications/" + id,
        json.createObjectNode()
            .put("version", 1)
            .put("status", "closed")
            .put("outcome", "follow_up")
            .put("internal_note", "转入商务洽谈"),
        200);
    write(
        "PATCH",
        "/api/v1/admin/dealer-applications/" + id,
        json.createObjectNode().put("version", 2).put("status", "under_review"),
        409);
    assertTrue(repo.find(EntityType.APPLICATION, id).path("open_dedupe_key").isNull());
  }

  @Test
  void strictContractValidationAndPrivateFieldFiltering() {
    ObjectNode input = contact();
    input.put("role", "admin");
    assertThrows(ApiException.class, () -> contract.input("ContactCreate", input));
    ObjectNode product = repo.all(EntityType.PRODUCT).getFirst();
    product.put("internal_secret", "not public");
    assertFalse(contract.output("ProductDetail", product).has("internal_secret"));
  }

  @Test
  void imageUploadRejectsSpoofedFilesAndAcceptsRealImages() throws Exception {
    var image = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_RGB);
    var bytes = new java.io.ByteArrayOutputStream();
    javax.imageio.ImageIO.write(image, "png", bytes);
    var file =
        new org.springframework.mock.web.MockMultipartFile(
            "file", "test-photo.png", "image/png", bytes.toByteArray());
    JsonNode result =
        read(
            mvc.perform(
                    multipart("/api/v1/admin/media")
                        .file(file)
                        .session(session)
                        .header("X-CSRF-Token", csrf))
                .andExpect(status().isCreated())
                .andReturn());
    String url = result.path("data").path("url").asText();
    try {
      mvc.perform(get(url))
          .andExpect(status().isOk())
          .andExpect(content().contentTypeCompatibleWith("image/jpeg"));
      var fake =
          new org.springframework.mock.web.MockMultipartFile(
              "file", "fake.jpg", "image/jpeg", "<script>not an image</script>".getBytes());
      mvc.perform(
              multipart("/api/v1/admin/media")
                  .file(fake)
                  .session(session)
                  .header("X-CSRF-Token", csrf))
          .andExpect(status().isUnprocessableEntity());
    } finally {
      java.nio.file.Files.deleteIfExists(
          java.nio.file.Path.of("target/test-uploads", url.substring("/media/".length())));
    }
  }

  @Test
  void contentPublishingAndHomepageConfiguration() throws Exception {
    ObjectNode article =
        repo.all(EntityType.CONTENT).stream()
            .filter(x -> x.path("type").asText().equals("article"))
            .findFirst()
            .orElseThrow();
    ObjectNode input = (ObjectNode) contract.output("ContentCreate", article);
    input.put("slug", "integration-article").put("title", "Integration article");
    String id = write("POST", "/api/v1/admin/content", input, 201).path("data").path("id").asText();
    write(
        "PATCH",
        "/api/v1/admin/content/" + id,
        json.createObjectNode().put("version", 1).put("status", "published"),
        200);
    mvc.perform(get("/play/integration-article")).andExpect(status().isOk());
    ObjectNode config = (ObjectNode) contract.output("HomeWrite", repo.find(EntityType.HOME, "1"));
    ((ObjectNode) config.path("hero")).put("title", "A changed homepage title");
    write("PUT", "/api/v1/admin/home", config, 200);
    mvc.perform(get("/api/v1/home"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.hero.title").value("A changed homepage title"));
    ObjectNode system = repo.by(EntityType.CONTENT, "slug", "privacy");
    write(
        "PATCH",
        "/api/v1/admin/content/" + system.path("id").asText(),
        json.createObjectNode()
            .put("version", system.path("version").asInt())
            .put("status", "archived"),
        409);
  }

  @Test
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
  void concurrentRetriesCreateExactlyOneSubmission() throws Exception {
    String key = UUID.randomUUID().toString();
    ObjectNode body = contact();
    body.put("email", "concurrent-" + key + "@example.com");
    var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
    var start = new java.util.concurrent.CountDownLatch(1);
    try {
      java.util.concurrent.Callable<JsonNode> task =
          () -> {
            start.await();
            return form("/api/v1/forms/contact", body, key, 201).path("data");
          };
      var one = executor.submit(task);
      var two = executor.submit(task);
      start.countDown();
      JsonNode first = one.get(10, java.util.concurrent.TimeUnit.SECONDS),
          second = two.get(10, java.util.concurrent.TimeUnit.SECONDS);
      assertEquals(first, second);
      assertEquals(
          1,
          jdbc.queryForObject(
              "SELECT COUNT(*) FROM contact_inquiry WHERE email=?",
              Integer.class,
              body.path("email").asText()));
    } finally {
      executor.shutdownNow();
      executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);
      jdbc.update("DELETE FROM contact_inquiry WHERE email=?", body.path("email").asText());
      jdbc.update(
          "DELETE FROM idempotency_record WHERE endpoint='/forms/contact' AND key_value=?", key);
    }
  }

  private ObjectNode contact() {
    return json.createObjectNode()
        .put("name", "Integration Buyer")
        .put("email", "test@example.com")
        .put("country", "CN")
        .put("type", "general")
        .put("subject", "Integration inquiry")
        .put("message", "Please provide more product information.")
        .put("privacy_consent", true)
        .put("privacy_version", "2026-09-04");
  }

  private JsonNode read(MvcResult result) throws Exception {
    return json.readTree(result.getResponse().getContentAsString());
  }

  private JsonNode write(String method, String path, JsonNode body, int expected) throws Exception {
    var request =
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                org.springframework.http.HttpMethod.valueOf(method), path)
            .session(session)
            .header("X-CSRF-Token", csrf)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body.toString());
    return read(mvc.perform(request).andExpect(status().is(expected)).andReturn());
  }

  private JsonNode form(String path, JsonNode body, String key, int expected) throws Exception {
    // Each test uses a distinct client address so rate windows don't leak between tests.
    return read(
        mvc.perform(
                post(path)
                    .session(session)
                    .header("X-CSRF-Token", csrf)
                    .header("Idempotency-Key", key)
                    .with(
                        r -> {
                          r.setRemoteAddr("test-" + testPassword);
                          return r;
                        })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString()))
            .andExpect(status().is(expected))
            .andReturn());
  }
}
