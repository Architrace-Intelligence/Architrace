/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.web;

import java.time.Duration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebConfiguration implements WebMvcConfigurer {

  static final String UI_LOCATION = "classpath:/static/";

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addRedirectViewController(ApiPaths.SWAGGER_UI, ApiPaths.SWAGGER_UI + "/index.html");
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler(ApiPaths.UI_ASSETS + "/**")
        .addResourceLocations(UI_LOCATION + "assets/")
        .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).immutable());
    registry
        .addResourceHandler("/**")
        .addResourceLocations(UI_LOCATION)
        .setCacheControl(CacheControl.noCache())
        .resourceChain(false)
        .addResolver(new SpaFallbackResourceResolver(ApiPaths.SERVER_ROUTES));
  }
}
