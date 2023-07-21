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

package org.springframework.boot.autoconfigure.data.r2dbc;

import java.time.Duration;

import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.data.empty.EmptyDataPackage;
import org.springframework.boot.autoconfigure.data.r2dbc.city.City;
import org.springframework.boot.autoconfigure.data.r2dbc.city.CityRepository;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.data.r2dbc.repository.config.R2dbcRepositoryConfigurationExtension;
import org.springframework.data.repository.Repository;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.r2dbc.core.DatabaseClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link R2dbcRepositoriesAutoConfiguration}.
 *
 * @author Mark Paluch
 */
class R2dbcRepositoriesAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(R2dbcRepositoriesAutoConfiguration.class));

	@Test
	void backsOffWithNoConnectionFactory() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(R2dbcRepositoryConfigurationExtension.class));
	}

	@Test
	void backsOffWithNoDatabaseClientOperations() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
			.withClassLoader(new FilteredClassLoader("org.springframework.r2dbc"))
			.withUserConfiguration(TestConfiguration.class)
			.run((context) -> {
				assertThat(context).doesNotHaveBean(DatabaseClient.class);
				assertThat(context).doesNotHaveBean(R2dbcRepositoryConfigurationExtension.class);
			});
	}

	@Test
	void basicAutoConfiguration() {
		this.contextRunner
			.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class, R2dbcDataAutoConfiguration.class))
			.withUserConfiguration(DatabaseInitializationConfiguration.class, TestConfiguration.class)
			.withPropertyValues("spring.r2dbc.generate-unique-name:true")
			.run((context) -> {
				assertThat(context).hasSingleBean(CityRepository.class);
				context.getBean(CityRepository.class)
					.findById(2000L)
					.as(StepVerifier::create)
					.expectNextCount(1)
					.expectComplete()
					.verify(Duration.ofSeconds(30));
			});
	}

	@Test
	void autoConfigurationWithNoRepositories() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
			.withUserConfiguration(EmptyConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(Repository.class));
	}

	@Test
	void honorsUsersEnableR2dbcRepositoriesConfiguration() {
		this.contextRunner
			.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class, R2dbcDataAutoConfiguration.class))
			.withUserConfiguration(DatabaseInitializationConfiguration.class, EnableRepositoriesConfiguration.class)
			.withPropertyValues("spring.r2dbc.generate-unique-name:true")
			.run((context) -> {
				assertThat(context).hasSingleBean(CityRepository.class);
				context.getBean(CityRepository.class)
					.findById(2000L)
					.as(StepVerifier::create)
					.expectNextCount(1)
					.expectComplete()
					.verify(Duration.ofSeconds(30));
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class DatabaseInitializationConfiguration {

		@Autowired
		void initializeDatabase(ConnectionFactory connectionFactory) {
			ResourceLoader resourceLoader = new DefaultResourceLoader();
			Resource[] scripts = new Resource[] { resourceLoader.getResource("classpath:data-city-schema.sql"),
					resourceLoader.getResource("classpath:city.sql") };
			new ResourceDatabasePopulator(scripts).populate(connectionFactory).block();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(City.class)
	static class TestConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(EmptyDataPackage.class)
	static class EmptyConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableR2dbcRepositories(basePackageClasses = City.class)
	static class EnableRepositoriesConfiguration {

	}

}
