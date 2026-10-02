/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

final class SpaFallbackResourceResolver extends PathResourceResolver {

    static final String INDEX = "index.html";

    private final List<String> serverRoutes;

    SpaFallbackResourceResolver(List<String> serverRoutes) {
        this.serverRoutes = serverRoutes.stream()
                .map(SpaFallbackResourceResolver::asRequestPath)
                .toList();
    }

    @Override
    protected Resource resolveResourceInternal(
            HttpServletRequest request,
            String requestPath,
            List<? extends Resource> locations,
            ResourceResolverChain chain) {
        Resource requested = super.resolveResourceInternal(request, requestPath, locations, chain);
        if (requested != null || !isApplicationRoute(requestPath) || !acceptsHtml(request)) {
            return requested;
        }
        return super.resolveResourceInternal(request, INDEX, locations, chain);
    }

    boolean isApplicationRoute(String requestPath) {
        return serverRoutes.stream().noneMatch(route -> isUnder(requestPath, route))
                && !lastSegment(requestPath).contains(".");
    }

    private static boolean acceptsHtml(HttpServletRequest request) {
        if (request == null) {
            return true;
        }
        List<MediaType> accepted = MediaType.parseMediaTypes(request.getHeader(HttpHeaders.ACCEPT));
        return accepted.isEmpty() || accepted.stream().anyMatch(type -> type.includes(MediaType.TEXT_HTML));
    }

    private static String asRequestPath(String route) {
        return route.charAt(0) == '/' ? route.substring(1) : route;
    }

    private static boolean isUnder(String requestPath, String route) {
        return requestPath.equals(route) || requestPath.startsWith(route + '/');
    }

    private static String lastSegment(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
