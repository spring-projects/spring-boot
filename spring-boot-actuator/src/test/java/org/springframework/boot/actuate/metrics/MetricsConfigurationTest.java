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

import static io.micrometer.core.instrument.Statistic.Count;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.LogbackMetrics;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * @author Jon Schneider
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "metrics.useGlobalRegistry=false")
public class MetricsConfigurationTest {
    @Autowired
	ApplicationContext context;

    @Autowired
	RestTemplate external;

    @Autowired
	TestRestTemplate loopback;

    @Autowired
	MeterRegistry registry;

    @Test
    public void restTemplateIsInstrumented() {
        MockRestServiceServer server = MockRestServiceServer.bindTo(external).build();
        server.expect(once(), requestTo("/api/external")).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("{\"message\": \"hello\"}", MediaType.APPLICATION_JSON));

        //noinspection unchecked
        assertThat(external.getForObject("/api/external", Map.class))
            .containsKey("message");

        assertThat(registry.find("http.client.requests").value(Count, 1.0).timer()).isPresent();
    }

    @Test
    public void requestMappingIsInstrumented() {
        loopback.getForObject("/api/people", Set.class);

        assertThat(registry.find("http.server.requests").value(Count, 1.0).timer()).isPresent();
    }

    @Test
    public void automaticallyRegisteredBinders() {
        assertThat(context.getBeansOfType(MeterBinder.class).values())
                .hasAtLeastOneElementOfType(LogbackMetrics.class)
                .hasAtLeastOneElementOfType(JvmMemoryMetrics.class);
    }

    @SpringBootApplication(scanBasePackages = "isolated")
    @Import(PersonController.class)
    static class MetricsApp {
        public static void main(String[] args) {
            SpringApplication.run(MetricsApp.class, "--debug");
        }

        @Bean
        public MeterRegistry registry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        public RestTemplate restTemplate() {
            return new RestTemplate();
        }
    }

    @RestController
    static class PersonController {
        @GetMapping("/api/people")
        Set<String> personName() {
            return Collections.singleton("Jon");
        }
    }
}