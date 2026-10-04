/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.architrace.publish.PublisherStats;
import io.micrometer.core.instrument.Counter;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MetricsServerTest {

    private final PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    private final PublisherStats publisher = new PublisherStats();
    private final MetricsServer sut = new MetricsServer(0, registry, publisher);
    private final HttpClient client = HttpClient.newHttpClient();

    @AfterEach
    void stop() {
        sut.close();
        client.close();
    }

    @Test
    void servesPrometheusTextHealthAndNotFound() throws IOException, InterruptedException {
        Counter.builder(AgentMetrics.PREFIX + "spans.received")
                .register(registry)
                .increment(3);
        sut.start();

        HttpResponse<String> metrics = get(MetricsServer.METRICS_PATH);
        HttpResponse<String> health = get(MetricsServer.HEALTH_PATH);
        publisher.connected(true);
        HttpResponse<String> connected = get(MetricsServer.HEALTH_PATH);
        HttpResponse<String> unknown = get("/nothing");

        assertThat(metrics.statusCode()).isEqualTo(200);
        assertThat(metrics.headers().firstValue("Content-Type")).contains("text/plain; version=0.0.4; charset=utf-8");
        assertThat(metrics.body()).contains("architrace_agent_spans_received_total 3.0");
        assertThat(health.statusCode()).isEqualTo(200);
        assertThat(health.body()).isEqualTo("{\"status\":\"UP\",\"controlPlane\":\"DISCONNECTED\"}");
        assertThat(connected.body()).isEqualTo("{\"status\":\"UP\",\"controlPlane\":\"CONNECTED\"}");
        assertThat(unknown.statusCode()).isEqualTo(404);
    }

    @Test
    void awaitReturnsOnlyWhenInterrupted() throws InterruptedException {
        sut.start();
        Thread waiter = Thread.ofPlatform().start(() -> {
            try {
                sut.await();
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            }
        });

        waiter.interrupt();
        waiter.join(5_000);

        assertThat(waiter.isAlive()).isFalse();
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + sut.port() + path))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
