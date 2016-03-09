/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.orm.jpa;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Test;

import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link EntityManagerFactoryBuilder}.
 *
 * @author Dave Syer
 */
@Deprecated
public class EntityManagerFactoryBuilderTests {

	private Map<String, Object> properties = new LinkedHashMap<String, Object>();

	private DataSource dataSource1 = mock(DataSource.class);

	private DataSource dataSource2 = mock(DataSource.class);

	@Test
	public void entityManagerFactoryPropertiesNotOverwritingDefaults() {
		EntityManagerFactoryBuilder factory = new EntityManagerFactoryBuilder(
				new HibernateJpaVendorAdapter(), this.properties, null);
		LocalContainerEntityManagerFactoryBean result1 = factory
				.dataSource(this.dataSource1)
				.properties(Collections.singletonMap("foo", "spam")).build();
		assertThat(result1.getJpaPropertyMap().isEmpty()).isFalse();
		assertThat(this.properties.isEmpty()).isTrue();
	}

	@Test
	public void multipleEntityManagerFactoriesDoNotOverwriteEachOther() {
		EntityManagerFactoryBuilder factory = new EntityManagerFactoryBuilder(
				new HibernateJpaVendorAdapter(), this.properties, null);
		LocalContainerEntityManagerFactoryBean result1 = factory
				.dataSource(this.dataSource1)
				.properties(Collections.singletonMap("foo", "spam")).build();
		assertThat(result1.getJpaPropertyMap().isEmpty()).isFalse();
		LocalContainerEntityManagerFactoryBean result2 = factory
				.dataSource(this.dataSource2).build();
		assertThat(result2.getJpaPropertyMap().isEmpty()).isTrue();
	}

}
