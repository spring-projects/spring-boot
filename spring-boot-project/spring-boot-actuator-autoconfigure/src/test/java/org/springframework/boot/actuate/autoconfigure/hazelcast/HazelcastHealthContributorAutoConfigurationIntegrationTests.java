/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.hazelcast.HazelcastHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastAutoConfiguration;
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
