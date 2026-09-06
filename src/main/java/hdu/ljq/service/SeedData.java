package hdu.ljq.service;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import hdu.ljq.persistence.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Order(10)
public class SeedData implements ApplicationRunner {
  private static final List<String> PRODUCT_NAMES =
      List.of(
          "Mini Bowling Play Set",
          "Balance & Move Kit",
          "Play Anywhere Ball Set",
          "Family Bowling Challenge",
          "Jump & Discover Kit",
          "Outdoor Adventure Set");
  private static final List<String> PRODUCT_SLUGS =
      List.of(
          "mini-bowling-play-set",
          "balance-move-kit",
          "play-anywhere-ball-set",
          "family-bowling-challenge",
          "jump-discover-kit",
          "outdoor-adventure-set");
  private static final List<String> PRODUCT_PHOTOS =
      List.of(
          "product-mini-bowling.jpg",
          "product-balance-move.jpg",
          "product-play-ball.jpg",
          "product-family-bowling.jpg",
          "product-jump-discover.jpg",
          "product-outdoor-adventure.jpg");
  private static final List<String> PRODUCT_ALTS =
      List.of(
          "Six wooden bowling pins with colored bands and a small blue ball",
          "Five stepping stones and a wooden balance beam in a bright playroom",
          "Three soft fabric play balls, marker cones and a canvas carry bag",
          "A family playing with a ten-pin wooden bowling challenge set",
          "A child using floor spots, soft hurdles and a cotton jump rope",
          "Children playing with a portable ball, beanbag, cone and ring-toss set outdoors");
  private static final List<String> SHORT_DESCRIPTIONS =
      List.of(
          "Six smooth wooden pins, one little blue ball and a very satisfying strike.",
          "Step, balance and build a new path with five stones and a wooden bridge.",
          "Three soft balls, four marker cones and one carry bag for play wherever you go.",
          "Ten wooden pins, two balls and score tiles turn the living room into a family lane.",
          "A jump rope, six activity spots and four soft hurdles for a course that changes every day.",
          "Balls, beanbags, cones and ring toss come together in one portable backyard challenge.");
  private static final List<String> COMPONENTS =
      List.of(
          "6 beechwood pins · 1 wooden ball",
          "5 non-slip stepping stones · 1 wooden balance beam",
          "3 soft fabric balls · 4 marker cones · 1 canvas bag",
          "10 beechwood pins · 2 wooden balls · score tiles",
          "1 cotton jump rope · 6 floor spots · 4 soft hurdles",
          "2 soft balls · 6 beanbags · 4 cones · 3 ring-toss pegs · carry tote");
  private final CatalogService c;
  private final MediaService media;
  private final PasswordEncoder passwords;
  private final TransactionTemplate tx;
  private final String email, password;

  public SeedData(
      CatalogService c,
      MediaService media,
      PasswordEncoder passwords,
      PlatformTransactionManager tm,
      @Value("${app.admin-email}") String email,
      @Value("${app.admin-password}") String password) {
    this.c = c;
    this.media = media;
    this.passwords = passwords;
    this.tx = new TransactionTemplate(tm);
    this.email = email;
    this.password = password;
  }

