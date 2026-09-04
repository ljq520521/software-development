package hdu.ljq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
    exclude =
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
            .class)
public class BuildingBlockWebApplication {

  public static void main(String[] args) {
    java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
    SpringApplication.run(BuildingBlockWebApplication.class, args);
  }
}
