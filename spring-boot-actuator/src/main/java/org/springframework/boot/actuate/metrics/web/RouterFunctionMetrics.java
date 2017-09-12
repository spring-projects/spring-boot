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

import static java.util.Collections.emptyList;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

/**
 * @author Jon Schneider
 */
public class RouterFunctionMetrics {
    private final MeterRegistry registry;
    private BiFunction<ServerRequest, ServerResponse, Collection<Tag>> defaultTags = (ServerRequest request, ServerResponse response) ->
        response != null ? Arrays.asList(method(request), status(response)) : Collections.singletonList(method(request));

    public RouterFunctionMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * @param defaultTags Generate a list of tags to apply to the timer. {@code ServerResponse} may be null.
     */
    public RouterFunctionMetrics defaultTags(BiFunction<ServerRequest, ServerResponse, Collection<Tag>> defaultTags) {
        this.defaultTags = defaultTags;
        return this;
    }

    public HandlerFilterFunction<ServerResponse, ServerResponse> timer(String name) {
        return timer(name, emptyList());
    }

    public HandlerFilterFunction<ServerResponse, ServerResponse> timer(String name, String... tags) {
        return timer(name, Tags.zip(tags));
    }

    public HandlerFilterFunction<ServerResponse, ServerResponse> timer(String name, Iterable<Tag> tags) {
        return (request, next) -> {
            final long start = System.nanoTime();
            return next
                    .handle(request)
                    .doOnSuccess(response -> {
                        Iterable<Tag> allTags = Tags.concat(tags, defaultTags.apply(request, response));
                        registry.timer(name, allTags).record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
                    })
                    .doOnError(error -> {
                        // FIXME how do we get the response under an error condition?
                        Iterable<Tag> allTags = Tags.concat(tags, defaultTags.apply(request, null));
                        registry.timer(name, allTags).record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
                    });
        };
    }

    /**
     * @param request The HTTP request.
     * @return A "method" tag whose value is a capitalized method (e.g. GET).
     */
    public static Tag method(ServerRequest request) {
        return Tag.of("method", request.method().toString());
    }

    /**
     * @param response The HTTP response.
     * @return A "status" tag whose value is the numeric status code.
     */
    public static Tag status(ServerResponse response) {
        return Tag.of("status", response.statusCode().toString());
    }
}
