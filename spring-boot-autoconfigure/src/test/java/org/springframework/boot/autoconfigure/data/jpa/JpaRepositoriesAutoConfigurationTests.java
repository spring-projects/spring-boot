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

package org.springframework.boot.autoconfigure.data.jpa;

import javax.persistence.EntityManagerFactory;

import org.junit.After;
import org.junit.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.alt.mongo.CityMongoDbRepository;
import org.springframework.boot.autoconfigure.data.alt.solr.CitySolrRepository;
import org.springframework.boot.autoconfigure.data.jpa.city.City;
import org.springframework.boot.autoconfigure.data.jpa.city.CityRepository;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JpaRepositoriesAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Oliver Gierke
 */
public class JpaRepositoriesAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		this.context.close();
	}

	@Test
	public void testDefaultRepositoryConfiguration() throws Exception {
		prepareApplicationContext(TestConfiguration.class);

		assertThat(this.context.getBean(CityRepository.class)).isNotNull();
		assertThat(this.context.getBean(PlatformTransactionManager.class)).isNotNull();
		assertThat(this.context.getBean(EntityManagerFactory.class)).isNotNull();
	}

	@Test
	public void testOverrideRepositoryConfiguration() throws Exception {
		prepareApplicationContext(CustomConfiguration.class);
		assertThat(this.context.getBean(
				org.springframework.boot.autoconfigure.data.alt.jpa.CityJpaRepository.class))
						.isNotNull();
		assertThat(this.context.getBean(PlatformTransactionManager.class)).isNotNull();
		assertThat(this.context.getBean(EntityManagerFactory.class)).isNotNull();
	}

	@Test(expected = NoSuchBeanDefinitionException.class)
	public void autoConfigurationShouldNotKickInEvenIfManualConfigDidNotCreateAnyRepositories() {
		prepareApplicationContext(SortOfInvalidCustomConfiguration.class);

		this.context.getBean(CityRepository.class);
	}

	private void prepareApplicationContext(Class<?>... configurationClasses) {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(configurationClasses);
		this.context.register(EmbeddedDataSourceConfiguration.class,
				HibernateJpaAutoConfiguration.class,
				JpaRepositoriesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
	}

	@Configuration
	@TestAutoConfigurationPackage(City.class)
	protected static class TestConfiguration {

	}

	@Configuration
	@EnableJpaRepositories(basePackageClasses = org.springframework.boot.autoconfigure.data.alt.jpa.CityJpaRepository.class, excludeFilters = {
			@Filter(type = FilterType.ASSIGNABLE_TYPE, value = CityMongoDbRepository.class),
			@Filter(type = FilterType.ASSIGNABLE_TYPE, value = CitySolrRepository.class) })
	@TestAutoConfigurationPackage(City.class)
	protected static class CustomConfiguration {

	}

	@Configuration
	// To not find any repositories
	@EnableJpaRepositories("foo.bar")
	@TestAutoConfigurationPackage(City.class)
	protected static class SortOfInvalidCustomConfiguration {

	}

}
