/*
 * Copyright 2012-2020 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseAutoConfiguration;
import org.springframework.boot.autoconfigure.data.couchbase.city.CityRepository;
import org.springframework.boot.autoconfigure.data.couchbase.city.ReactiveCityRepository;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
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
class CouchbaseReactiveAndImperativeRepositoriesAutoConfigurationTests {

	@Test
	void shouldCreateInstancesForReactiveAndImperativeRepositories() {
		new ApplicationContextRunner()
				.withUserConfiguration(ImperativeAndReactiveConfiguration.class, BaseConfiguration.class)
				.withPropertyValues("spring.datasource.initialization-mode:never").run((context) -> assertThat(context)
						.hasSingleBean(CityRepository.class).hasSingleBean(ReactiveCityRepository.class));
	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(CouchbaseAutoConfiguration.class)
	@EnableCouchbaseRepositories(basePackageClasses = CityRepository.class)
	@EnableReactiveCouchbaseRepositories(basePackageClasses = ReactiveCityRepository.class)
	static class ImperativeAndReactiveConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@Import({ CouchbaseMockConfiguration.class, Registrar.class })
	static class BaseConfiguration {

	}

	static class Registrar implements ImportSelector {

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
