/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.liquibase.autoconfigure;

import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.liquibase.actuate.endpoint.LiquibaseEndpoint;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LiquibaseEndpointAutoConfiguration}.
 *
 * @author Phillip Webb
 */
class LiquibaseEndpointAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(LiquibaseEndpointAutoConfiguration.class));

	@Test
	void runShouldHaveEndpointBean() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=liquibase")
			.withBean(SpringLiquibase.class, () -> mock(SpringLiquibase.class))
			.run((context) -> assertThat(context).hasSingleBean(LiquibaseEndpoint.class));
	}

	@Test
	void runWhenEnabledPropertyIsFalseShouldNotHaveEndpointBean() {
		this.contextRunner.withBean(SpringLiquibase.class, () -> mock(SpringLiquibase.class))
			.withPropertyValues("management.endpoint.liquibase.enabled:false")
			.run((context) -> assertThat(context).doesNotHaveBean(LiquibaseEndpoint.class));
	}

	@Test
	void runWhenNotExposedShouldNotHaveEndpointBean() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(LiquibaseEndpoint.class));
	}

	@Test
	void disablesCloseOfDataSourceWhenEndpointIsEnabled() {
		this.contextRunner.withUserConfiguration(DataSourceClosingLiquibaseConfiguration.class)
			.withPropertyValues("management.endpoints.web.exposure.include=liquibase")
			.run((context) -> {
				assertThat(context).hasSingleBean(LiquibaseEndpoint.class);
				assertThat(context.getBean(DataSourceClosingSpringLiquibase.class))
					.hasFieldOrPropertyWithValue("closeDataSourceOnceMigrated", false);
			});
	}

	@Test
	void doesNotDisableCloseOfDataSourceWhenEndpointIsDisabled() {
		this.contextRunner.withUserConfiguration(DataSourceClosingLiquibaseConfiguration.class)
			.withPropertyValues("management.endpoint.liquibase.enabled:false")
			.run((context) -> {
				assertThat(context).doesNotHaveBean(LiquibaseEndpoint.class);
				DataSourceClosingSpringLiquibase bean = context.getBean(DataSourceClosingSpringLiquibase.class);
				assertThat(bean).hasFieldOrPropertyWithValue("closeDataSourceOnceMigrated", true);
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class DataSourceClosingLiquibaseConfiguration {

		@Bean
		SpringLiquibase liquibase() {
			return new DataSourceClosingSpringLiquibase() {

				private boolean propertiesSet;

				@Override
				public void setCloseDataSourceOnceMigrated(boolean closeDataSourceOnceMigrated) {
					if (this.propertiesSet) {
						throw new IllegalStateException(
								"setCloseDataSourceOnceMigrated invoked after afterPropertiesSet");
					}
					super.setCloseDataSourceOnceMigrated(closeDataSourceOnceMigrated);
				}

				@Override
				public void afterPropertiesSet() {
					this.propertiesSet = true;
				}

			};
		}

	}

}
