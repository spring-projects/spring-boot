/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.jms;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.jms.JmsHealthIndicator;
import org.springframework.boot.actuate.ldap.LdapHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.assertj.SimpleAsyncTaskExecutorAssert;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JmsHealthContributorAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class JmsHealthContributorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ArtemisAutoConfiguration.class,
				JmsHealthContributorAutoConfiguration.class, HealthContributorAutoConfiguration.class));

	@Test
	void runShouldCreateIndicator() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(JmsHealthIndicator.class));
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void shouldUseVirtualThreadsIfEnabled() {
		this.contextRunner.withPropertyValues("spring.threads.virtual.enabled=true").run((context) -> {
			JmsHealthIndicator jmsHealthIndicator = context.getBean(JmsHealthIndicator.class);
			assertThat(jmsHealthIndicator).isNotNull();
			Object taskExecutor = ReflectionTestUtils.getField(jmsHealthIndicator, "taskExecutor");
			assertThat(taskExecutor).isInstanceOf(SimpleAsyncTaskExecutor.class);
			SimpleAsyncTaskExecutorAssert.assertThat((SimpleAsyncTaskExecutor) taskExecutor).usesVirtualThreads();
		});
	}

	@Test
	void runWhenDisabledShouldNotCreateIndicator() {
		this.contextRunner.withPropertyValues("management.health.jms.enabled:false")
			.run((context) -> assertThat(context).doesNotHaveBean(LdapHealthIndicator.class));
	}

}
