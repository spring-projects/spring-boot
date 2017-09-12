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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.MetricsConfigurationProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Metrics are recorded on server requests when
 * {@link MetricsConfigurationProperties.Web#autoTimeServerRequests} is true.
 * It is true by default.
 *
 * @author Jon Schneider
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = MetricsHandlerInterceptorAutoTimedTest.App.class)
@WebMvcTest({MetricsHandlerInterceptorAutoTimedTest.Controller.class})
@TestPropertySource(properties = "metrics.useGlobalRegistry=false")
public class MetricsHandlerInterceptorAutoTimedTest {
    @Autowired
	MockMvc mvc;

    @Autowired
	MeterRegistry registry;

    @Test
    public void metricsCanBeAutoTimed() throws Exception {
        mvc.perform(get("/api/10")).andExpect(status().isOk());
        assertThat(registry.find("http.server.requests").tags("status", "200").timer())
            .hasValueSatisfying(t -> assertThat(t.count()).isEqualTo(1));
    }

    @SpringBootApplication(scanBasePackages = "isolated")
    @Import({ Controller.class })
    static class App {
        @Bean
		MeterRegistry registry() {
            return new SimpleMeterRegistry();
        }
    }

    @RestController
    @RequestMapping("/api")
    static class Controller {
        @GetMapping("/{id}")
        public String successful(@PathVariable Long id) {
            return id.toString();
        }
    }
}
