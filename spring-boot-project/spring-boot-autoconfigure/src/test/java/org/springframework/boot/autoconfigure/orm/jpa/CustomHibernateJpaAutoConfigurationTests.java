/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.orm.jpa;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.test.City;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Additional tests for {@link HibernateJpaAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 */
public class CustomHibernateJpaAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.datasource.generate-unique-name=true")
			.withUserConfiguration(TestConfiguration.class)
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
					HibernateJpaAutoConfiguration.class));

	@Test
	public void namingStrategyDelegatorTakesPrecedence() {
		this.contextRunner.withPropertyValues(
				"spring.jpa.properties.hibernate.ejb.naming_strategy_delegator:"
						+ "org.hibernate.cfg.naming.ImprovedNamingStrategyDelegator")
				.run((context) -> {
					JpaProperties jpaProperties = context.getBean(JpaProperties.class);
					HibernateProperties hibernateProperties = context
							.getBean(HibernateProperties.class);
					Map<String, Object> properties = hibernateProperties
							.determineHibernateProperties(jpaProperties.getProperties(),
									new HibernateSettings());
					assertThat(properties.get("hibernate.ejb.naming_strategy")).isNull();
				});
	}

	@Test
	public void namingStrategyBeansAreUsed() {
		this.contextRunner.withUserConfiguration(NamingStrategyConfiguration.class)
				.withPropertyValues(
						"spring.datasource.url:jdbc:h2:mem:naming-strategy-beans")
				.run((context) -> {
					HibernateJpaConfiguration jpaConfiguration = context
							.getBean(HibernateJpaConfiguration.class);
					Map<String, Object> hibernateProperties = jpaConfiguration
							.getVendorProperties();
					assertThat(hibernateProperties
							.get("hibernate.implicit_naming_strategy")).isEqualTo(
									NamingStrategyConfiguration.implicitNamingStrategy);
					assertThat(hibernateProperties
							.get("hibernate.physical_naming_strategy")).isEqualTo(
									NamingStrategyConfiguration.physicalNamingStrategy);
				});
	}

	@Test
	public void hibernatePropertiesCustomizersAreAppliedInOrder() {
		this.contextRunner
				.withUserConfiguration(HibernatePropertiesCustomizerConfiguration.class)
				.run((context) -> {
					HibernateJpaConfiguration jpaConfiguration = context
							.getBean(HibernateJpaConfiguration.class);
					Map<String, Object> hibernateProperties = jpaConfiguration
							.getVendorProperties();
					assertThat(hibernateProperties.get("test.counter")).isEqualTo(2);
				});
	}

	@Test
	public void defaultDatabaseForH2() {
		this.contextRunner.withPropertyValues("spring.datasource.url:jdbc:h2:mem:testdb",
				"spring.datasource.initialization-mode:never").run((context) -> {
					HibernateJpaVendorAdapter bean = context
							.getBean(HibernateJpaVendorAdapter.class);
					Database database = (Database) ReflectionTestUtils.getField(bean,
							"database");
					assertThat(database).isEqualTo(Database.H2);
				});
	}

	@Configuration
	@TestAutoConfigurationPackage(City.class)
	protected static class TestConfiguration {

	}

	@Configuration
	protected static class MockDataSourceConfiguration {

		@Bean
		public DataSource dataSource() {
			DataSource dataSource = mock(DataSource.class);
			try {
				given(dataSource.getConnection()).willReturn(mock(Connection.class));
				given(dataSource.getConnection().getMetaData())
						.willReturn(mock(DatabaseMetaData.class));
			}
			catch (SQLException ex) {
				// Do nothing
			}
			return dataSource;
		}

	}

	@Configuration
	static class NamingStrategyConfiguration {

		static final ImplicitNamingStrategy implicitNamingStrategy = new ImplicitNamingStrategyJpaCompliantImpl();

		static final PhysicalNamingStrategy physicalNamingStrategy = new PhysicalNamingStrategyStandardImpl();

		@Bean
		public ImplicitNamingStrategy implicitNamingStrategy() {
			return implicitNamingStrategy;
		}

		@Bean
		public PhysicalNamingStrategy physicalNamingStrategy() {
			return physicalNamingStrategy;
		}

	}

	@Configuration
	static class HibernatePropertiesCustomizerConfiguration {

		@Bean
		@Order(2)
		public HibernatePropertiesCustomizer sampleCustomizer() {
			return ((hibernateProperties) -> hibernateProperties.put("test.counter", 2));
		}

		@Bean
		@Order(1)
		public HibernatePropertiesCustomizer anotherCustomizer() {
			return ((hibernateProperties) -> hibernateProperties.put("test.counter", 1));
		}

	}

}
