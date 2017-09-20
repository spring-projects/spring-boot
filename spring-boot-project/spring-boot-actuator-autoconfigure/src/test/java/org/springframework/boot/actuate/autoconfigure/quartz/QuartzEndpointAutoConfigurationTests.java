/*
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

package org.springframework.boot.actuate.autoconfigure.quartz;

import org.junit.Test;
import org.quartz.Scheduler;

import org.springframework.boot.actuate.quartz.QuartzEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link QuartzEndpointAutoConfiguration}.
 *
 * @author Vedran Pavic
 */
public class QuartzEndpointAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(QuartzEndpointAutoConfiguration.class))
			.withUserConfiguration(QuartzConfiguration.class);

	@Test
	public void runShouldHaveEndpointBean() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(QuartzEndpoint.class));
	}

	@Test
	public void runWhenEnabledPropertyIsFalseShouldNotHaveEndpointBean() throws Exception {
		this.contextRunner.withPropertyValues("management.endpoint.quartz.enabled:false")
				.run((context) -> assertThat(context).doesNotHaveBean(QuartzEndpoint.class));
	}

	@Configuration
	static class QuartzConfiguration {

		@Bean
		public Scheduler scheduler() {
			return mock(Scheduler.class);
		}

	}

}
