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

package org.springframework.boot.autoconfigure.flyway;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.migration.JavaMigration;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.ClassPathOverrides;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FlywayAutoConfiguration} with Flyway 5.x.
 *
 * @author Andy Wilkinson
 */
@ClassPathOverrides("org.flywaydb:flyway-core:5.2.4")
class Flyway5xAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(FlywayAutoConfiguration.class))
			.withPropertyValues("spring.datasource.generate-unique-name=true");

	@Test
	void defaultFlyway() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(Flyway.class);
			Flyway flyway = context.getBean(Flyway.class);
			assertThat(flyway.getConfiguration().getLocations())
					.containsExactly(new Location("classpath:db/migration"));
		});
	}

	@Test
	void flywayJavaMigrationsAreIgnored() {
		this.contextRunner
				.withUserConfiguration(EmbeddedDataSourceConfiguration.class, FlywayJavaMigrationsConfiguration.class)
				.run((context) -> assertThat(context).hasNotFailed());
	}

	@Configuration(proxyBeanMethods = false)
	static class FlywayJavaMigrationsConfiguration {

		@Bean
		TestMigration migration1() {
			return new TestMigration("2", "M1");
		}

		@Bean
		TestMigration migration2() {
			return new TestMigration("3", "M2");
		}

	}

	private static final class TestMigration implements JavaMigration {

		private final MigrationVersion version;

		private final String description;

		private TestMigration(String version, String description) {
			this.version = MigrationVersion.fromVersion(version);
			this.description = description;
		}

		@Override
		public MigrationVersion getVersion() {
			return this.version;
		}

		@Override
		public String getDescription() {
			return this.description;
		}

		@Override
		public Integer getChecksum() {
			return 1;
		}

		@Override
		public boolean isUndo() {
			return false;
		}

		@Override
		public boolean canExecuteInTransaction() {
			return true;
		}

		@Override
		public void migrate(org.flywaydb.core.api.migration.Context context) {

		}

	}

}
