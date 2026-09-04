package hdu.ljq.config;

import hdu.ljq.service.MediaService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  private final MediaService media;

  public WebConfig(MediaService media) {
    this.media = media;
  }

  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler("/media/**")
        .addResourceLocations(media.directory.toUri().toString())
        .setCachePeriod(86400);
  }
}
