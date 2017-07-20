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

package org.springframework.boot.autoconfigure.data.cassandra;

import java.util.Set;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import org.junit.After;
import org.junit.Test;

import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.alt.cassandra.ReactiveCityCassandraRepository;
import org.springframework.boot.autoconfigure.data.cassandra.city.City;
import org.springframework.boot.autoconfigure.data.cassandra.city.ReactiveCityRepository;
import org.springframework.boot.autoconfigure.data.empty.EmptyDataPackage;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.cassandra.ReactiveSession;
import org.springframework.data.cassandra.core.mapping.CassandraMappingContext;
import org.springframework.data.cassandra.repository.config.EnableReactiveCassandraRepositories;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CassandraReactiveRepositoriesAutoConfiguration}.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Mark Paluch
 */
public class CassandraReactiveRepositoriesAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testDefaultRepositoryConfiguration() {
		load(TestConfiguration.class);
		assertThat(this.context.getBean(ReactiveCityRepository.class)).isNotNull();
		assertThat(this.context.getBean(Cluster.class)).isNotNull();
		assertThat(getInitialEntitySet()).hasSize(1);
	}

	@Test
	public void testNoRepositoryConfiguration() {
		load(TestExcludeConfiguration.class, EmptyConfiguration.class);
		assertThat(this.context.getBean(Cluster.class)).isNotNull();
		assertThat(getInitialEntitySet()).hasSize(1).containsOnly(City.class);
	}

	@Test
	public void doesNotTriggerDefaultRepositoryDetectionIfCustomized() {
		load(TestExcludeConfiguration.class, CustomizedConfiguration.class);
		assertThat(this.context.getBean(ReactiveCityCassandraRepository.class))
				.isNotNull();
		assertThat(getInitialEntitySet()).hasSize(1).containsOnly(City.class);
	}

	@SuppressWarnings("unchecked")
	private Set<Class<?>> getInitialEntitySet() {
		CassandraMappingContext mappingContext = this.context
				.getBean(CassandraMappingContext.class);
		return (Set<Class<?>>) ReflectionTestUtils.getField(mappingContext,
				"initialEntitySet");
	}

	private void load(Class<?>... configurations) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(configurations);
		ctx.register(CassandraAutoConfiguration.class,
				CassandraRepositoriesAutoConfiguration.class,
				CassandraDataAutoConfiguration.class,
				CassandraReactiveDataAutoConfiguration.class,
				CassandraReactiveRepositoriesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		ctx.refresh();
		this.context = ctx;
	}

	@Configuration
	@TestAutoConfigurationPackage(City.class)
	static class TestConfiguration {

		@Bean
		public Session Session() {
			return mock(Session.class);
		}

	}

	@Configuration
	@TestAutoConfigurationPackage(EmptyDataPackage.class)
	static class EmptyConfiguration {

	}

	@Configuration
	@TestAutoConfigurationPackage(CassandraReactiveRepositoriesAutoConfigurationTests.class)
	@EnableReactiveCassandraRepositories(basePackageClasses = ReactiveCityCassandraRepository.class)
	static class CustomizedConfiguration {

	}

	@Configuration
	@ComponentScan(excludeFilters = @Filter(classes = {
			ReactiveSession.class }, type = FilterType.ASSIGNABLE_TYPE))
	static class TestExcludeConfiguration {

	}

}