  public void run(ApplicationArguments args) {
    tx.executeWithoutResult(
        status -> {
          c.repository().mapper.lock("seed");
          if (c.repository().count(EntityType.ADMIN) == 0) {
            if (password.length() < 12)
              throw new IllegalStateException(
                  "Set ADMIN_PASSWORD to a unique password of at least 12 characters (see"
                      + " .env.example).");
            c.repository()
                .create(
                    EntityType.ADMIN,
                    node(
                        "email",
                        email.toLowerCase(Locale.ROOT),
                        "password_hash",
                        passwords.encode(password),
                        "display_name",
                        "网站管理员",
                        "status",
                        "active"));
          }
          List<String> files = new ArrayList<>();
          files.add("wemove-hero-play.jpg");
          files.addAll(PRODUCT_PHOTOS);
          List<String> mids = ensureMedia(files);
          if (c.repository().count(EntityType.SITE) > 0) {
            refreshExistingDemoCatalog(mids);
            return;
          }
          String now = Instant.now().toString();
          List<String> cats = new ArrayList<>();
          String[] names = {"Bowling & aim", "Balance & coordination", "Outdoor games"},
              slugs = {"bowling", "balance", "outdoor"};
          for (int i = 0; i < names.length; i++)
            cats.add(
                c.repository()
                    .create(
                        EntityType.CATEGORY,
                        node(
                            "name",
                            names[i],
                            "slug",
                            slugs[i],
                            "description",
                            "Simple ways to discover the joy of movement.",
                            "enabled",
                            true,
                            "sort_order",
                            i))
                    .path("id")
                    .asText());
          List<String> pids = new ArrayList<>();
          for (int i = 0; i < PRODUCT_NAMES.size(); i++) {
            int category = i % 3;
            String photo = mids.get(i + 1);
            ObjectNode p =
                node(
                    "name",
                    PRODUCT_NAMES.get(i),
                    "slug",
                    PRODUCT_SLUGS.get(i),
                    "sku",
                    "DEMO-" + (100 + i),
                    "category_id",
                    cats.get(category),
                    "price_cents",
                    9900 + i * 2000,
                    "currency",
                    "CNY",
                    "age_min",
                    3 + (i % 2),
                    "age_max",
                    10,
                    "featured",
                    i < 4,
                    "status",
                    "active",
                    "first_published_at",
                    now);
            applyDemoPresentation(p, i, photo);
            p.set(
                "seo",
                seo(
                    PRODUCT_NAMES.get(i),
                    "Explore this sample activity collection and contact WEMOVE SPORTS to learn"
                        + " more."));
            pids.add(c.repository().create(EntityType.PRODUCT, p).path("id").asText());
          }
          String[][] pages = {
            {
              "about",
              "Made for the joy of moving",
              "We believe the best discoveries begin with play.",
              "## Play is where possibility begins\n"
                  + "A small challenge. A shared laugh. One more try. WEMOVE SPORTS brings movement"
                  + " into the moments that bring us together.\n\n"
                  + "## Our story, in motion\n"
                  + "Explore our collections, find an activity for your family, and get in touch to"
                  + " learn more. This website contains demonstration catalog content awaiting"
                  + " brand review."
            },
            {
              "quality-safety",
              "Thoughtful play starts here",
              "Clear information for confident choices.",
              "## Before you begin\n"
                  + "Always follow the specific product instructions, age recommendations, and"
                  + " adult-supervision requirements supplied with your product.\n\n"
                  + "## Ask us about a product\n"
                  + "Our sample catalog does not make certification claims. Contact us for"
                  + " confirmed specifications, materials and current safety documents."
            },
            {
              "support",
              "How can we help?",
              "A little guidance for your next adventure.",
              "## Find your answer\n"
                  + "Browse our frequently asked questions or contact us about a specific"
                  + " product.\n\n"
                  + "## Product information\n"
                  + "Choose a product from the catalog and use the contact button to include it"
                  + " with your question."
            },
            {
              "privacy",
              "Privacy information",
              "About the information submitted through this demonstration website.",
              "## Information you choose to share\n"
                  + "Contact and partnership forms collect adult contact details and business"
                  + " messages. Do not submit children's personal information.\n\n"
                  + "## How this site works\n"
                  + "Necessary session cookies support sign-in and secure forms. No advertising or"
                  + " analytics cookies are enabled. Submitted messages are stored for"
                  + " administrators to review.\n\n"
                  + "## Contact and review\n"
                  + "Use the published contact address for access or deletion requests. This is"
                  + " demonstration policy text; the operator must replace it with a reviewed"
                  + " policy before public operation."
            },
            {
              "terms",
              "Website terms",
              "Information about using this demonstration catalog.",
              "## Catalog information\n"
                  + "Products and specifications on this installation are demonstration records,"
                  + " not a commercial offer. Activity photos are illustrative.\n\n"
                  + "## No online purchase\n"
                  + "The website accepts product questions and partnership inquiries. It does not"
                  + " accept orders or payments.\n\n"
                  + "## Contact\n"
                  + "Please ask us to confirm product availability and specifications. Replace"
                  + " these demonstration terms with reviewed business terms before public"
                  + " operation."
            }
          };
          for (String[] p : pages)
            createContent("page", p[0], p[1], p[2], p[3], null, null, true, now);
          createContent(
              "article",
              "five-ways-to-play-together",
              "Five little ways to move together",
              "Make everyday moments a little more playful.",
              "## Start small\n"
                  + "Set aside ten minutes and choose a comfortable space. Let everyone help decide"
                  + " what to play.\n\n"
                  + "## Try a friendly challenge\n"
                  + "Count successful throws, build a simple movement course, or make up a"
                  + " balancing game. Adjust the challenge to suit the people taking part.\n\n"
                  + "## Keep it playful\n"
                  + "Take turns, celebrate effort and pause when someone needs a break. Always use"
                  + " age-appropriate equipment and adult supervision.",
              mids.get(5),
              "A child creating a movement course with hurdles, floor spots and a jump rope",
              false,
              now);
          createContent(
              "article",
              "take-play-outside",
              "A little fresh air. A whole lot of play.",
              "Ideas for a shared afternoon outdoors.",
              "## Make space for discovery\n"
                  + "Choose a safe, open area and an activity everyone can enjoy. A familiar ball"
                  + " game can become a new adventure with a few simple rules.\n\n"
                  + "## Bring the family together\n"
                  + "Take turns inventing a challenge. Keep the focus on enjoying time together"
                  + " rather than winning.\n\n"
                  + "## Pack up with care\n"
                  + "Follow the care instructions for your equipment and check the play area before"
                  + " you leave.",
              mids.get(6),
              "Children exploring an outdoor challenge with balls, beanbags and ring toss",
              false,
              now);
          String[][] faqs = {
            {
              "How do I choose a product?",
              "Browse by activity and suggested age, then contact us to confirm the product's"
                  + " specifications and instructions.",
              "Products"
            },
            {
              "Can I buy directly from this website?",
              "This catalog accepts inquiries. Use the contact form to discuss a product and"
                  + " available purchasing channels.",
              "Ordering"
            },
            {
              "How can my business become a partner?",
              "Complete the partnership application. Your submission receives a reference number"
                  + " and is reviewed by our team.",
              "Partnerships"
            },
            {
              "Where can I find safety information?",
              "Follow the instructions supplied with your specific product. Contact us to request"
                  + " confirmed safety and care information.",
              "Support"
            },
            {
              "Will I receive a confirmation email?",
              "This installation shows an on-screen reference after a successful submission. Please"
                  + " save it for your records.",
              "Contact"
            }
          };
          for (int i = 0; i < faqs.length; i++)
            c.repository()
                .create(
                    EntityType.FAQ,
                    node(
                        "question",
                        faqs[i][0],
                        "answer",
                        faqs[i][1],
                        "group_name",
                        faqs[i][2],
                        "enabled",
                        true,
                        "sort_order",
                        i));
          c.repository()
              .create(
                  EntityType.SITE,
                  node(
                      "brand_name",
                      "WEMOVE SPORTS",
                      "tagline",
                      "Small moves. Big discoveries.",
                      "contact_email",
                      "hello@example.com",
                      "contact_phone",
                      "",
                      "privacy_version",
                      "2026-09-04"));
          ObjectNode home = node();
          home.set(
              "section_order",
              c.json()
                  .valueToTree(
                      List.of(
                          "hero", "categories", "featured_products", "articles", "dealer_cta")));
          home.set("enabled_sections", home.get("section_order").deepCopy());
          home.set("featured_product_ids", c.json().valueToTree(pids.subList(0, 4)));
          ObjectNode hero =
              node(
                  "title",
                  "Small moves.\nBig discoveries.",
                  "subtitle",
                  "More movement. More imagination. More moments together. Discover a world of"
                      + " active play for the whole family.");
          hero.set(
              "image",
              node(
                  "media_id",
                  mids.get(0),
                  "alt",
                  "A family playing together with the WEMOVE active-play collection"));
          hero.set("primary_cta", node("label", "Find your next adventure", "href", "/products"));
          home.set("hero", hero);
          home.set(
              "dealer_cta",
              node(
                  "title",
                  "Let's get more people moving.",
                  "description",
                  "Bring the joy of active play to your community. Start a conversation about"
                      + " becoming a WEMOVE partner.",
                  "button_label",
                  "Become a partner"));
          c.repository().create(EntityType.HOME, home);
        });
  }

