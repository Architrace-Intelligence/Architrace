/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.resource.ResourceResolverChain;

class SpaFallbackResourceResolverTest {

  private final List<Resource> locations = List.of(new ClassPathResource("static/"));
  private final ResourceResolverChain chain = new EndOfChain();
  private final SpaFallbackResourceResolver resolver =
      new SpaFallbackResourceResolver(ApiPaths.SERVER_ROUTES);

  @Test
  void servesAnExistingFileAsItself() throws IOException {
    Resource resource = resolve("swagger-ui/index.html", MediaType.ALL_VALUE);

    assertThat(resource).isNotNull();
    assertThat(resource.getURL().getPath()).endsWith("/static/swagger-ui/index.html");
  }

  @ParameterizedTest
  @ValueSource(strings = {"projects", "projects/webshop/PROD/k8s-prod-eu1", "map/v1.2/webshop"})
  void answersTheIndexPageForApplicationRoutesTheBrowserOpens(String route) throws IOException {
    Resource resource = resolve(route, "text/html,application/xhtml+xml,*/*;q=0.8");

    assertThat(resource).isNotNull();
    assertThat(resource.getURL().getPath()).endsWith("/static/index.html");
  }

  @Test
  void answersTheIndexPageWhenNoMediaTypeIsRequested() throws IOException {
    Resource withoutHeader = resolver.resolveResource(null, "projects", locations, chain);
    Resource withEmptyHeader = resolve("projects", "");

    assertThat(withoutHeader).isNotNull();
    assertThat(withoutHeader.getURL().getPath()).endsWith("/static/index.html");
    assertThat(withEmptyHeader).isNotNull();
  }

  @Test
  void leavesApplicationRoutesUnresolvedForClientsThatWantData() {
    assertThat(resolve("projects", MediaType.APPLICATION_JSON_VALUE)).isNull();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "api",
        "api/v1/missing",
        "actuator/missing",
        "swagger-ui/missing",
        "webjars/missing",
        "assets/missing.js",
        "missing.ico"
      })
  void leavesServerRoutesAndMissingFilesUnresolved(String path) {
    assertThat(resolve(path, MediaType.TEXT_HTML_VALUE)).isNull();
  }

  @Test
  void treatsOnlyTheLastSegmentAsAFileName() {
    assertThat(resolver.isApplicationRoute("map/v1.2/details")).isTrue();
    assertThat(resolver.isApplicationRoute("map/v1.2/details.json")).isFalse();
    assertThat(resolver.isApplicationRoute("apis/catalog")).isTrue();
  }

  private Resource resolve(String path, String accept) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/" + path);
    request.addHeader(HttpHeaders.ACCEPT, accept);
    return resolver.resolveResource(request, path, locations, chain);
  }

  private static final class EndOfChain implements ResourceResolverChain {

    @Override
    public Resource resolveResource(
        HttpServletRequest request, String requestPath, List<? extends Resource> locations) {
      return null;
    }

    @Override
    public String resolveUrlPath(String resourcePath, List<? extends Resource> locations) {
      return null;
    }
  }
}
