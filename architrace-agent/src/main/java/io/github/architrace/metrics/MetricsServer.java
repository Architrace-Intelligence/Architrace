/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.metrics;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.architrace.publish.PublisherStats;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;

public final class MetricsServer implements AutoCloseable {

    static final String METRICS_PATH = "/metrics";
    static final String HEALTH_PATH = "/health";
    private static final String PROMETHEUS_TEXT = "text/plain; version=0.0.4; charset=utf-8";
    private static final String JSON = "application/json; charset=utf-8";
    private static final int OK = 200;
    private static final int NOT_FOUND = 404;

    private final HttpServer server;
    private final PublisherStats publisher;

    public MetricsServer(int port, PrometheusMeterRegistry registry, PublisherStats publisher) {
        Objects.requireNonNull(registry, "registry");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to bind the metrics endpoint on port " + port, e);
        }
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext(METRICS_PATH, exchange -> respond(exchange, OK, PROMETHEUS_TEXT, registry.scrape()));
        server.createContext(HEALTH_PATH, exchange -> respond(exchange, OK, JSON, health()));
        server.createContext("/", exchange -> respond(exchange, NOT_FOUND, JSON, "{\"error\":\"not found\"}"));
    }

    public void start() {
        server.start();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    public void await() throws InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            LockSupport.park(this);
        }
        throw new InterruptedException("Interrupted while serving metrics");
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private String health() {
        return "{\"status\":\"UP\",\"controlPlane\":\"" + (publisher.isConnected() ? "CONNECTED" : "DISCONNECTED")
                + "\"}";
    }

    private static void respond(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
