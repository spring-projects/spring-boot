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

package org.springframework.boot.autoconfigure.r2dbc;

import java.util.UUID;

import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Wrapped;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.r2dbc.OptionsCapableConnectionFactory;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link R2dbcAutoConfiguration} without the {@code io.r2dbc:r2dbc-pool}
 * dependency.
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions("r2dbc-pool-*.jar")
class R2dbcAutoConfigurationWithoutConnectionPoolTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class));

	@Test
	void configureWithoutR2dbcPoolCreateGenericConnectionFactory() {
		this.contextRunner
			.withPropertyValues("spring.r2dbc.url:r2dbc:h2:mem:///" + randomDatabaseName()
					+ "?options=DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
			.run((context) -> {
				assertThat(context).hasSingleBean(ConnectionFactory.class);
				assertThat(context.getBean(ConnectionFactory.class))
					.asInstanceOf(type(OptionsCapableConnectionFactory.class))
					.extracting(Wrapped<ConnectionFactory>::unwrap)
					.isExactlyInstanceOf(H2ConnectionFactory.class);
			});
	}

	@Test
	void configureWithoutR2dbcPoolAndPoolEnabledShouldFail() {
		this.contextRunner
			.withPropertyValues("spring.r2dbc.pool.enabled=true",
					"spring.r2dbc.url:r2dbc:h2:mem:///" + randomDatabaseName()
							+ "?options=DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
			.run((context) -> assertThat(context).getFailure()
				.rootCause()
				.isInstanceOf(MissingR2dbcPoolDependencyException.class));
	}

	@Test
	void configureWithoutR2dbcPoolAndPoolUrlShouldFail() {
		this.contextRunner
			.withPropertyValues("spring.r2dbc.url:r2dbc:pool:h2:mem:///" + randomDatabaseName()
					+ "?options=DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
			.run((context) -> assertThat(context).getFailure()
				.rootCause()
				.isInstanceOf(MissingR2dbcPoolDependencyException.class));
	}

	private <T> InstanceOfAssertFactory<T, ObjectAssert<T>> type(Class<T> type) {
		return InstanceOfAssertFactories.type(type);
	}

	private String randomDatabaseName() {
		return "testdb-" + UUID.randomUUID();
	}

}
