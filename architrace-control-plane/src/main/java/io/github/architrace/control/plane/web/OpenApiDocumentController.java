/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.web;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class OpenApiDocumentController {

  @GetMapping(value = ApiPaths.DOCUMENT, produces = "application/yaml")
  Resource document() {
    return new ClassPathResource("openapi/architrace-query-api.yaml");
  }
}
