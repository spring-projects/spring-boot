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
package org.springframework.boot.actuate.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.graphite.GraphiteMeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;

/**
 * Validate that the default composite registry is filled with implementations
 * available on the classpath
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {
    "metrics.useGlobalRegistry=false",
    "metrics.datadog.enabled=false" // this requires an API key
})
public class MetricsConfigurationCompositeTest {
    @Autowired
	CompositeMeterRegistry registry;

    @Test
    public void compositeContainsImplementationsOnClasspath() {
        assertThat(registry.getRegistries())
            .hasAtLeastOneElementOfType(PrometheusMeterRegistry.class)
            .hasAtLeastOneElementOfType(GraphiteMeterRegistry.class);
    }

    @SpringBootApplication(scanBasePackages = "isolated")
    static class MetricsApp {}
}
