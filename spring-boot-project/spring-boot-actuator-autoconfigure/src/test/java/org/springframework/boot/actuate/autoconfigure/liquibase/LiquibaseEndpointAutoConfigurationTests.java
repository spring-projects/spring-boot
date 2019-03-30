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

package org.springframework.boot.actuate.autoconfigure.liquibase;

import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.Test;

import org.springframework.boot.actuate.liquibase.LiquibaseEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.liquibase.DataSourceClosingSpringLiquibase;
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
public class LiquibaseEndpointAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(LiquibaseEndpointAutoConfiguration.class));

	@Test
	public void runShouldHaveEndpointBean() {
		this.contextRunner
				.withPropertyValues("management.endpoints.web.exposure.include=liquibase")
				.withUserConfiguration(LiquibaseConfiguration.class)
				.run((context) -> assertThat(context)
						.hasSingleBean(LiquibaseEndpoint.class));
	}

	@Test
	public void runWhenEnabledPropertyIsFalseShouldNotHaveEndpointBean() {
		this.contextRunner.withUserConfiguration(LiquibaseConfiguration.class)
				.withPropertyValues("management.endpoint.liquibase.enabled:false")
				.run((context) -> assertThat(context)
						.doesNotHaveBean(LiquibaseEndpoint.class));
	}

	@Test
	public void runWhenNotExposedShouldNotHaveEndpointBean() {
		this.contextRunner.run((context) -> assertThat(context)
				.doesNotHaveBean(LiquibaseEndpoint.class));
	}

	@Test
	public void disablesCloseOfDataSourceWhenEndpointIsEnabled() {
		this.contextRunner
				.withUserConfiguration(DataSourceClosingLiquibaseConfiguration.class)
				.withPropertyValues("management.endpoints.web.exposure.include=liquibase")
				.run((context) -> {
					assertThat(context).hasSingleBean(LiquibaseEndpoint.class);
					assertThat(context.getBean(DataSourceClosingSpringLiquibase.class))
							.hasFieldOrPropertyWithValue("closeDataSourceOnceMigrated",
									false);
				});
	}

	@Test
	public void doesNotDisableCloseOfDataSourceWhenEndpointIsDisabled() {
		this.contextRunner
				.withUserConfiguration(DataSourceClosingLiquibaseConfiguration.class)
				.withPropertyValues("management.endpoint.liquibase.enabled:false")
				.run((context) -> {
					assertThat(context).doesNotHaveBean(LiquibaseEndpoint.class);
					DataSourceClosingSpringLiquibase bean = context
							.getBean(DataSourceClosingSpringLiquibase.class);
					assertThat(bean).hasFieldOrPropertyWithValue(
							"closeDataSourceOnceMigrated", true);
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class LiquibaseConfiguration {

		@Bean
		public SpringLiquibase liquibase() {
			return mock(SpringLiquibase.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DataSourceClosingLiquibaseConfiguration {

		@Bean
		public SpringLiquibase liquibase() {
			return new DataSourceClosingSpringLiquibase() {

				private boolean propertiesSet = false;

				@Override
				public void setCloseDataSourceOnceMigrated(
						boolean closeDataSourceOnceMigrated) {
					if (this.propertiesSet) {
						throw new IllegalStateException("setCloseDataSourceOnceMigrated "
								+ "invoked after afterPropertiesSet");
					}
					super.setCloseDataSourceOnceMigrated(closeDataSourceOnceMigrated);
				}

				@Override
				public void afterPropertiesSet() throws LiquibaseException {
					this.propertiesSet = true;
				}

			};
		}

	}

}
