/*
 * Copyright 2012-2014 the original author or authors.
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
import java.util.Collections;

import javax.sql.DataSource;

import org.hibernate.cfg.ImprovedNamingStrategy;
import org.junit.Test;
import org.springframework.boot.orm.jpa.hibernate.SpringNamingStrategy;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import static org.jgroups.util.Util.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link EntityManagerFactoryBuilder}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
public class EntityManagerFactoryBuilderTests {

	private JpaProperties properties = new JpaProperties();

	@Test
	public void entityManagerFactoryPropertiesNotOverwritingDefaults() {
		EntityManagerFactoryBuilder factory = new EntityManagerFactoryBuilder(
				new HibernateJpaVendorAdapter(), this.properties, null);
		LocalContainerEntityManagerFactoryBean result1 = factory
				.dataSource(mockDataSource())
				.properties(Collections.singletonMap("foo", "spam")).build();
		assertFalse(result1.getJpaPropertyMap().isEmpty());
		assertTrue(this.properties.getProperties().isEmpty());
	}

	@Test
	public void multipleEntityManagerFactoriesDoNotOverwriteEachOther() {
		EntityManagerFactoryBuilder factory = new EntityManagerFactoryBuilder(
				new HibernateJpaVendorAdapter(), this.properties, null);
		LocalContainerEntityManagerFactoryBean result1 = factory
				.dataSource(mockDataSource())
				.properties(Collections.singletonMap("foo", "spam")).build();
		assertFalse(result1.getJpaPropertyMap().isEmpty());
		LocalContainerEntityManagerFactoryBean result2 = factory.dataSource(
				mockDataSource()).build();
		assertFalse(result2.getJpaPropertyMap().containsKey("foo"));
	}

	@Test
	public void entityManagerApplyHibernateCustomizationsByDefault() {
		EntityManagerFactoryBuilder factory = new EntityManagerFactoryBuilder(
				new HibernateJpaVendorAdapter(), this.properties, null);
		LocalContainerEntityManagerFactoryBean result = factory
				.dataSource(mockDataSource("H2"))
				.build();
		assertEquals(SpringNamingStrategy.class.getName(),
				result.getJpaPropertyMap().get("hibernate.ejb.naming_strategy"));
		assertEquals("create-drop", result.getJpaPropertyMap().get("hibernate.hbm2ddl.auto"));
	}

	@Test
	public void entityManagerUseCustomHibernateCustomizationsByDefault() {
		this.properties.getHibernate().setNamingStrategy(ImprovedNamingStrategy.class);
		this.properties.getHibernate().setDdlAuto("update");
		EntityManagerFactoryBuilder factory = new EntityManagerFactoryBuilder(
				new HibernateJpaVendorAdapter(), this.properties, null);
		LocalContainerEntityManagerFactoryBean result = factory
				.dataSource(mockDataSource("H2"))
				.build();
		assertEquals(ImprovedNamingStrategy.class.getName(),
				result.getJpaPropertyMap().get("hibernate.ejb.naming_strategy"));
		assertEquals("update", result.getJpaPropertyMap().get("hibernate.hbm2ddl.auto"));
	}

	@Test
	public void entityManagerHibernateCustomizationsDisabledIfPropertiesAreSet() {
		this.properties.getHibernate().setNamingStrategy(ImprovedNamingStrategy.class);
		EntityManagerFactoryBuilder factory = new EntityManagerFactoryBuilder(
				new HibernateJpaVendorAdapter(), this.properties, null);
		LocalContainerEntityManagerFactoryBean result = factory
				.dataSource(mockDataSource())
				.properties(Collections.singletonMap("foo", "spam"))
				.build();
		assertFalse(result.getJpaPropertyMap().containsKey("hibernate.ejb.naming_strategy"));
		assertFalse(result.getJpaPropertyMap().containsKey("hibernate.hbm2ddl.auto"));
	}


	private DataSource mockDataSource() {
		return mockDataSource("Test");
	}

	private DataSource mockDataSource(String productName) {
		try {
			DataSource mock = mock(DataSource.class);
			Connection connection = mock(Connection.class);
			DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
			given(databaseMetaData.getDatabaseProductName()).willReturn(productName);
			given(connection.getMetaData()).willReturn(databaseMetaData);
			given(mock.getConnection()).willReturn(connection);
			return mock;
		}
		catch (SQLException e) {
			throw new IllegalStateException(e);
		}
	}

}
