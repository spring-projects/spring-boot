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

package org.springframework.boot.autoconfigure.data.neo4j;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.neo4j.ogm.drivers.embedded.driver.EmbeddedDriver;

import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.city.City;
import org.springframework.boot.autoconfigure.data.jpa.city.CityRepository;
import org.springframework.boot.autoconfigure.data.neo4j.country.Country;
import org.springframework.boot.autoconfigure.data.neo4j.country.CountryRepository;
import org.springframework.boot.autoconfigure.data.neo4j.empty.EmptyMarker;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Neo4jRepositoriesAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Oliver Gierke
 * @author Michael Hunger
 * @author Vince Bickers
 * @author Stephane Nicoll
 */
class MixedNeo4jRepositoriesAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@AfterEach
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void testDefaultRepositoryConfiguration() {
		load(TestConfiguration.class);
		assertThat(this.context.getBean(CountryRepository.class)).isNotNull();
	}

	@Test
	void testMixedRepositoryConfiguration() {
		load(MixedConfiguration.class);
		assertThat(this.context.getBean(CountryRepository.class)).isNotNull();
		assertThat(this.context.getBean(CityRepository.class)).isNotNull();
	}

	@Test
	void testJpaRepositoryConfigurationWithNeo4jTemplate() {
		load(JpaConfiguration.class);
		assertThat(this.context.getBean(CityRepository.class)).isNotNull();
	}

	@Test
	@Disabled
	void testJpaRepositoryConfigurationWithNeo4jOverlap() {
		load(OverlapConfiguration.class);
		assertThat(this.context.getBean(CityRepository.class)).isNotNull();
	}

	@Test
	void testJpaRepositoryConfigurationWithNeo4jOverlapDisabled() {
		load(OverlapConfiguration.class, "spring.data.neo4j.repositories.enabled:false");
		assertThat(this.context.getBean(CityRepository.class)).isNotNull();
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.setClassLoader(new FilteredClassLoader(EmbeddedDriver.class));
		TestPropertyValues.of(environment).and("spring.datasource.initialization-mode=never").applyTo(context);
		context.register(config);
		context.register(DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
				JpaRepositoriesAutoConfiguration.class, Neo4jDataAutoConfiguration.class,
				Neo4jRepositoriesAutoConfiguration.class);
		context.refresh();
		this.context = context;
	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(EmptyMarker.class)
	// Not this package or its parent
	@EnableNeo4jRepositories(basePackageClasses = Country.class)
	protected static class TestConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(EmptyMarker.class)
	@EnableNeo4jRepositories(basePackageClasses = Country.class)
	@EntityScan(basePackageClasses = City.class)
	@EnableJpaRepositories(basePackageClasses = CityRepository.class)
	protected static class MixedConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(EmptyMarker.class)
	@EntityScan(basePackageClasses = City.class)
	@EnableJpaRepositories(basePackageClasses = CityRepository.class)
	protected static class JpaConfiguration {

	}

	// In this one the Jpa repositories and the auto-configuration packages overlap, so
	// Neo4j will try and configure the same repositories
	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(CityRepository.class)
	@EnableJpaRepositories(basePackageClasses = CityRepository.class)
	protected static class OverlapConfiguration {

	}

}
