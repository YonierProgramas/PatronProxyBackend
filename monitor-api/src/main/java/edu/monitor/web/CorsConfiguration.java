package edu.monitor.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class CorsConfiguration implements WebMvcConfigurer {
  private final String[] origins;

  public CorsConfiguration(@Value("${cors.allowed.origins}") String origins) {
    this.origins = origins.split(",");
    for (String origin : this.origins)
      if (origin.isBlank() || origin.contains("*"))
        throw new IllegalArgumentException("CORS requires explicit origins");
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/api/**")
        .allowedOrigins(origins)
        .allowedMethods("GET", "POST")
        .allowedHeaders("Content-Type")
        .exposedHeaders("X-Request-Id");
  }
}
