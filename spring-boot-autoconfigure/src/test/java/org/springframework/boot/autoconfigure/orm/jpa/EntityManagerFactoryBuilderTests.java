/*
 * Copyright 2012-2013 the original author or authors.
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

import java.util.Collections;

import javax.sql.DataSource;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Dave Syer
 */
public class EntityManagerFactoryBuilderTests {

	private JpaProperties properties = new JpaProperties();

	private DataSource dataSource1 = Mockito.mock(DataSource.class);

	private DataSource dataSource2 = Mockito.mock(DataSource.class);

	@Test
	public void entityManagerFactoryPropertiesNotOverwritingDefaults() {
		EntityManagerFactoryBuilder factory = new EntityManagerFactoryBuilder(
				new HibernateJpaVendorAdapter(), this.properties, null);
		LocalContainerEntityManagerFactoryBean result1 = factory
				.dataSource(this.dataSource1)
				.properties(Collections.singletonMap("foo", (Object) "spam")).build();
		assertFalse(result1.getJpaPropertyMap().isEmpty());
		assertTrue(this.properties.getProperties().isEmpty());
	}

	@Test
	public void multipleEntityManagerFactoriesDoNotOverwriteEachOther() {
		EntityManagerFactoryBuilder factory = new EntityManagerFactoryBuilder(
				new HibernateJpaVendorAdapter(), this.properties, null);
		LocalContainerEntityManagerFactoryBean result1 = factory
				.dataSource(this.dataSource1)
				.properties(Collections.singletonMap("foo", (Object) "spam")).build();
		assertFalse(result1.getJpaPropertyMap().isEmpty());
		LocalContainerEntityManagerFactoryBean result2 = factory.dataSource(
				this.dataSource2).build();
		assertTrue(result2.getJpaPropertyMap().isEmpty());
	}

}
