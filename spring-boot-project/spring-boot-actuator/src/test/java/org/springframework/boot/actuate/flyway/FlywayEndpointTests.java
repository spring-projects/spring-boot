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

package org.springframework.boot.actuate.flyway;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.flyway.FlywayEndpoint.FlywayDescriptor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FlywayEndpoint}.
 *
 * @author Eddú Meléndez
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class FlywayEndpointTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(FlywayAutoConfiguration.class))
			.withUserConfiguration(EmbeddedDataSourceConfiguration.class).withBean("endpoint", FlywayEndpoint.class);

	@Test
	void flywayReportIsProduced() {
		this.contextRunner.run((context) -> {
			Map<String, FlywayDescriptor> flywayBeans = context.getBean(FlywayEndpoint.class).flywayBeans()
					.getContexts().get(context.getId()).getFlywayBeans();
			assertThat(flywayBeans).hasSize(1);
			assertThat(flywayBeans.values().iterator().next().getMigrations()).hasSize(3);
		});
	}

	@Test
	@SuppressWarnings("deprecation")
	void whenFlywayHasBeenBaselinedFlywayReportIsProduced() {
		this.contextRunner.withBean(FlywayMigrationStrategy.class, () -> (flyway) -> {
			flyway.setBaselineVersionAsString("2");
			flyway.baseline();
			flyway.migrate();
		}).run((context) -> {
			Map<String, FlywayDescriptor> flywayBeans = context.getBean(FlywayEndpoint.class).flywayBeans()
					.getContexts().get(context.getId()).getFlywayBeans();
			assertThat(flywayBeans).hasSize(1);
			assertThat(flywayBeans.values().iterator().next().getMigrations()).hasSize(3);
		});
	}

}
