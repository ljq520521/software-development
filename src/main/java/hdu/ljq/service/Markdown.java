package hdu.ljq.service;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Component;

@Component("markdown")
public class Markdown {
  private final Parser parser = Parser.builder().build();
  private final HtmlRenderer renderer =
      HtmlRenderer.builder().escapeHtml(true).sanitizeUrls(true).build();

  public String render(String value) {
    return renderer.render(parser.parse(value == null ? "" : value));
  }
}