  private List<String> ensureMedia(List<String> files) {
    List<String> ids = new ArrayList<>();
    for (String file : files) {
      try {
        Path path = media.directory.resolve("sample-" + file);
        Files.copy(
            new ClassPathResource("static/assets/" + file).getInputStream(),
            path,
            StandardCopyOption.REPLACE_EXISTING);
        var img = ImageIO.read(path.toFile());
        ObjectNode values =
            node(
                "url",
                "/media/sample-" + file,
                "mime_type",
                "image/jpeg",
                "byte_size",
                Files.size(path),
                "width",
                img.getWidth(),
                "height",
                img.getHeight(),
                "original_name",
                file);
        ObjectNode existing = c.repository().by(EntityType.MEDIA, "original_name", file);
        ObjectNode saved =
            existing == null
                ? c.repository().create(EntityType.MEDIA, values)
                : c.repository()
                    .update(
                        EntityType.MEDIA,
                        existing.path("id").asText(),
                        values,
                        existing.path("version").asInt());
        ids.add(saved.path("id").asText());
      } catch (Exception e) {
        throw new IllegalStateException("Unable to initialize sample photos", e);
      }
    }
    return ids;
  }

  private void refreshExistingDemoCatalog(List<String> mediaIds) {
    for (int i = 0; i < PRODUCT_SLUGS.size(); i++) {
      ObjectNode existing = c.repository().by(EntityType.PRODUCT, "slug", PRODUCT_SLUGS.get(i));
      if (existing == null || !existing.path("sku").asText().startsWith("DEMO-")) continue;
      ObjectNode values = node();
      applyDemoPresentation(values, i, mediaIds.get(i + 1));
      c.repository()
          .update(
              EntityType.PRODUCT,
              existing.path("id").asText(),
              values,
              existing.path("version").asInt());
    }

    List<ObjectNode> homes = c.repository().all(EntityType.HOME);
    if (!homes.isEmpty()) {
      ObjectNode existing = homes.getFirst();
      ObjectNode hero = existing.path("hero").deepCopy();
      hero.set(
          "image",
          node(
              "media_id",
              mediaIds.getFirst(),
              "alt",
              "A family playing together with the WEMOVE active-play collection"));
      ObjectNode values = node();
      values.set("hero", hero);
      c.repository()
          .update(
              EntityType.HOME,
              existing.path("id").asText(),
              values,
              existing.path("version").asInt());
    }

    refreshArticleCover(
        "five-ways-to-play-together",
        mediaIds.get(5),
        "A child creating a movement course with hurdles, floor spots and a jump rope");
    refreshArticleCover(
        "take-play-outside",
        mediaIds.get(6),
        "Children exploring an outdoor challenge with balls, beanbags and ring toss");
  }

