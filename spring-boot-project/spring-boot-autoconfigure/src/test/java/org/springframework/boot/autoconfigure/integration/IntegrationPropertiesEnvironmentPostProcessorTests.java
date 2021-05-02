/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.integration;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.boot.origin.TextResourceOrigin;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link IntegrationPropertiesEnvironmentPostProcessor}.
 *
 * @author Stephane Nicoll
 */
class IntegrationPropertiesEnvironmentPostProcessorTests {

	@Test
	void postProcessEnvironmentAddPropertySource() {
		ConfigurableEnvironment environment = new StandardEnvironment();
		new IntegrationPropertiesEnvironmentPostProcessor().postProcessEnvironment(environment,
				mock(SpringApplication.class));
		assertThat(environment.getPropertySources().contains("META-INF/spring.integration.properties")).isTrue();
		assertThat(environment.getProperty("spring.integration.endpoint.no-auto-startup")).isEqualTo("testService*");
	}

	@Test
	void postProcessEnvironmentAddPropertySourceLast() {
		ConfigurableEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addLast(new MapPropertySource("test",
				Collections.singletonMap("spring.integration.endpoint.no-auto-startup", "another*")));
		new IntegrationPropertiesEnvironmentPostProcessor().postProcessEnvironment(environment,
				mock(SpringApplication.class));
		assertThat(environment.getPropertySources().contains("META-INF/spring.integration.properties")).isTrue();
		assertThat(environment.getProperty("spring.integration.endpoint.no-auto-startup")).isEqualTo("another*");
	}

	@Test
	void registerIntegrationPropertiesPropertySourceWithUnknownResourceThrowsException() {
		ConfigurableEnvironment environment = new StandardEnvironment();
		ClassPathResource unknown = new ClassPathResource("does-not-exist.properties", getClass());
		assertThatThrownBy(() -> new IntegrationPropertiesEnvironmentPostProcessor()
				.registerIntegrationPropertiesPropertySource(environment, unknown))
						.isInstanceOf(IllegalStateException.class).hasCauseInstanceOf(FileNotFoundException.class)
						.hasMessageContaining(unknown.toString());
	}

	@Test
	void registerIntegrationPropertiesPropertySourceWithResourceAddPropertySource() {
		ConfigurableEnvironment environment = new StandardEnvironment();
		new IntegrationPropertiesEnvironmentPostProcessor().registerIntegrationPropertiesPropertySource(environment,
				new ClassPathResource("spring.integration.properties", getClass()));
		assertThat(environment.getProperty("spring.integration.channel.auto-create", Boolean.class)).isFalse();
		assertThat(environment.getProperty("spring.integration.channel.max-unicast-subscribers", Integer.class))
				.isEqualTo(4);
		assertThat(environment.getProperty("spring.integration.channel.max-broadcast-subscribers", Integer.class))
				.isEqualTo(6);
		assertThat(environment.getProperty("spring.integration.error.require-subscribers", Boolean.class)).isFalse();
		assertThat(environment.getProperty("spring.integration.error.ignore-failures", Boolean.class)).isFalse();
		assertThat(environment.getProperty("spring.integration.endpoint.throw-exception-on-late-reply", Boolean.class))
				.isTrue();
		assertThat(environment.getProperty("spring.integration.endpoint.read-only-headers", String.class))
				.isEqualTo("header1,header2");
		assertThat(environment.getProperty("spring.integration.endpoint.no-auto-startup", String.class))
				.isEqualTo("testService,anotherService");
	}

	@Test
	@SuppressWarnings("unchecked")
	void registerIntegrationPropertiesPropertySourceWithResourceCanRetrieveOrigin() {
		ConfigurableEnvironment environment = new StandardEnvironment();
		ClassPathResource resource = new ClassPathResource("spring.integration.properties", getClass());
		new IntegrationPropertiesEnvironmentPostProcessor().registerIntegrationPropertiesPropertySource(environment,
				resource);
		PropertySource<?> ps = environment.getPropertySources().get("META-INF/spring.integration.properties");
		assertThat(ps).isNotNull().isInstanceOf(OriginLookup.class);
		OriginLookup<String> originLookup = (OriginLookup<String>) ps;
		assertThat(originLookup.getOrigin("spring.integration.channel.auto-create"))
				.satisfies(textOrigin(resource, 0, 39));
		assertThat(originLookup.getOrigin("spring.integration.channel.max-unicast-subscribers"))
				.satisfies(textOrigin(resource, 1, 50));
		assertThat(originLookup.getOrigin("spring.integration.channel.max-broadcast-subscribers"))
				.satisfies(textOrigin(resource, 2, 52));
	}

	private Consumer<Origin> textOrigin(Resource resource, int line, int column) {
		return (origin) -> {
			assertThat(origin).isInstanceOf(TextResourceOrigin.class);
			TextResourceOrigin textOrigin = (TextResourceOrigin) origin;
			assertThat(textOrigin.getResource()).isEqualTo(resource);
			assertThat(textOrigin.getLocation().getLine()).isEqualTo(line);
			assertThat(textOrigin.getLocation().getColumn()).isEqualTo(column);
		};
	}

}
