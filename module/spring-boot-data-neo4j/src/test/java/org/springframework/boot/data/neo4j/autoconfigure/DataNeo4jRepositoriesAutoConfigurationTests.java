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

package org.springframework.boot.data.neo4j.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.data.neo4j.domain.city.City;
import org.springframework.boot.data.neo4j.domain.city.CityRepository;
import org.springframework.boot.data.neo4j.domain.city.ReactiveCityRepository;
import org.springframework.boot.data.neo4j.domain.country.CountryRepository;
import org.springframework.boot.data.neo4j.domain.country.ReactiveCountryRepository;
import org.springframework.boot.data.neo4j.domain.empty.EmptyPackage;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.repository.support.ReactiveNeo4jRepositoryFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DataNeo4jRepositoriesAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Oliver Gierke
 * @author Michael Hunger
 * @author Vince Bickers
 * @author Stephane Nicoll
 * @author Michael J. Simons
 */
class DataNeo4jRepositoriesAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(MockedDriverConfiguration.class)
		.withConfiguration(
				AutoConfigurations.of(DataNeo4jAutoConfiguration.class, DataNeo4jRepositoriesAutoConfiguration.class));

	@Test
	void configurationWithDefaultRepositories() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(CityRepository.class));
	}

	@Test
	void configurationWithNoRepositories() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(Neo4jTransactionManager.class)
				.doesNotHaveBean(Neo4jRepository.class));
	}

	@Test
	void configurationWithDisabledRepositories() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
			.withPropertyValues("spring.data.neo4j.repositories.type=none")
			.run((context) -> assertThat(context).doesNotHaveBean(Neo4jRepository.class));
	}

	@Test
	void autoConfigurationShouldNotKickInEvenIfManualConfigDidNotCreateAnyRepositories() {
		this.contextRunner.withUserConfiguration(SortOfInvalidCustomConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(Neo4jTransactionManager.class)
				.doesNotHaveBean(Neo4jRepository.class));
	}

	@Test
	void shouldRespectAtEnableNeo4jRepositories() {
		this.contextRunner.withUserConfiguration(SortOfInvalidCustomConfiguration.class, WithCustomRepositoryScan.class)
			.run((context) -> assertThat(context).doesNotHaveBean(CityRepository.class)
				.doesNotHaveBean(ReactiveCityRepository.class)
				.hasSingleBean(CountryRepository.class)
				.doesNotHaveBean(ReactiveCountryRepository.class));
	}

	@Configuration(proxyBeanMethods = false)
	@EnableNeo4jRepositories(basePackageClasses = CountryRepository.class)
	static class WithCustomRepositoryScan {

	}

	@Configuration(proxyBeanMethods = false)
	static class WithFakeEnabledReactiveNeo4jRepositories {

		@Bean
		ReactiveNeo4jRepositoryFactoryBean<?, ?, ?> reactiveNeo4jRepositoryFactoryBean() {
			return mock(ReactiveNeo4jRepositoryFactoryBean.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(City.class)
	static class TestConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(EmptyPackage.class)
	static class EmptyConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableNeo4jRepositories("foo.bar")
	@TestAutoConfigurationPackage(DataNeo4jRepositoriesAutoConfigurationTests.class)
	static class SortOfInvalidCustomConfiguration {

	}

}
