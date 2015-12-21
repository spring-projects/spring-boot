/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.data.alt.cassandra.CityCassandraRepository;
import org.springframework.boot.autoconfigure.data.cassandra.city.City;
import org.springframework.boot.autoconfigure.data.cassandra.city.CityRepository;
import org.springframework.boot.autoconfigure.data.empty.EmptyDataPackage;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CassandraRepositoriesAutoConfiguration}.
 *
 * @author Eddú Meléndez
 */
public class CassandraRepositoriesAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@Before
	public void setUp() {
		this.context = new AnnotationConfigApplicationContext();
	}

	@After
	public void close() {
		this.context.close();
	}

	@Test
	public void testDefaultRepositoryConfiguration() {
		addConfigurations(TestConfiguration.class);
		assertNotNull(this.context.getBean(CityRepository.class));
		assertNotNull(this.context.getBean(Cluster.class));
	}

	@Test
	public void testNoRepositoryConfiguration() {
		addConfigurations(TestExcludeConfiguration.class, EmptyConfiguration.class);
		assertNotNull(this.context.getBean(Cluster.class));
	}

	@Test
	public void doesNotTriggerDefaultRepositoryDetectionIfCustomized() {
		addConfigurations(TestExcludeConfiguration.class, CustomizedConfiguration.class);
		assertNotNull(this.context.getBean(CityCassandraRepository.class));
	}

	private void addConfigurations(Class<?>... configurations) {
		this.context.register(configurations);
		this.context.register(CassandraAutoConfiguration.class,
				CassandraRepositoriesAutoConfiguration.class,
				CassandraDataAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
	}

	@Configuration
	@TestAutoConfigurationPackage(City.class)
	static class TestConfiguration {

		@Bean
		public Session session() {
			return mock(Session.class);
		}

	}

	@Configuration
	@TestAutoConfigurationPackage(EmptyDataPackage.class)
	static class EmptyConfiguration {

	}

	@Configuration
	@TestAutoConfigurationPackage(CassandraRepositoriesAutoConfigurationTests.class)
	@EnableCassandraRepositories(basePackageClasses = CityCassandraRepository.class)
	static class CustomizedConfiguration {

	}

	@Configuration
	@ComponentScan(excludeFilters = @ComponentScan.Filter(classes = {
			Session.class }, type = FilterType.ASSIGNABLE_TYPE))
	static class TestExcludeConfiguration {

	}

}
