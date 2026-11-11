/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.couchbase.autoconfigure;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.couchbase.autoconfigure.CouchbaseAutoConfiguration;
import org.springframework.boot.data.couchbase.domain.city.CityRepository;
import org.springframework.boot.data.couchbase.domain.city.ReactiveCityRepository;
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
 * Tests for {@link DataCouchbaseRepositoriesAutoConfiguration} and
 * {@link DataCouchbaseReactiveRepositoriesAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class DataCouchbaseReactiveAndImperativeRepositoriesAutoConfigurationTests {

	@Test
	void shouldCreateInstancesForReactiveAndImperativeRepositories() {
		new ApplicationContextRunner()
			.withUserConfiguration(ImperativeAndReactiveConfiguration.class, BaseConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(CityRepository.class)
				.hasSingleBean(ReactiveCityRepository.class));
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
					DataCouchbaseAutoConfiguration.class, DataCouchbaseRepositoriesAutoConfiguration.class,
					DataCouchbaseReactiveAutoConfiguration.class,
					DataCouchbaseReactiveRepositoriesAutoConfiguration.class }) {
				names.add(type.getName());
			}
			return StringUtils.toStringArray(names);
		}

	}

}
