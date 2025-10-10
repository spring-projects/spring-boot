/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.data.cassandra.autoconfigure;

import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.cassandra.autoconfigure.CassandraAutoConfiguration;
import org.springframework.boot.data.cassandra.domain.city.City;
import org.springframework.boot.data.cassandra.domain.city.CityRepository;
import org.springframework.boot.data.cassandra.domain.empty.EmptyDataPackage;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.cassandra.core.mapping.CassandraMappingContext;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;
import org.springframework.data.domain.ManagedTypes;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataCassandraRepositoriesAutoConfiguration}.
 *
 * @author Eddú Meléndez
 * @author Mark Paluch
 * @author Stephane Nicoll
 */
class DataCassandraRepositoriesAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(CassandraAutoConfiguration.class, DataCassandraRepositoriesAutoConfiguration.class,
					DataCassandraAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class));

	@Test
	void testDefaultRepositoryConfiguration() {
		this.contextRunner.withUserConfiguration(DefaultConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(CityRepository.class);
			assertThat(context).hasSingleBean(CqlSessionBuilder.class);
			assertThat(getManagedTypes(context).toList()).hasSize(1);
		});
	}

	@Test
	void testNoRepositoryConfiguration() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(CqlSessionBuilder.class);
			assertThat(getManagedTypes(context).toList()).isEmpty();
		});
	}

	@Test
	void doesNotTriggerDefaultRepositoryDetectionIfCustomized() {
		this.contextRunner.withUserConfiguration(CustomizedConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(CityRepository.class);
			assertThat(getManagedTypes(context).toList()).hasSize(1).containsOnly(City.class);
		});
	}

	@Test
	void enablingReactiveRepositoriesDisablesImperativeRepositories() {
		this.contextRunner.withUserConfiguration(DefaultConfiguration.class)
			.withPropertyValues("spring.data.cassandra.repositories.type=reactive")
			.run((context) -> assertThat(context).doesNotHaveBean(CityRepository.class));
	}

	@Test
	void enablingNoRepositoriesDisablesImperativeRepositories() {
		this.contextRunner.withUserConfiguration(DefaultConfiguration.class)
			.withPropertyValues("spring.data.cassandra.repositories.type=none")
			.run((context) -> assertThat(context).doesNotHaveBean(CityRepository.class));
	}

	private ManagedTypes getManagedTypes(AssertableApplicationContext context) {
		CassandraMappingContext mappingContext = context.getBean(CassandraMappingContext.class);
		Object field = ReflectionTestUtils.getField(mappingContext, "managedTypes");
		assertThat(field).isNotNull();
		return (ManagedTypes) field;
	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(EmptyDataPackage.class)
	@Import(CassandraMockConfiguration.class)
	static class EmptyConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(City.class)
	@Import(CassandraMockConfiguration.class)
	static class DefaultConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(City.class)
	@EnableCassandraRepositories(basePackageClasses = CityRepository.class)
	@Import(CassandraMockConfiguration.class)
	static class CustomizedConfiguration {

	}

}
