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

package org.springframework.boot.flyway.endpoint;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.flyway.endpoint.FlywayEndpoint.FlywayDescriptor;
import org.springframework.boot.jdbc.autoconfigure.EmbeddedDataSourceConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.resources.WithResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FlywayEndpoint}.
 *
 * @author Eddú Meléndez
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@WithResource(name = "db/migration/V1__init.sql", content = "DROP TABLE IF EXISTS TEST;")
@WithResource(name = "db/migration/V2__update.sql", content = "DROP TABLE IF EXISTS TEST;")
@WithResource(name = "db/migration/V3__update.sql", content = "DROP TABLE IF EXISTS TEST;")
class FlywayEndpointTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(FlywayAutoConfiguration.class))
		.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
		.withBean("endpoint", FlywayEndpoint.class);

	@Test
	void flywayReportIsProduced() {
		this.contextRunner.run((context) -> {
			Map<String, FlywayDescriptor> flywayBeans = context.getBean(FlywayEndpoint.class)
				.flywayBeans()
				.getContexts()
				.get(context.getId())
				.getFlywayBeans();
			assertThat(flywayBeans).hasSize(1);
			assertThat(flywayBeans.values().iterator().next().getMigrations()).hasSize(3);
		});
	}

	@Test
	void whenFlywayHasBeenBaselinedFlywayReportIsProduced() {
		this.contextRunner.withPropertyValues("spring.flyway.baseline-version=2")
			.withBean(FlywayMigrationStrategy.class, () -> (flyway) -> {
				flyway.baseline();
				flyway.migrate();
			})
			.run((context) -> {
				Map<String, FlywayDescriptor> flywayBeans = context.getBean(FlywayEndpoint.class)
					.flywayBeans()
					.getContexts()
					.get(context.getId())
					.getFlywayBeans();
				assertThat(flywayBeans).hasSize(1);
				assertThat(flywayBeans.values().iterator().next().getMigrations()).hasSize(4);
			});
	}

}
