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

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;

/**
 * @author Jon Schneider
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "metrics.useGlobalRegistry=false",
    "endpoints.prometheus.web.enabled=true"
})
public class PrometheusScrapeEndpointTest {
    @Autowired
	TestRestTemplate loopback;

    @Test
    public void scrapeHasContentTypeText004() {
        ResponseEntity<String> response = loopback.getForEntity("/application/prometheus", String.class);
        AssertionsForClassTypes.assertThat(response)
            .satisfies(r -> AssertionsForClassTypes.assertThat(r.getStatusCode().value()).isEqualTo(200))
            .satisfies(r -> assertThat(r.getHeaders().get(CONTENT_TYPE))
                .hasOnlyOneElementSatisfying(type -> AssertionsForClassTypes.assertThat(type).contains("0.0.4")));
    }

    @SpringBootApplication(scanBasePackages = "isolated")
    static class MetricsApp {
        @Bean
        public CollectorRegistry promRegistry() {
            return new CollectorRegistry(true);
        }

        @Bean
        public MeterRegistry registry(CollectorRegistry registry) {
            return new PrometheusMeterRegistry(k -> null, registry, Clock.SYSTEM);
        }
    }
}
