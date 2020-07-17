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

package org.springframework.boot.autoconfigure.neo4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.driver.Driver;
import org.neo4j.driver.exceptions.ClientException;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.when;

/**
 * @author Michael J. Simons
 */
class Neo4jAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(Neo4jAutoConfiguration.class));

	@Test
	void shouldRequireAllNeededClasses() {

		this.contextRunner.withPropertyValues("spring.neo4j.uri=bolt://localhost:4711")
				.withClassLoader(new FilteredClassLoader(Driver.class))
				.run((ctx) -> assertThat(ctx).doesNotHaveBean(Driver.class));
	}

	@Test
	void shouldNotRequireUri() {

		this.contextRunner.run((ctx) -> assertThat(ctx).hasSingleBean(Driver.class));
	}

	@Test
	void shouldCreateDriver() {

		this.contextRunner.withPropertyValues("spring.neo4j.uri=bolt://localhost:4711")
				.run((ctx) -> assertThat(ctx).hasSingleBean(Driver.class));
	}

	/**
	 * These tests assert correct configuration behaviour for cases in which one of the
	 * "advanced" schemes is used to configure the driver. If any of the schemes is used,
	 * than a contradicting explicit configuration will throw an error.
	 * @param scheme The scheme to test.
	 */
	@ParameterizedTest
	@ValueSource(strings = { "bolt+s", "bolt+ssc", "neo4j+s", "neo4j+ssc" })
	void schemesShouldBeApplied(String scheme) {

		this.contextRunner.withPropertyValues("spring.neo4j.uri=" + scheme + "://localhost:4711").run((ctx) -> {
			assertThat(ctx).hasSingleBean(Driver.class);

			Driver driver = ctx.getBean(Driver.class);
			assertThat(driver.isEncrypted()).isTrue();
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class WithDriver {

		@Bean
		Driver driver() {
			Driver driver = mock(Driver.class);
			when(driver.metrics()).thenThrow(ClientException.class);
			return driver;
		}

	}

}
