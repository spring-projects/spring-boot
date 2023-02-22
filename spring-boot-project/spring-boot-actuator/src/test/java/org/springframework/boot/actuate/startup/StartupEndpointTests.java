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

package org.springframework.boot.actuate.startup;

import java.util.Set;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.actuate.startup.StartupEndpoint.StartupDescriptor;
import org.springframework.boot.actuate.startup.StartupEndpoint.StartupEndpointRuntimeHints;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.metrics.ApplicationStartup;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StartupEndpoint}.
 *
 * @author Brian Clozel
 * @author Chris Bono
 * @author Moritz Halbritter
 */
class StartupEndpointTests {

	@Test
	void startupEventsAreFound() {
		BufferingApplicationStartup applicationStartup = new BufferingApplicationStartup(256);
		testStartupEndpoint(applicationStartup, (startupEndpoint) -> {
			StartupDescriptor startup = startupEndpoint.startup();
			assertThat(startup.getSpringBootVersion()).isEqualTo(SpringBootVersion.getVersion());
			assertThat(startup.getTimeline().getStartTime())
				.isEqualTo(applicationStartup.getBufferedTimeline().getStartTime());
		});
	}

	@Test
	void bufferWithGetIsNotDrained() {
		BufferingApplicationStartup applicationStartup = new BufferingApplicationStartup(256);
		testStartupEndpoint(applicationStartup, (startupEndpoint) -> {
			StartupDescriptor startup = startupEndpoint.startupSnapshot();
			assertThat(startup.getTimeline().getEvents()).isNotEmpty();
			assertThat(applicationStartup.getBufferedTimeline().getEvents()).isNotEmpty();
		});
	}

	@Test
	void bufferWithPostIsDrained() {
		BufferingApplicationStartup applicationStartup = new BufferingApplicationStartup(256);
		testStartupEndpoint(applicationStartup, (startupEndpoint) -> {
			StartupDescriptor startup = startupEndpoint.startup();
			assertThat(startup.getTimeline().getEvents()).isNotEmpty();
			assertThat(applicationStartup.getBufferedTimeline().getEvents()).isEmpty();
		});
	}

	@Test
	void shouldRegisterHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		new StartupEndpointRuntimeHints().registerHints(runtimeHints, getClass().getClassLoader());
		Set<TypeReference> bindingTypes = Set.of(
				TypeReference.of("org.springframework.boot.context.metrics.buffering.BufferedStartupStep$DefaultTag"),
				TypeReference.of("org.springframework.core.metrics.jfr.FlightRecorderStartupStep$FlightRecorderTag"));
		for (TypeReference bindingType : bindingTypes) {
			assertThat(RuntimeHintsPredicates.reflection()
				.onType(bindingType)
				.withMemberCategories(MemberCategory.INVOKE_PUBLIC_METHODS)).accepts(runtimeHints);
		}
	}

	private void testStartupEndpoint(ApplicationStartup applicationStartup, Consumer<StartupEndpoint> startupEndpoint) {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withInitializer((context) -> context.setApplicationStartup(applicationStartup))
			.withUserConfiguration(EndpointConfiguration.class);
		contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(StartupEndpoint.class);
			startupEndpoint.accept(context.getBean(StartupEndpoint.class));
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
