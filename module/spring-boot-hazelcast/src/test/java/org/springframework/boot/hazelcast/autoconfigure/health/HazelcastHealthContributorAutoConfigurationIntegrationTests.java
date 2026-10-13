/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.hazelcast.autoconfigure.health;

import com.hazelcast.core.HazelcastInstance;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.hazelcast.autoconfigure.HazelcastAutoConfiguration;
import org.springframework.boot.hazelcast.health.HazelcastHealthIndicator;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.resources.WithResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link HazelcastHealthContributorAutoConfiguration}.
 *
 * @author Dmytro Nosan
 */
@WithResource(name = "hazelcast.xml", content = """
		<hazelcast
				xsi:schemaLocation="http://www.hazelcast.com/schema/config hazelcast-config-5.0.xsd"
				xmlns="http://www.hazelcast.com/schema/config"
				xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
			<map name="defaultCache" />
			<network>
				<join>
					<auto-detection enabled="false"/>
					<multicast enabled="false"/>
				</join>
			</network>
		</hazelcast>
		""")
class HazelcastHealthContributorAutoConfigurationIntegrationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(HazelcastHealthContributorAutoConfiguration.class,
				HazelcastAutoConfiguration.class, HealthContributorAutoConfiguration.class));

	@Test
	void hazelcastUp() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(HazelcastInstance.class).hasSingleBean(HazelcastHealthIndicator.class);
			HazelcastInstance hazelcast = context.getBean(HazelcastInstance.class);
			Health health = context.getBean(HazelcastHealthIndicator.class).health();
			assertThat(health.getStatus()).isEqualTo(Status.UP);
			assertThat(health.getDetails()).containsOnlyKeys("name", "uuid")
				.containsEntry("name", hazelcast.getName())
				.containsEntry("uuid", hazelcast.getLocalEndpoint().getUuid().toString());
		});
	}

	@Test
	void hazelcastDown() {
		this.contextRunner.run((context) -> {
			context.getBean(HazelcastInstance.class).shutdown();
			assertThat(context).hasSingleBean(HazelcastHealthIndicator.class);
			Health health = context.getBean(HazelcastHealthIndicator.class).health();
			assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		});
	}

}
