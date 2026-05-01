package org.freakz.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SpaForwardConfig implements WebMvcConfigurer {

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addViewController("/{path:[^\\.]*}")
        .setViewName("forward:/index.html");
    registry.addViewController("/**/{path:[^\\.]*}")
        .setViewName("forward:/index.html");
  }
}
