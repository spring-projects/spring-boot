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

package org.springframework.boot.autoconfigure.data.couchbase;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseAutoConfiguration;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseTestConfigurer;
import org.springframework.boot.autoconfigure.data.alt.couchbase.CityCouchbaseRepository;
import org.springframework.boot.autoconfigure.data.alt.couchbase.ReactiveCityCouchbaseRepository;
import org.springframework.boot.autoconfigure.data.couchbase.city.City;
import org.springframework.boot.autoconfigure.data.couchbase.city.ReactiveCityRepository;
import org.springframework.boot.autoconfigure.data.empty.EmptyDataPackage;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.couchbase.repository.config.EnableCouchbaseRepositories;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CouchbaseReactiveRepositoriesAutoConfiguration}.
 *
 * @author Alex Derkach
 */
public class CouchbaseReactiveRepositoriesAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void couchbaseNotAvailable() {
		load(null);
		assertThat(this.context.getBeansOfType(ReactiveCityRepository.class)).hasSize(0);
	}

	@Test
	public void defaultRepository() {
		load(DefaultConfiguration.class);
		assertThat(this.context.getBeansOfType(ReactiveCityRepository.class)).hasSize(1);
	}

	@Test
	public void imperativeRepositories() {
		load(DefaultConfiguration.class,
				"spring.data.couchbase.repositories.type=imperative");
		assertThat(this.context.getBeansOfType(ReactiveCityRepository.class)).hasSize(0);
	}

	@Test
	public void disabledRepositories() {
		load(DefaultConfiguration.class, "spring.data.couchbase.repositories.type=none");
		assertThat(this.context.getBeansOfType(ReactiveCityRepository.class)).hasSize(0);
	}

	@Test
	public void noRepositoryAvailable() {
		load(NoRepositoryConfiguration.class);
		assertThat(this.context.getBeansOfType(ReactiveCityRepository.class)).hasSize(0);
	}

	@Test
	public void doesNotTriggerDefaultRepositoryDetectionIfCustomized() {
		load(CustomizedConfiguration.class);
		assertThat(this.context.getBeansOfType(ReactiveCityCouchbaseRepository.class))
				.isEmpty();
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of(environment).applyTo(context);
		if (config != null) {
			context.register(config);
		}
		context.register(PropertyPlaceholderAutoConfiguration.class,
				CouchbaseAutoConfiguration.class, CouchbaseDataAutoConfiguration.class,
				CouchbaseRepositoriesAutoConfiguration.class,
				CouchbaseReactiveDataAutoConfiguration.class,
				CouchbaseReactiveRepositoriesAutoConfiguration.class);
		context.refresh();
		this.context = context;
	}

	@Configuration
	@TestAutoConfigurationPackage(City.class)
	@Import(CouchbaseTestConfigurer.class)
	static class DefaultConfiguration {

	}

	@Configuration
	@TestAutoConfigurationPackage(EmptyDataPackage.class)
	@Import(CouchbaseTestConfigurer.class)
	protected static class NoRepositoryConfiguration {

	}

	@Configuration
	@TestAutoConfigurationPackage(CouchbaseReactiveRepositoriesAutoConfigurationTests.class)
	@EnableCouchbaseRepositories(basePackageClasses = CityCouchbaseRepository.class)
	@Import(CouchbaseDataAutoConfigurationTests.CustomCouchbaseConfiguration.class)
	protected static class CustomizedConfiguration {

	}

}
