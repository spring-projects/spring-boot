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

import com.datastax.driver.core.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.city.City;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.cassandra.core.session.ReactiveSession;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.cassandra.core.ReactiveCassandraTemplate;
import org.springframework.data.cassandra.mapping.CassandraMappingContext;
import org.springframework.data.cassandra.mapping.SimpleUserTypeResolver;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ReactiveCassandraDataAutoConfiguration} that require a Cassandra instance.
 *
 * @author Eddú Meléndez
 */
public class ReactiveCassandraDataAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@Before
	public void setup() {
		this.context = new AnnotationConfigApplicationContext();
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void templateExists() {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.data.cassandra.keyspaceName:boot_test");
		this.context.register(TestExcludeConfiguration.class, TestConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				CassandraAutoConfiguration.class, CassandraDataAutoConfiguration.class,
				ReactiveCassandraDataAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(ReactiveCassandraTemplate.class).length)
				.isEqualTo(1);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void entityScanShouldSetInitialEntitySet() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.data.cassandra.keyspaceName:boot_test");
		this.context.register(TestConfiguration.class, EntityScanConfig.class,
				PropertyPlaceholderAutoConfiguration.class,
				CassandraAutoConfiguration.class, CassandraDataAutoConfiguration.class,
				ReactiveCassandraDataAutoConfiguration.class);
		this.context.refresh();
		CassandraMappingContext mappingContext = this.context
				.getBean(CassandraMappingContext.class);
		Set<Class<?>> initialEntitySet = (Set<Class<?>>) ReflectionTestUtils
				.getField(mappingContext, "initialEntitySet");
		assertThat(initialEntitySet).containsOnly(City.class);
	}

	@Test
	public void userTypeResolverShouldBeSet() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.data.cassandra.keyspaceName:boot_test");
		this.context.register(TestConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				CassandraAutoConfiguration.class, CassandraDataAutoConfiguration.class,
				ReactiveCassandraDataAutoConfiguration.class);
		this.context.refresh();
		CassandraMappingContext mappingContext = this.context
				.getBean(CassandraMappingContext.class);
		assertThat(ReflectionTestUtils.getField(mappingContext, "userTypeResolver"))
				.isInstanceOf(SimpleUserTypeResolver.class);
	}

	@Configuration
	@ComponentScan(excludeFilters = @Filter(classes = {
			ReactiveSession.class }, type = FilterType.ASSIGNABLE_TYPE))
	static class TestExcludeConfiguration {

	}

	@Configuration
	static class TestConfiguration {

		@Bean
		public Session session() {
			return mock(Session.class);
		}

	}

	@Configuration
	@EntityScan("org.springframework.boot.autoconfigure.data.cassandra.city")
	static class EntityScanConfig {

	}

}
