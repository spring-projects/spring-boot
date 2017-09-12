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
package org.springframework.boot.actuate.metrics.export.simple;

import org.springframework.boot.actuate.metrics.export.MetricsExporter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@Configuration
@EnableConfigurationProperties(SimpleConfigurationProperties.class)
public class SimpleExportConfiguration {
    @ConditionalOnProperty(value = "metrics.simple.enabled", matchIfMissing = true)
    @ConditionalOnMissingBean(MetricsExporter.class) // steps out of the way the moment any other monitoring system is configured
    @Bean
    public MetricsExporter simpleExporter(Clock clock) {
        return () -> new SimpleMeterRegistry(clock);
    }

    @ConditionalOnMissingBean
    @Bean
    public Clock clock() {
        return Clock.SYSTEM;
    }
}