  private void refreshArticleCover(String slug, String mediaId, String alt) {
    ObjectNode existing = c.repository().by(EntityType.CONTENT, "slug", slug);
    if (existing == null) return;
    ObjectNode values = node();
    values.set("cover", c.json().valueToTree(List.of(Map.of("media_id", mediaId, "alt", alt))));
    c.repository()
        .update(
            EntityType.CONTENT,
            existing.path("id").asText(),
            values,
            existing.path("version").asInt());
  }

  private void applyDemoPresentation(ObjectNode product, int index, String mediaId) {
    String playSetting =
        switch (index) {
          case 1, 3 -> "Indoor play";
          case 5 -> "Outdoor play";
          default -> "Indoor or outdoor play";
        };
    product.put("short_description", SHORT_DESCRIPTIONS.get(index));
    product.put(
        "description_markdown",
        "## Everything in the picture\n"
            + "This sample set includes "
            + COMPONENTS.get(index)
            + ". Every item named here is represented in the product image.\n\n"
            + "## Make the challenge your own\n"
            + "Start with one simple game, then rearrange the pieces, change the distance or invite"
            + " another player. The open-ended format makes it easy to create a fresh activity for"
            + " different spaces and confidence levels.\n\n"
            + "## Sample catalog information\n"
            + "This is a product concept for demonstration. Confirm final materials, dimensions and"
            + " safety instructions before commercial use.");
    product.set(
        "environments",
        c.json()
            .valueToTree(
                index == 1 || index == 3
                    ? List.of("indoor")
                    : index == 5 ? List.of("outdoor") : List.of("indoor", "outdoor")));
    product.set(
        "features",
        c.json()
            .valueToTree(
                List.of(
                    "The complete set is shown in the product image",
                    "Rearrange the pieces to create new challenges",
                    "Designed for movement, imagination and shared play")));
    product.set(
        "specifications",
        c.json()
            .valueToTree(
                List.of(
                    Map.of("name", "Set includes", "value", COMPONENTS.get(index)),
                    Map.of("name", "Suggested play", "value", playSetting),
                    Map.of("name", "Catalog status", "value", "Demonstration product concept"))));
    product.set(
        "images",
        c.json()
            .valueToTree(
                List.of(Map.of("media_id", mediaId, "alt", PRODUCT_ALTS.get(index)))));
  }

  private void createContent(
      String type,
      String slug,
      String title,
      String excerpt,
      String body,
      String mediaId,
      String mediaAlt,
      boolean system,
      String now) {
    ObjectNode d =
        node(
            "type",
            type,
            "slug",
            slug,
            "title",
            title,
            "excerpt",
            excerpt,
            "body_markdown",
            body,
            "status",
            "published",
            "is_system",
            system,
            "first_published_at",
            now);
    ArrayNode cover = c.json().createArrayNode();
    if (mediaId != null)
      cover.add(node("media_id", mediaId, "alt", mediaAlt));
    d.set("cover", cover);
    d.set("seo", seo(title, excerpt));
    c.repository().create(EntityType.CONTENT, d);
  }

  private ObjectNode seo(String title, String desc) {
    return node("title", title + " | WEMOVE SPORTS", "description", desc);
  }

  private ObjectNode node(Object... pairs) {
    ObjectNode n = c.json().createObjectNode();
    for (int i = 0; i < pairs.length; i += 2)
      n.set(pairs[i].toString(), c.json().valueToTree(pairs[i + 1]));
    return n;
  }
}
