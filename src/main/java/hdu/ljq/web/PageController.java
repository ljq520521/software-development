package hdu.ljq.web;

import com.fasterxml.jackson.databind.*;
import hdu.ljq.common.*;
import hdu.ljq.persistence.*;
import hdu.ljq.service.*;
import jakarta.servlet.http.*;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class PageController {
  private final CatalogService c;

  public PageController(CatalogService c) {
    this.c = c;
  }

  private void base(Model m, String title, String current) {
    m.addAttribute("site", map(c.site()));
    m.addAttribute("title", title + " | WEMOVE SPORTS");
    m.addAttribute(
        "description", "Discover movement, imagination and shared play with WEMOVE SPORTS.");
    m.addAttribute("current", current);
  }

  @GetMapping("/")
  public String home(Model m) {
    base(m, "Small moves. Big discoveries.", "home");
    m.addAttribute("home", map(c.home()));
    return "home";
  }

  @GetMapping("/products")
  public String products(@RequestParam Map<String, String> q, Model m) {
    base(m, "Explore our products", "products");
    m.addAttribute("result", map(c.list(EntityType.PRODUCT, q, false)));
    m.addAttribute(
        "categories",
        map(c.list(EntityType.CATEGORY, Map.of("page_size", "50"), false)).get("items"));
    m.addAttribute("filters", q);
    return "products";
  }

  @GetMapping("/products/{slug}")
  public String product(@PathVariable String slug, Model m) {
    JsonNode p = c.product(slug);
    base(m, p.path("seo").path("title").asText(), "products");
    m.addAttribute("description", p.path("seo").path("description").asText());
    m.addAttribute("product", map(p));
    return "product";
  }

  @GetMapping("/play")
  public String articles(@RequestParam Map<String, String> q, Model m) {
    base(m, "Play & learn", "play");
    Map<String, String> params = new HashMap<>(q);
    params.put("type", "article");
    m.addAttribute("result", map(c.list(EntityType.CONTENT, params, false)));
    return "articles";
  }

  @GetMapping({"/play/{slug}", "/pages/{slug}"})
  public String article(@PathVariable String slug, HttpServletRequest req, Model m) {
    JsonNode d = c.content(slug);
    boolean article = req.getRequestURI().startsWith("/play/");
    if (!d.path("type").asText().equals(article ? "article" : "page")) throw ApiException.missing();
    if (!article && Set.of("about", "quality-safety", "support", "privacy", "terms").contains(slug))
      return "redirect:/" + slug;
    return contentPage(d, m, article ? "play" : "about");
  }

  @GetMapping({"/about", "/quality-safety", "/privacy", "/terms"})
  public String page(HttpServletRequest r, Model m) {
    return contentPage(c.content(r.getRequestURI().substring(1)), m, "about");
  }

  private String contentPage(JsonNode d, Model m, String current) {
    base(m, d.path("title").asText(), current);
    m.addAttribute("description", d.path("seo").path("description").asText());
    m.addAttribute("article", map(d));
    return "article";
  }

  @GetMapping({"/support", "/support/faq"})
  public String support(@RequestParam Map<String, String> q, Model m) {
    base(m, "We're here to help", "support");
    m.addAttribute("result", map(c.list(EntityType.FAQ, q, false)));
    m.addAttribute("support", map(c.content("support")));
    return "support";
  }

  @GetMapping({"/contact", "/dealers/apply"})
  public String form(
      @RequestParam(required = false) String product_id, HttpServletRequest r, Model m) {
    boolean dealer = r.getRequestURI().startsWith("/dealers");
    base(m, dealer ? "Become a partner" : "Let's talk", dealer ? "dealers" : "contact");
    m.addAttribute("dealer", dealer);
    m.addAttribute("productId", product_id == null ? "" : product_id);
    java.util.List<Object> options = new java.util.ArrayList<>();
    int page = 1;
    while (true) {
      Map<String, Object> result =
          map(
              c.list(
                  EntityType.PRODUCT,
                  Map.of("page_size", "50", "page", String.valueOf(page)),
                  false));
      options.addAll((java.util.List<?>) result.get("items"));
      if (page++ >= ((Number) result.get("total_pages")).intValue()) break;
    }
    m.addAttribute("products", options);
    m.addAttribute(
        "countries",
        Arrays.stream(Locale.getISOCountries())
            .map(
                code ->
                    Map.of(
                        "code",
                        code,
                        "name",
                        Locale.of("", code).getDisplayCountry(Locale.ENGLISH)))
            .sorted(java.util.Comparator.comparing(item -> item.get("name")))
            .toList());
    return "form";
  }

  @GetMapping({"/admin", "/admin/{page}"})
  public String admin(Model m) {
    base(m, "管理后台", "admin");
    return "admin";
  }

  @GetMapping(value = "/robots.txt", produces = "text/plain")
  @ResponseBody
  public String robots() {
    return "User-agent: *\nDisallow: /\n";
  }

  @ExceptionHandler(ApiException.class)
  public String failure(ApiException e, Model m, HttpServletResponse r) {
    r.setStatus(e.status);
    base(m, e.status == 404 ? "Page not found" : "Please check your request", "");
    m.addAttribute("status", e.status);
    m.addAttribute("errorMessage", e.getMessage());
    return "error";
  }

  private Map<String, Object> map(JsonNode n) {
    return c.json()
        .convertValue(
            n, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
  }
}
