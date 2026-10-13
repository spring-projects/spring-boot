/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.actuator;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.health.contributor.CompositeHealthContributor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SampleActuatorApplication {

	@Bean
	public HealthIndicator helloHealthIndicator() {
		return createHealthIndicator("world");
	}

	@Bean
	public HealthContributor compositeHelloHealthContributor() {
		Map<String, HealthContributor> map = new LinkedHashMap<>();
		map.put("spring", createNestedHealthContributor("spring"));
		map.put("boot", createNestedHealthContributor("boot"));
		return CompositeHealthContributor.fromMap(map);
	}

	private HealthContributor createNestedHealthContributor(String name) {
		Map<String, HealthContributor> map = new LinkedHashMap<>();
		map.put("a", createHealthIndicator(name + "-a"));
		map.put("b", createHealthIndicator(name + "-b"));
		map.put("c", createHealthIndicator(name + "-c"));
		return CompositeHealthContributor.fromMap(map);
	}

	private HealthIndicator createHealthIndicator(String value) {
		return () -> Health.up().withDetail("hello", value).build();
	}

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(SampleActuatorApplication.class);
		application.setApplicationStartup(new BufferingApplicationStartup(1024));
		application.run(args);
	}

}
