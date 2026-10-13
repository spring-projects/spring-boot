/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.flyway.actuate.endpoint;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.flyway.actuate.endpoint.FlywayEndpoint.ContextFlywayBeansDescriptor;
import org.springframework.boot.flyway.actuate.endpoint.FlywayEndpoint.FlywayDescriptor;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
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
			ContextFlywayBeansDescriptor descriptor = context.getBean(FlywayEndpoint.class)
				.flywayBeans()
				.getContexts()
				.get(context.getId());
			assertThat(descriptor).isNotNull();
			Map<String, FlywayDescriptor> flywayBeans = descriptor.getFlywayBeans();
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
				ContextFlywayBeansDescriptor descriptor = context.getBean(FlywayEndpoint.class)
					.flywayBeans()
					.getContexts()
					.get(context.getId());
				assertThat(descriptor).isNotNull();
				Map<String, FlywayDescriptor> flywayBeans = descriptor.getFlywayBeans();
				assertThat(flywayBeans).hasSize(1);
				assertThat(flywayBeans.values().iterator().next().getMigrations()).hasSize(4);
			});
	}

}
