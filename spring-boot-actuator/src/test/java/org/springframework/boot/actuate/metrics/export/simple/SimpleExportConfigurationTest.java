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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * @author Jon Schneider
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {
    "metrics.atlas.enabled=false",
    "metrics.prometheus.enabled=false",
    "metrics.datadog.enabled=false",
    "metrics.ganglia.enabled=false",
    "metrics.graphite.enabled=false",
    "metrics.influx.enabled=false",
    "metrics.jmx.enabled=false"
})
public class SimpleExportConfigurationTest {
    @Autowired
	CompositeMeterRegistry registry;

    @Test
    public void simpleMeterRegistryIsInTheCompositeWhenNoOtherRegistryIs() {
        assertThat(registry.getRegistries())
            .hasAtLeastOneElementOfType(SimpleMeterRegistry.class);
    }

    @SpringBootApplication(scanBasePackages = "isolated")
    static class MetricsApp {}
}