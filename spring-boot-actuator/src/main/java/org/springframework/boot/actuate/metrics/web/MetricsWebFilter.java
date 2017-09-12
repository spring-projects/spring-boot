/**
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.boot.actuate.metrics.web;

import java.util.concurrent.TimeUnit;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import io.micrometer.core.instrument.MeterRegistry;
import reactor.core.publisher.Mono;

/**
 * Intercepts incoming HTTP requests modeled with the Webflux annotation-based programming model.
 *
 * @author Jon Schneider
 */
public class MetricsWebFilter implements WebFilter {
    private final MeterRegistry registry;
    private final WebfluxTagConfigurer tagConfigurer;
    private final String metricName;

    public MetricsWebFilter(MeterRegistry registry,
                            WebfluxTagConfigurer tagConfigurer,
                            String metricName) {
        this.registry = registry;
        this.tagConfigurer = tagConfigurer;
        this.metricName = metricName;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return chain.filter(exchange). compose(f -> {
            long start = System.nanoTime();
            return f
                .doOnSuccess(done ->
                    registry.timer(metricName, tagConfigurer.httpRequestTags(exchange, null))
                        .record(System.nanoTime() - start, TimeUnit.NANOSECONDS)
                )
                .doOnError(t ->
                    registry.timer(metricName, tagConfigurer.httpRequestTags(exchange, t))
                        .record(System.nanoTime() - start, TimeUnit.NANOSECONDS)
                );
        });
    }
}