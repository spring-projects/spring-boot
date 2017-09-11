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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.actuate.metrics.MetricsConfigurationProperties;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.stats.hist.Histogram;

/**
 * Intercepts RestTemplate requests and records metrics about execution time and results.
 *
 * @since 2.0.0
 * @author Jon Schneider
 */
public class MetricsRestTemplateInterceptor implements ClientHttpRequestInterceptor {
    private final MeterRegistry meterRegistry;
    private final RestTemplateTagConfigurer tagProvider;
    private final MetricsConfigurationProperties properties;

    public MetricsRestTemplateInterceptor(MeterRegistry meterRegistry,
                                          RestTemplateTagConfigurer tagProvider,
                                          MetricsConfigurationProperties properties) {
        this.tagProvider = tagProvider;
        this.meterRegistry = meterRegistry;
        this.properties = properties;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
										ClientHttpRequestExecution execution) throws IOException {
        long startTime = System.nanoTime();

        ClientHttpResponse response = null;
        try {
            response = execution.execute(request, body);
            return response;
        } finally {
            Timer.Builder builder = Timer.builder(properties.getWeb().getClientRequestsName())
                .tags(tagProvider.clientHttpRequestTags(request, response))
                .description("Timer of RestTemplate operation");

            if(properties.getWeb().getClientRequestPercentiles())
                builder = builder.histogram(Histogram.percentiles());

            builder
                .register(meterRegistry)
                .record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        }
    }
}
