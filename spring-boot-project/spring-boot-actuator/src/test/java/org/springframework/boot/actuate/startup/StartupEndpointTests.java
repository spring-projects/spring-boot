/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.startup;

import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StartupEndpoint}.
 *
 * @author Brian Clozel
 */
class StartupEndpointTests {

	@Test
	void startupEventsAreFound() {
		BufferingApplicationStartup applicationStartup = new BufferingApplicationStartup(256);
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withInitializer((context) -> context.setApplicationStartup(applicationStartup))
				.withUserConfiguration(EndpointConfiguration.class);
		contextRunner.run((context) -> {
			StartupEndpoint.StartupResponse startup = context.getBean(StartupEndpoint.class).startup();
			assertThat(startup.getSpringBootVersion()).isEqualTo(SpringBootVersion.getVersion());
			assertThat(startup.getTimeline().getStartTime())
					.isEqualTo(applicationStartup.getBufferedTimeline().getStartTime());
		});
	}

	@Test
	void bufferIsDrained() {
		BufferingApplicationStartup applicationStartup = new BufferingApplicationStartup(256);
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withInitializer((context) -> context.setApplicationStartup(applicationStartup))
				.withUserConfiguration(EndpointConfiguration.class);
		contextRunner.run((context) -> {
			StartupEndpoint.StartupResponse startup = context.getBean(StartupEndpoint.class).startup();
			assertThat(startup.getTimeline().getEvents()).isNotEmpty();
			assertThat(applicationStartup.getBufferedTimeline().getEvents()).isEmpty();
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class EndpointConfiguration {

		@Bean
		StartupEndpoint endpoint(BufferingApplicationStartup applicationStartup) {
			return new StartupEndpoint(applicationStartup);
		}

	}

}
