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

package org.springframework.boot.data.mongodb.autoconfigure;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.data.mongodb.autoconfigure.domain.city.CityRepository;
import org.springframework.boot.data.mongodb.autoconfigure.domain.city.ReactiveCityRepository;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoReactiveAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataMongoRepositoriesAutoConfiguration} and
 * {@link DataMongoReactiveRepositoriesAutoConfiguration}.
 *
 * @author Mark Paluch
 */
class DataMongoReactiveAndBlockingRepositoriesAutoConfigurationTests {

	private @Nullable AnnotationConfigApplicationContext context;

	@AfterEach
	void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void shouldCreateInstancesForReactiveAndBlockingRepositories() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(BlockingAndReactiveConfiguration.class, BaseConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(CityRepository.class)).isNotNull();
		assertThat(this.context.getBean(ReactiveCityRepository.class)).isNotNull();
	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(MongoAutoConfiguration.class)
	@EnableMongoRepositories(basePackageClasses = ReactiveCityRepository.class)
	@EnableReactiveMongoRepositories(basePackageClasses = ReactiveCityRepository.class)
	static class BlockingAndReactiveConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@Import(Registrar.class)
	static class BaseConfiguration {

	}

	static class Registrar implements ImportSelector {

		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			List<String> names = new ArrayList<>();
			for (Class<?> type : new Class<?>[] { MongoAutoConfiguration.class, MongoReactiveAutoConfiguration.class,
					DataMongoAutoConfiguration.class, DataMongoRepositoriesAutoConfiguration.class,
					DataMongoReactiveAutoConfiguration.class, DataMongoReactiveRepositoriesAutoConfiguration.class }) {
				names.add(type.getName());
			}
			return StringUtils.toStringArray(names);
		}

	}

}
