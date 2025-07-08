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

package org.springframework.boot.data.couchbase.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.couchbase.autoconfigure.CouchbaseAutoConfiguration;
import org.springframework.boot.data.couchbase.domain.city.City;
import org.springframework.boot.data.couchbase.domain.city.CityRepository;
import org.springframework.boot.data.couchbase.domain.city.ReactiveCityRepository;
import org.springframework.boot.data.couchbase.domain.empty.EmptyDataPackage;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.couchbase.repository.config.EnableCouchbaseRepositories;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CouchbaseReactiveRepositoriesAutoConfiguration}.
 *
 * @author Alex Derkach
 * @author Stephane Nicoll
 */
class CouchbaseReactiveRepositoriesAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(CouchbaseAutoConfiguration.class, CouchbaseDataAutoConfiguration.class,
				CouchbaseRepositoriesAutoConfiguration.class, CouchbaseReactiveDataAutoConfiguration.class,
				CouchbaseReactiveRepositoriesAutoConfiguration.class));

	@Test
	void couchbaseNotAvailable() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(ReactiveCityRepository.class));
	}

	@Test
	void defaultRepository() {
		this.contextRunner.withUserConfiguration(DefaultConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(ReactiveCityRepository.class));
	}

	@Test
	void imperativeRepositories() {
		this.contextRunner.withUserConfiguration(DefaultConfiguration.class)
			.withPropertyValues("spring.data.couchbase.repositories.type=imperative")
			.run((context) -> assertThat(context).doesNotHaveBean(ReactiveCityRepository.class));
	}

	@Test
	void disabledRepositories() {
		this.contextRunner.withUserConfiguration(DefaultConfiguration.class)
			.withPropertyValues("spring.data.couchbase.repositories.type=none")
			.run((context) -> assertThat(context).doesNotHaveBean(ReactiveCityRepository.class));
	}

	@Test
	void noRepositoryAvailable() {
		this.contextRunner.withUserConfiguration(NoRepositoryConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(ReactiveCityRepository.class));
	}

	@Test
	void doesNotTriggerDefaultRepositoryDetectionIfCustomized() {
		this.contextRunner.withUserConfiguration(CustomizedConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(ReactiveCityRepository.class));
	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(City.class)
	@Import(CouchbaseMockConfiguration.class)
	static class DefaultConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(EmptyDataPackage.class)
	@Import(CouchbaseMockConfiguration.class)
	static class NoRepositoryConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(CouchbaseReactiveRepositoriesAutoConfigurationTests.class)
	@EnableCouchbaseRepositories(basePackageClasses = CityRepository.class)
	@Import(CouchbaseMockConfiguration.class)
	static class CustomizedConfiguration {

	}

}
