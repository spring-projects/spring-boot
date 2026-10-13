/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.hazelcast.autoconfigure.health;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.hazelcast.autoconfigure.HazelcastAutoConfiguration;
import org.springframework.boot.hazelcast.health.HazelcastHealthIndicator;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.resources.WithResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HazelcastHealthContributorAutoConfiguration}.
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
class HazelcastHealthContributorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(HazelcastAutoConfiguration.class,
				HazelcastHealthContributorAutoConfiguration.class, HealthContributorAutoConfiguration.class));

	@Test
	void runShouldCreateIndicator() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(HazelcastHealthIndicator.class));
	}

	@Test
	void runWhenDisabledShouldNotCreateIndicator() {
		this.contextRunner.withPropertyValues("management.health.hazelcast.enabled:false")
			.run((context) -> assertThat(context).doesNotHaveBean(HazelcastHealthIndicator.class));
	}

}
