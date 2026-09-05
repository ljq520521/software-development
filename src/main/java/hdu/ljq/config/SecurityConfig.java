package hdu.ljq.config;

import hdu.ljq.common.*;
import hdu.ljq.persistence.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.*;
import org.springframework.security.web.csrf.*;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwords() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  HttpSessionCsrfTokenRepository csrfRepository() {
    HttpSessionCsrfTokenRepository r = new HttpSessionCsrfTokenRepository();
    r.setHeaderName("X-CSRF-Token");
    return r;
  }

  @Bean
  SecurityContextRepository contexts() {
    return new HttpSessionSecurityContextRepository();
  }

  @Bean
  SecurityFilterChain security(
      HttpSecurity http,
      HttpSessionCsrfTokenRepository csrf,
      SecurityContextRepository contexts,
      ApiResponses responses,
      Repository repo)
      throws Exception {
    http.csrf(
            c ->
                c.csrfTokenRepository(csrf)
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
        .securityContext(c -> c.securityContextRepository(contexts))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers("/api/v1/admin/**", "/api/v1/auth/me", "/api/v1/auth/logout")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/v1/dealer/auth/me", "/api/v1/dealer/auth/logout")
                    .hasRole("DEALER")
                    .anyRequest()
                    .permitAll())
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .requestCache(AbstractHttpConfigurer::disable)
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (r, s, x) ->
                            responses.write(
                                r, s, new ApiException(401, "UNAUTHENTICATED", "Please sign in.")))
                    .accessDeniedHandler(
                        (r, s, x) ->
                            responses.write(
                                r,
                                s,
                                new ApiException(
                                    403,
                                    x instanceof CsrfException ? "CSRF_INVALID" : "FORBIDDEN",
                                    "The request could not be authorized."))))
        .headers(
            h ->
                h.contentSecurityPolicy(
                    c ->
                        c.policyDirectives(
                            "default-src 'self'; script-src 'self'; style-src 'self'; img-src"
                                + " 'self' blob:; font-src 'self'; connect-src 'self'; object-src"
                                + " 'none'; base-uri 'self'; frame-ancestors 'none'; form-action"
                                + " 'self'")));
    http.addFilterBefore(
        new OncePerRequestFilter() {
          @Override
          protected void doFilterInternal(
              HttpServletRequest r, HttpServletResponse s, FilterChain chain)
              throws ServletException, IOException {
            HttpSession session = r.getSession(false);
            if (session != null && session.getAttribute("actorId") != null) {
              boolean invalid =
                  System.currentTimeMillis() - (long) session.getAttribute("loginAt") > 28800000;
              try {
                invalid |=
                    !repo.find(EntityType.ADMIN, session.getAttribute("actorId").toString())
                        .path("status")
                        .asText()
                        .equals("active");
              } catch (ApiException ex) {
                invalid = true;
              }
              if (invalid) {
                session.invalidate();
                org.springframework.security.core.context.SecurityContextHolder.clearContext();
              }
            } else if (session != null && session.getAttribute("dealerAccountId") != null) {
              boolean invalid =
                  System.currentTimeMillis() - (long) session.getAttribute("dealerLoginAt")
                      > 28800000;
              try {
                invalid |=
                    !repo.find(
                            EntityType.DEALER,
                            session.getAttribute("dealerAccountId").toString())
                        .path("status")
                        .asText()
                        .equals("active");
              } catch (ApiException ex) {
                invalid = true;
              }
              if (invalid) {
                session.invalidate();
                org.springframework.security.core.context.SecurityContextHolder.clearContext();
              }
            }
            chain.doFilter(r, s);
          }
        },
        AuthorizationFilter.class);
    return http.build();
  }
}
