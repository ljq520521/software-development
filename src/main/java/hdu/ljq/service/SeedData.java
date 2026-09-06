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
          "迷你保龄球套装",
          "平衡与移动套装",
          "随处玩乐球类套装",
          "家庭保龄球挑战套装",
          "跳跃探索套装",
          "户外冒险套装");
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
          "彩色木制保龄球瓶与一颗蓝色小球",
          "五个踏脚石与一根木质平衡木,置于明亮的游戏室",
          "三颗布质软球、标志锥和一个帆布收纳袋",
          "一家人正在玩十瓶木质保龄球挑战套装",
          "孩子在使用地面圆点、软栏和棉质跳绳",
          "孩子们在户外玩便携球、豆袋、锥桶与套圈套装");
  private static final List<String> SHORT_DESCRIPTIONS =
      List.of(
          "六根光滑木制球瓶、一颗蓝色小球,带来一次非常满足的全中。",
          "踩着五块石头和木质平衡木,迈步、保持平衡,走出属于你们的新路线。",
          "三颗软球、四个标志锥和一个收纳袋,走到哪里都能玩起来。",
          "十根木质球瓶、两颗球和记分牌,把客厅变成家庭球道。",
          "一条跳绳、六个活动圆点和四个软栏,让赛道每天都能变化。",
          "球、豆袋、锥桶和套圈组合成一套便携的后院挑战。");
  private static final List<String> COMPONENTS =
      List.of(
          "6 根山毛榉球瓶 · 1 颗木球",
          "5 个防滑踏脚石 · 1 根木质平衡木",
          "3 颗布质软球 · 4 个标志锥 · 1 个帆布袋",
          "10 根山毛榉球瓶 · 2 颗木球 · 记分牌",
          "1 条棉质跳绳 · 6 个地面圆点 · 4 个软栏",
          "2 颗软球 · 6 个豆袋 · 4 个锥桶 · 3 个套圈桩 · 收纳提袋");
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
          String[] names = {"保龄球与瞄准", "平衡与协调", "户外游戏"},
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
                            "用简单的方式发现运动的乐趣。",
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
              "为运动的快乐而生",
              "我们相信,最好的发现始于玩乐。",
              "## 玩乐,是一切可能的起点\n"
                  + "一个小小的挑战、一次共同的欢笑、再多试一次。WEMOVE 把运动带进那些让我们"
                  + " 彼此靠近的瞬间。\n\n"
                  + "## 我们的故事,一直在运动\n"
                  + "探索我们的系列产品,为家人找一项活动,或与我们联系了解更多。本网站目前"
                  + " 展示的是待品牌审阅的演示目录内容。"
            },
            {
              "quality-safety",
              "用心玩乐,从这里开始",
              "清晰的信息,让选择更安心。",
              "## 开始之前\n"
                  + "请始终遵循产品随附的具体说明、适龄建议和成人看护要求。\n\n"
                  + "## 咨询产品信息\n"
                  + "本示例目录不包含认证声明。如需确认的规格、材质和当前安全文件,请联系我们。"
            },
            {
              "support",
              "我们能帮您什么?",
              "为您的下一次冒险提供一点指引。",
              "## 找到您的答案\n"
                  + "浏览常见问题,或就具体产品联系我们。\n\n"
                  + "## 产品信息\n"
                  + "从目录中选择产品,通过联系按钮将问题随产品一起提交。"
            },
            {
              "privacy",
              "隐私信息",
              "关于通过本演示网站提交的信息。",
              "## 您选择分享的信息\n"
                  + "联系与合作伙伴表单会收集成年人的联系方式与商务留言。请勿提交儿童的个人信息。\n\n"
                  + "## 本网站如何运作\n"
                  + "必要的会话 Cookie 用于支持登录与安全表单。本网站未启用广告或分析 Cookie。"
                  + " 提交的留言会保存供管理员查阅。\n\n"
                  + "## 联系与查阅\n"
                  + "如需查阅或删除信息,请使用公开的联系地址。这是演示用政策文本;正式运营前,"
                  + " 运营方必须替换为经审阅的政策。"
            },
            {
              "terms",
              "网站条款",
              "关于使用本演示目录的信息。",
              "## 目录信息\n"
                  + "本网站上的产品与规格均为演示记录,不构成商业要约。活动照片仅作示意。\n\n"
                  + "## 不提供在线购买\n"
                  + "本网站接受产品咨询与合作咨询,不接受订单或付款。\n\n"
                  + "## 联系方式\n"
                  + "请向我们确认产品库存与规格。正式运营前,请将这些演示条款替换为经审阅的"
                  + " 商业条款。"
            }
          };
          for (String[] p : pages)
            createContent("page", p[0], p[1], p[2], p[3], null, null, true, now);
          createContent(
              "article",
              "five-ways-to-play-together",
              "五个一起动起来的小方法",
              "让平凡的时刻多一点乐趣。",
              "## 从小处开始\n"
                  + "留出十分钟,选一个舒适的空间,让每个人都帮忙决定玩什么。\n\n"
                  + "## 试一试友好的挑战\n"
                  + "数一数投中的次数、搭建一个简单的运动路线,或者发明一个平衡游戏。根据参与者"
                  + " 的情况调整挑战难度。\n\n"
                  + "## 保持轻松有趣\n"
                  + "轮流进行、为每一次努力喝彩,当有人需要休息时就暂停。始终使用适龄的装备并"
                  + " 由成人看护。",
              mids.get(5),
              "孩子正在用软栏、地面圆点和跳绳搭建运动路线",
              false,
              now);
          createContent(
              "article",
              "take-play-outside",
              "一点新鲜空气,一整段快乐时光。",
              "一起度过一个户外的下午。",
              "## 为探索腾出空间\n"
                  + "选择一个安全、开阔的区域,和一项大家都喜欢的活动。一个熟悉的球类游戏,加上"
                  + " 几条简单的规则,就能变成新的冒险。\n\n"
                  + "## 把家人聚在一起\n"
                  + "轮流发明挑战,把重点放在享受在一起的时光,而不是输赢。\n\n"
                  + "## 细心收拾\n"
                  + "按照装备的保养说明收纳,并在离开前检查活动区域。",
              mids.get(6),
              "孩子们正在户外玩球、豆袋和套圈挑战",
              false,
              now);
          String[][] faqs = {
            {
              "如何选择产品?",
              "按活动和建议年龄浏览,然后联系我们确认产品的规格和使用说明。",
              "产品"
            },
            {
              "可以直接在本网站购买吗?",
              "本目录接受咨询。请使用联系表单咨询产品和可用的购买渠道。",
              "订购"
            },
            {
              "我的企业如何成为合作伙伴?",
              "填写合作申请。提交后会获得一个回执编号,由我们的团队进行审核。",
              "合作"
            },
            {
              "在哪里可以找到安全信息?",
              "请遵循具体产品随附的说明。如需确认的安全与保养信息,请联系我们。",
              "支持"
            },
            {
              "我会收到确认邮件吗?",
              "本网站会在提交成功后显示屏幕回执编号,请妥善保存以备查阅。",
              "联系"
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
                      "WEMOVE",
                      "tagline",
                      "小小的动作,大大的发现。",
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
                  "小小的动作\n大大的发现。",
                  "subtitle",
                  "更多的运动、更多的想象、更多的亲子时刻。一起发现属于全家的运动世界。");
          hero.set(
              "image",
              node(
                  "media_id",
                  mids.get(0),
                  "alt",
                  "一家人正在玩 WEMOVE 运动玩乐系列"));
          hero.set("primary_cta", node("label", "开启下一场冒险", "href", "/products"));
          home.set("hero", hero);
          home.set(
              "dealer_cta",
              node(
                  "title",
                  "让更多人一起动起来。",
                  "description",
                  "把快乐运动的喜悦带到你的社区。与我们聊聊,成为 WEMOVE 合作伙伴。",
                  "button_label",
                  "成为合作伙伴"));
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
              "一家人正在玩 WEMOVE 运动玩乐系列"));
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
          case 1, 3 -> "室内玩法";
          case 5 -> "户外玩法";
          default -> "室内或户外玩法";
        };
    product.put("short_description", SHORT_DESCRIPTIONS.get(index));
    product.put(
        "description_markdown",
        "## 图中的每一件都在这里\n"
            + "本示例套装包含 "
            + COMPONENTS.get(index)
            + "。图片中出现的每一件物品都包含在内。\n\n"
            + "## 让挑战成为你的专属\n"
            + "从一个简单的小游戏开始,然后重新摆放道具、改变距离或邀请更多玩家。开放式设计让您轻松为不同空间和不同水平的孩子创造新玩法。\n\n"
            + "## 示例目录说明\n"
            + "此为演示用产品概念。商用前请确认最终材质、尺寸与安全说明。");
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
                    "完整套装内容见产品图片",
                    "重新组合道具,创造新挑战",
                    "为运动、想象与亲子共玩而设计")));
    product.set(
        "specifications",
        c.json()
            .valueToTree(
                List.of(
                    Map.of("name", "套装包含", "value", COMPONENTS.get(index)),
                    Map.of("name", "建议玩法", "value", playSetting),
                    Map.of("name", "目录状态", "value", "演示产品概念"))));
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
    return node("title", title + " | WEMOVE", "description", desc);
  }

  private ObjectNode node(Object... pairs) {
    ObjectNode n = c.json().createObjectNode();
    for (int i = 0; i < pairs.length; i += 2)
      n.set(pairs[i].toString(), c.json().valueToTree(pairs[i + 1]));
    return n;
  }
}
