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

package org.springframework.boot.autoconfigure.neo4j;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.v1.Driver;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
class Neo4jDriverAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(Neo4jDriverAutoConfiguration.class));

	@Test
	void shouldRequireAllNeededClasses() {

		this.contextRunner.withPropertyValues(Neo4jDriverProperties.PREFIX + ".uri=bolt://localhost:4711")
				.withClassLoader(new FilteredClassLoader(Driver.class))
				.run((ctx) -> assertThat(ctx).doesNotHaveBean(Driver.class));
	}

	@Test
	void shouldRequireUri() {

		this.contextRunner.run((ctx) -> assertThat(ctx).doesNotHaveBean(Driver.class));
	}

	@Test
	void shouldCreateDriver() {

		this.contextRunner.withPropertyValues(Neo4jDriverProperties.PREFIX + ".uri=bolt://localhost:4711")
				.run((ctx) -> assertThat(ctx).hasSingleBean(Driver.class));
	}

}
