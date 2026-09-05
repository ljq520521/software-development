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
          if (c.repository().count(EntityType.SITE) > 0) return;
          String now = Instant.now().toString();
          List<String> files =
              List.of(
                  "coordination-rope.jpg",
                  "bowling.jpg",
                  "outdoor-ball.jpg",
                  "hero-outdoor.jpg",
                  "family-play.jpg");
          List<String> mids = new ArrayList<>();
          for (String file : files) {
            try {
              Path path = media.directory.resolve("sample-" + file);
              if (!Files.exists(path))
                Files.copy(new ClassPathResource("static/assets/" + file).getInputStream(), path);
              var img = ImageIO.read(path.toFile());
              ObjectNode m =
                  c.repository()
                      .create(
                          EntityType.MEDIA,
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
                              file));
              mids.add(m.path("id").asText());
            } catch (Exception e) {
              throw new IllegalStateException("Unable to initialize sample photos", e);
            }
          }
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
          String[] products = {
            "Mini Bowling Play Set",
            "Balance & Move Kit",
            "Play Anywhere Ball Set",
            "Family Bowling Challenge",
            "Jump & Discover Kit",
            "Outdoor Adventure Set"
          };
          String[] productSlugs = {
            "mini-bowling-play-set",
            "balance-move-kit",
            "play-anywhere-ball-set",
            "family-bowling-challenge",
            "jump-discover-kit",
            "outdoor-adventure-set"
          };
          List<String> pids = new ArrayList<>();
          for (int i = 0; i < products.length; i++) {
            int category = i % 3;
            String photo = mids.get(category == 0 ? 1 : category == 1 ? 0 : 2);
            ObjectNode p =
                node(
                    "name",
                    products[i],
                    "slug",
                    productSlugs[i],
                    "sku",
                    "DEMO-" + (100 + i),
                    "category_id",
                    cats.get(category),
                    "price_cents",
                    9900 + i * 2000,
                    "currency",
                    "CNY",
                    "short_description",
                    category == 0
                        ? "A little aim. A lot of shared fun."
                        : category == 1
                            ? "Find your rhythm, one joyful move at a time."
                            : "Take play outside and make room for discovery.",
                    "description_markdown",
                    "## A new way to play together\n"
                        + "Bring a little movement to everyday moments. Explore easy activities,"
                        + " invent your own challenges, and enjoy time together.\n\n"
                        + "## Sample catalog information\n"
                        + "This is a demonstration record. Activity photography is illustrative and"
                        + " does not show an actual WEMOVE product. Final product specifications"
                        + " and safety instructions must be supplied before commercial use.",
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
            p.set("environments", c.json().valueToTree(List.of("indoor", "outdoor")));
            p.set(
                "features",
                c.json()
                    .valueToTree(
                        List.of(
                            "Discover a new activity together",
                            "Flexible games for shared play",
                            "Simple ideas for everyday movement")));
            p.set(
                "specifications",
                c.json()
                    .valueToTree(
                        List.of(
                            Map.of("name", "Catalog type", "value", "Demonstration sample"),
                            Map.of("name", "Suggested play", "value", "With adult supervision"),
                            Map.of(
                                "name",
                                "Product details",
                                "value",
                                "Contact us to confirm specifications"))));
            p.set(
                "images",
                c.json()
                    .valueToTree(
                        List.of(
                            Map.of(
                                "media_id",
                                photo,
                                "alt",
                                "Illustrative activity photo for " + products[i]))));
            p.set(
                "seo",
                seo(
                    products[i],
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
          for (String[] p : pages) createContent("page", p[0], p[1], p[2], p[3], null, true, now);
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
              mids.get(0),
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
              mids.get(3),
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
                  "Children enjoying an outdoor skipping activity"));
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

  private void createContent(
      String type,
      String slug,
      String title,
      String excerpt,
      String body,
      String mediaId,
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
      cover.add(node("media_id", mediaId, "alt", "Illustrative family activity photo"));
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
