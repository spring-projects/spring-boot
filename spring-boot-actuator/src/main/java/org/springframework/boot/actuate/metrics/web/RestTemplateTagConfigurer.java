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

import static java.util.Arrays.asList;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import io.micrometer.core.instrument.Tag;

/**
 * Defines the default set of tags added to instrumented web requests. It is only necessary to implement
 * providers for the programming model(s) you are using.
 *
 * @since 2.0.0
 * @author Jon Schneider
 */
public class RestTemplateTagConfigurer {
    /**
     * Supplies default tags to timers monitoring RestTemplate requests.
     *
     * @param request  RestTemplate client HTTP request
     * @param response may be null in the event of a client error
     * @return a set of tags added to every client HTTP request metric
     */
    Iterable<Tag> clientHttpRequestTags(HttpRequest request,
                                        ClientHttpResponse response) {
        return asList(method(request), uri(), status(response), clientName(request));
    }

    public Tag method(HttpRequest request) {
        return Tag.of("method", request.getMethod().name());
    }

    public Tag uri() {
        String urlTemplate = RestTemplateUrlTemplateHolder.getRestTemplateUrlTemplate();
        if (urlTemplate == null) {
            urlTemplate = "none";
        }

        String strippedUrlTemplate = urlTemplate.replaceAll("^https?://[^/]+/", "");
        return Tag.of("uri", strippedUrlTemplate);
    }

    public Tag status(ClientHttpResponse response) {
        String status;
        try {
            status = (response == null) ? "CLIENT_ERROR" : ((Integer) response
                .getRawStatusCode()).toString();
        } catch (IOException e) {
            status = "IO_ERROR";
        }

        return Tag.of("status", status);
    }

    public Tag clientName(HttpRequest request) {
        String host = request.getURI().getHost();
        if (host == null) {
            host = "none";
        }

        return Tag.of("clientName", host);
    }
}
