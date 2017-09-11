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

import static io.micrometer.core.instrument.Statistic.Count;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = "metrics.useGlobalRegistry=false")
public class RestTemplateUrlTemplateCapturingTest {
    @Autowired
	RestTemplate restTemplate;

    @Autowired
	MeterRegistry registry;

    @Test
    public void urlCaptured() throws URISyntaxException {
        MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
        mockServer.expect(ExpectedCount.times(2), MockRestRequestMatchers.requestTo("/test/123"))
            .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
            .andRespond(MockRestResponseCreators.withSuccess("OK", MediaType.APPLICATION_JSON));

        restTemplate.getForObject("/test/{id}", String.class, "123");

        assertThat(registry.find("http.client.requests").tags("uri", "/test/{id}")
            .value(Count, 1.0).timer()).isPresent();

        // this would be an inappropriate way to use RestTemplate, as the unparameterized URI will be used
        // as a tag value, potentially causing dimensional explosion
        restTemplate.getForObject(new URI("/test/123"), String.class);

        // issue #98
        assertThat(registry.find("http.client.requests").tags("uri", "/test/123")
            .value(Count, 1.0).timer()).isPresent();
    }

    @SpringBootApplication(scanBasePackages = "isolated")
    static class MetricsApp {
        @Bean
        public MeterRegistry registry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        public RestTemplate restTemplate() {
            return new RestTemplate();
        }
    }
}
