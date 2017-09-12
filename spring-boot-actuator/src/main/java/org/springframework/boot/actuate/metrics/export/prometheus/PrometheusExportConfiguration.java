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
package org.springframework.boot.actuate.metrics.export.prometheus;

import org.springframework.boot.actuate.autoconfigure.ManagementContextConfiguration;
import org.springframework.boot.actuate.metrics.export.MetricsExporter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.Clock;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;

/**
 * @since 2.0.0
 * @author Jon Schneider
 */
@Configuration
@ConditionalOnClass(name = "io.micrometer.prometheus.PrometheusMeterRegistry")
@EnableConfigurationProperties(PrometheusConfigurationProperties.class)
public class PrometheusExportConfiguration {
    @ConditionalOnProperty(value = "metrics.prometheus.enabled", matchIfMissing = true)
    @Bean
    public MetricsExporter prometheusExporter(PrometheusConfig config, CollectorRegistry collectorRegistry, Clock clock) {
        return () -> new PrometheusMeterRegistry(config, collectorRegistry, clock);
    }

    @ConditionalOnMissingBean
    @Bean
    public CollectorRegistry collectorRegistry() {
        return new CollectorRegistry(true);
    }

    @ConditionalOnMissingBean
    @Bean
    public Clock clock() {
        return Clock.SYSTEM;
    }

    @ManagementContextConfiguration
    public static class PrometheusScrapeEndpointConfiguration {
        @Bean
        public PrometheusScrapeEndpoint prometheusEndpoint(CollectorRegistry collectorRegistry) {
            return new PrometheusScrapeEndpoint(collectorRegistry);
        }
    }
}
