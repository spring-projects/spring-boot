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

package org.springframework.boot.autoconfigure.data.couchbase;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseAutoConfiguration;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseAutoConfigurationTests;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseTestConfigurer;
import org.springframework.boot.autoconfigure.data.couchbase.city.CityRepository;
import org.springframework.boot.autoconfigure.data.couchbase.city.ReactiveCityRepository;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.couchbase.repository.config.EnableCouchbaseRepositories;
import org.springframework.data.couchbase.repository.config.EnableReactiveCouchbaseRepositories;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CouchbaseRepositoriesAutoConfiguration} and
 * {@link CouchbaseReactiveRepositoriesAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
public class CouchbaseReactiveAndImperativeRepositoriesAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		this.context.close();
	}

	@Test
	public void shouldCreateInstancesForReactiveAndImperativeRepositories() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.datasource.initialization-mode:never").applyTo(this.context);
		this.context.register(ImperativeAndReactiveConfiguration.class, BaseConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(CityRepository.class)).isNotNull();
		assertThat(this.context.getBean(ReactiveCityRepository.class)).isNotNull();
	}

	@Configuration
	@TestAutoConfigurationPackage(CouchbaseAutoConfigurationTests.class)
	@EnableCouchbaseRepositories(basePackageClasses = CityRepository.class)
	@EnableReactiveCouchbaseRepositories(basePackageClasses = ReactiveCityRepository.class)
	protected static class ImperativeAndReactiveConfiguration {

	}

	@Configuration
	@Import({ CouchbaseTestConfigurer.class, Registrar.class })
	protected static class BaseConfiguration {

	}

	protected static class Registrar implements ImportSelector {

		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			List<String> names = new ArrayList<>();
			for (Class<?> type : new Class<?>[] { CouchbaseAutoConfiguration.class,
					CouchbaseDataAutoConfiguration.class, CouchbaseRepositoriesAutoConfiguration.class,
					CouchbaseReactiveDataAutoConfiguration.class,
					CouchbaseReactiveRepositoriesAutoConfiguration.class }) {
				names.add(type.getName());
			}
			return StringUtils.toStringArray(names);
		}

	}

}
