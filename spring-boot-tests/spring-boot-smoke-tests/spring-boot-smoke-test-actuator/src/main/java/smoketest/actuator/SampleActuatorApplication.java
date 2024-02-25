/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package smoketest.actuator;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

/**
 * SampleActuatorApplication class.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SampleActuatorApplication {

	/**
     * Creates a health indicator for the "hello" endpoint.
     * 
     * @return the health indicator for the "hello" endpoint
     */
    @Bean
	public HealthIndicator helloHealthIndicator() {
		return createHealthIndicator("world");
	}

	/**
     * Creates a composite health contributor that combines multiple nested health contributors.
     * 
     * @return the composite health contributor
     */
    @Bean
	public HealthContributor compositeHelloHealthContributor() {
		Map<String, HealthContributor> map = new LinkedHashMap<>();
		map.put("spring", createNestedHealthContributor("spring"));
		map.put("boot", createNestedHealthContributor("boot"));
		return CompositeHealthContributor.fromMap(map);
	}

	/**
     * Creates a nested HealthContributor with the given name.
     * 
     * @param name the name of the HealthContributor
     * @return the created nested HealthContributor
     */
    private HealthContributor createNestedHealthContributor(String name) {
		Map<String, HealthContributor> map = new LinkedHashMap<>();
		map.put("a", createHealthIndicator(name + "-a"));
		map.put("b", createHealthIndicator(name + "-b"));
		map.put("c", createHealthIndicator(name + "-c"));
		return CompositeHealthContributor.fromMap(map);
	}

	/**
     * Creates a health indicator with the given value.
     * 
     * @param value the value to be included in the health indicator details
     * @return a HealthIndicator object representing the health indicator
     */
    private HealthIndicator createHealthIndicator(String value) {
		return () -> Health.up().withDetail("hello", value).build();
	}

	/**
     * The main method of the SampleActuatorApplication class.
     * 
     * @param args the command line arguments
     */
    public static void main(String[] args) {
		SpringApplication application = new SpringApplication(SampleActuatorApplication.class);
		application.setApplicationStartup(new BufferingApplicationStartup(1024));
		application.run(args);
	}

}
