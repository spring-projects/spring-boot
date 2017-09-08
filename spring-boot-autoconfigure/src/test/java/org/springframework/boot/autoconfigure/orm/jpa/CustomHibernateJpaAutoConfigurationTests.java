/*
 * Copyright 2012-2017 the original author or authors.
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

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.test.City;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HibernateJpaAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Eddú Meléndez
 */
public class CustomHibernateJpaAutoConfigurationTests {

	protected AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@After
	public void close() {
		this.context.close();
	}

	@Test
	public void testNamingStrategyDelegatorTakesPrecedence() {
		TestPropertyValues
				.of("spring.jpa.properties.hibernate.ejb.naming_strategy_delegator:"
						+ "org.hibernate.cfg.naming.ImprovedNamingStrategyDelegator");
		this.context.register(TestConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				HibernateJpaAutoConfiguration.class);
		this.context.refresh();
		JpaProperties bean = this.context.getBean(JpaProperties.class);
		Map<String, String> hibernateProperties = bean
				.getHibernateProperties("create-drop");
		assertThat(hibernateProperties.get("hibernate.ejb.naming_strategy")).isNull();
	}

	@Test
	public void testDefaultDatabaseForH2() throws Exception {
		TestPropertyValues.of("spring.datasource.url:jdbc:h2:mem:testdb",
				"spring.datasource.initialize:false").applyTo(this.context);
		this.context.register(TestConfiguration.class, DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				HibernateJpaAutoConfiguration.class);
		this.context.refresh();
		HibernateJpaVendorAdapter bean = this.context
				.getBean(HibernateJpaVendorAdapter.class);
		Database database = (Database) ReflectionTestUtils.getField(bean, "database");
		assertThat(database).isEqualTo(Database.H2);
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
			catch (SQLException e) {
				// Do nothing
			}
			return dataSource;
		}

	}

}
