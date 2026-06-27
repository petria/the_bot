package org.freakz.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SpaForwardConfig implements WebMvcConfigurer {

  private static final long ONE_HOUR_MILLIS = 60L * 60L * 1000L;

  @Override
  public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
    configurer.setDefaultTimeout(ONE_HOUR_MILLIS);
  }

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addViewController("/")
        .setViewName("forward:/index.html");
    registry.addViewController("/{path:[^\\.]*}")
        .setViewName("forward:/index.html");
    registry.addViewController("/**/{path:[^\\.]*}")
        .setViewName("forward:/index.html");
  }
}
