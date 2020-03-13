/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.jet;

import com.hazelcast.jet.JetInstance;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.jet.HazelcastJetHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jet.HazelcastJetAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link HazelcastJetHealthContributorAutoConfiguration}.
 *
 * @author Ali Gurbuz
 */
class HazelcastJetHealthContributorAutoConfigurationIntegrationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(HazelcastJetHealthContributorAutoConfiguration.class,
					HazelcastJetAutoConfiguration.class, HealthContributorAutoConfiguration.class));

	@Test
	void hazelcastUp() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(JetInstance.class).hasSingleBean(HazelcastJetHealthIndicator.class);
			JetInstance jet = context.getBean(JetInstance.class);
			Health health = context.getBean(HazelcastJetHealthIndicator.class).health();
			assertThat(health.getStatus()).isEqualTo(Status.UP);
			assertThat(health.getDetails()).containsOnlyKeys("name", "uuid").containsEntry("name", jet.getName())
					.containsEntry("uuid", jet.getHazelcastInstance().getLocalEndpoint().getUuid());
		});
	}

	@Test
	void hazelcastDown() {
		this.contextRunner.run((context) -> {
			context.getBean(JetInstance.class).shutdown();
			assertThat(context).hasSingleBean(HazelcastJetHealthIndicator.class);
			Health health = context.getBean(HazelcastJetHealthIndicator.class).health();
			assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		});
	}

}
