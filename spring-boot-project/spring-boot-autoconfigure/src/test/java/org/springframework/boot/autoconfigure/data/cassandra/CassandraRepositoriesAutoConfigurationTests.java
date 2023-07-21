/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.cassandra;

import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.alt.cassandra.CityCassandraRepository;
import org.springframework.boot.autoconfigure.data.cassandra.city.City;
import org.springframework.boot.autoconfigure.data.cassandra.city.CityRepository;
import org.springframework.boot.autoconfigure.data.empty.EmptyDataPackage;
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
 * Tests for {@link CassandraRepositoriesAutoConfiguration}.
 *
 * @author Eddú Meléndez
 * @author Mark Paluch
 * @author Stephane Nicoll
 */
class CassandraRepositoriesAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(CassandraAutoConfiguration.class, CassandraRepositoriesAutoConfiguration.class,
					CassandraDataAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class));

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
			assertThat(context).hasSingleBean(CityCassandraRepository.class);
			assertThat(getManagedTypes(context).toList()).hasSize(1).containsOnly(City.class);
		});
	}

	@Test
	void enablingReactiveRepositoriesDisablesImperativeRepositories() {
		this.contextRunner.withUserConfiguration(DefaultConfiguration.class)
			.withPropertyValues("spring.cassandra.repositories.type=reactive")
			.run((context) -> assertThat(context).doesNotHaveBean(CityCassandraRepository.class));
	}

	@Test
	void enablingNoRepositoriesDisablesImperativeRepositories() {
		this.contextRunner.withUserConfiguration(DefaultConfiguration.class)
			.withPropertyValues("spring.cassandra.repositories.type=none")
			.run((context) -> assertThat(context).doesNotHaveBean(CityCassandraRepository.class));
	}

	private ManagedTypes getManagedTypes(AssertableApplicationContext context) {
		CassandraMappingContext mappingContext = context.getBean(CassandraMappingContext.class);
		return (ManagedTypes) ReflectionTestUtils.getField(mappingContext, "managedTypes");
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
	@TestAutoConfigurationPackage(CassandraRepositoriesAutoConfigurationTests.class)
	@EnableCassandraRepositories(basePackageClasses = CityCassandraRepository.class)
	@Import(CassandraMockConfiguration.class)
	static class CustomizedConfiguration {

	}

}
