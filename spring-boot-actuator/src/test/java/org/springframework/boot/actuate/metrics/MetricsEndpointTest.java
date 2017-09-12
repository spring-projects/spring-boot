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

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * @author Jon Schneider
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "metrics.useGlobalRegistry=false",
    "endpoints.metrics.web.enabled=true"
})
public class MetricsEndpointTest {
    @Autowired
	TestRestTemplate loopback;

    ObjectMapper mapper = new ObjectMapper();

    @Test
    public void listNames() throws IOException {
        List<String> names = mapper.readValue(loopback.getForObject("/application/metrics", String.class),
            new TypeReference<List<String>>() {});

        assertThat(names).contains("jvm.memory.used");
    }

    @Test
    public void selectByName() throws IOException {
        Map<String, Collection<MetricsEndpoint.MeasurementSample>> measurements = mapper.readValue(loopback.getForObject("/application/metrics/jvm.memory.used", String.class),
            new TypeReference<Map<String, Collection<MetricsEndpoint.MeasurementSample>>>() {});

        System.out.println(measurements);

        // one entry per tag combination
        assertThat(measurements).containsKeys(
            "jvm_memory_used.area.nonheap.id.Compressed_Class_Space",
            "jvm_memory_used.area.heap.id.PS_Old_Gen");
    }

    @SpringBootApplication(scanBasePackages = "isolated")
    static class MetricsApp {
        @Bean
        public MeterRegistry registry() {
            return new SimpleMeterRegistry();
        }
    }
}
