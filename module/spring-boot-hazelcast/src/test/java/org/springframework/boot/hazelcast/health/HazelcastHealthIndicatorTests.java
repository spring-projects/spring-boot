/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.hazelcast.health;

import com.hazelcast.core.HazelcastException;
import com.hazelcast.core.HazelcastInstance;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.hazelcast.autoconfigure.HazelcastAutoConfiguration;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.resources.WithResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HazelcastHealthIndicator}.
 *
 * @author Dmytro Nosan
 * @author Stephane Nicoll
 */
class HazelcastHealthIndicatorTests {

	@Test
	@WithResource(name = "hazelcast.xml", content = """
			<hazelcast xmlns="http://www.hazelcast.com/schema/config"
					   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
					   xsi:schemaLocation="http://www.hazelcast.com/schema/config
			           http://www.hazelcast.com/schema/config/hazelcast-config-5.0.xsd">
				<instance-name>actuator-hazelcast</instance-name>
				<map name="defaultCache" />
				<network>
					<join>
						<auto-detection enabled="false"/>
						<multicast enabled="false"/>
					</join>
				</network>
			</hazelcast>
			""")
	void hazelcastUp() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(HazelcastAutoConfiguration.class))
			.withPropertyValues("spring.hazelcast.config=hazelcast.xml")
			.run((context) -> {
				HazelcastInstance hazelcast = context.getBean(HazelcastInstance.class);
				Health health = new HazelcastHealthIndicator(hazelcast).health();
				assertThat(health.getStatus()).isEqualTo(Status.UP);
				assertThat(health.getDetails()).containsOnlyKeys("name", "uuid")
					.containsEntry("name", "actuator-hazelcast");
				assertThat(health.getDetails().get("uuid")).asString().isNotEmpty();
			});
	}

	@Test
	void hazelcastDown() {
		HazelcastInstance hazelcast = mock(HazelcastInstance.class);
		given(hazelcast.executeTransaction(any())).willThrow(new HazelcastException());
		Health health = new HazelcastHealthIndicator(hazelcast).health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
	}

}
