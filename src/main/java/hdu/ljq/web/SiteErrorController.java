package hdu.ljq.web;

import hdu.ljq.common.*;
import hdu.ljq.service.CatalogService;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.*;
import java.util.Map;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SiteErrorController implements ErrorController {
  private final CatalogService catalog;
  private final ApiResponses responses;

  public SiteErrorController(CatalogService c, ApiResponses a) {
    catalog = c;
    responses = a;
  }

  @RequestMapping("/error")
  public String error(HttpServletRequest request, HttpServletResponse response, Model model)
      throws Exception {
    Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
    int code = status instanceof Integer n ? n : 404;
    String message =
        code == 404 ? "We couldn't find that page." : "Something went wrong. Please try again.";
    String original = String.valueOf(request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));
    if (original.startsWith("/api/")) {
      responses.write(
          request,
          response,
          new ApiException(code, code == 404 ? "NOT_FOUND" : "INTERNAL_ERROR", message));
      return null;
    }
    model.addAttribute("site", catalog.json().convertValue(catalog.site(), Map.class));
    model.addAttribute("title", "Page not found | WEMOVE SPORTS");
    model.addAttribute("description", message);
    model.addAttribute("current", "");
    model.addAttribute("status", code);
    model.addAttribute("errorMessage", message);
    response.setStatus(code);
    return "error";
  }
}
