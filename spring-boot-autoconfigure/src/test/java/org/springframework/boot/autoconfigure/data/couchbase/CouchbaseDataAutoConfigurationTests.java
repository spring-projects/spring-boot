/*
 * Copyright 2012-2016 the original author or authors.
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

import java.util.Set;

import javax.validation.Validator;

import org.junit.After;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseAutoConfiguration;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseTestConfigurer;
import org.springframework.boot.autoconfigure.data.couchbase.city.City;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.couchbase.config.AbstractCouchbaseDataConfiguration;
import org.springframework.data.couchbase.config.CouchbaseConfigurer;
import org.springframework.data.couchbase.core.CouchbaseTemplate;
import org.springframework.data.couchbase.core.mapping.CouchbaseMappingContext;
import org.springframework.data.couchbase.core.mapping.event.ValidatingCouchbaseEventListener;
import org.springframework.data.couchbase.core.query.Consistency;
import org.springframework.data.couchbase.repository.support.IndexManager;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CouchbaseDataAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
public class CouchbaseDataAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void disabledIfCouchbaseIsNotConfigured() {
		load(null);
		assertThat(this.context.getBeansOfType(IndexManager.class)).isEmpty();
	}

	@Test
	public void customConfiguration() {
		load(CustomCouchbaseConfiguration.class);
		CouchbaseTemplate couchbaseTemplate = this.context
				.getBean(CouchbaseTemplate.class);
		assertThat(couchbaseTemplate.getDefaultConsistency())
				.isEqualTo(Consistency.STRONGLY_CONSISTENT);
	}

	@Test
	public void validatorIsPresent() {
		load(ValidatorConfiguration.class);
		ValidatingCouchbaseEventListener listener = this.context
				.getBean(ValidatingCouchbaseEventListener.class);
		assertThat(new DirectFieldAccessor(listener).getPropertyValue("validator"))
				.isEqualTo(this.context.getBean(Validator.class));
	}

	@Test
	public void autoIndexIsDisabledByDefault() {
		load(CouchbaseTestConfigurer.class);
		IndexManager indexManager = this.context.getBean(IndexManager.class);
		assertThat(indexManager.isIgnoreViews()).isTrue();
		assertThat(indexManager.isIgnoreN1qlPrimary()).isTrue();
		assertThat(indexManager.isIgnoreN1qlSecondary()).isTrue();
	}

	@Test
	public void enableAutoIndex() {
		load(CouchbaseTestConfigurer.class, "spring.data.couchbase.auto-index=true");
		IndexManager indexManager = this.context.getBean(IndexManager.class);
		assertThat(indexManager.isIgnoreViews()).isFalse();
		assertThat(indexManager.isIgnoreN1qlPrimary()).isFalse();
		assertThat(indexManager.isIgnoreN1qlSecondary()).isFalse();
	}

	@Test
	public void changeConsistency() {
		load(CouchbaseTestConfigurer.class,
				"spring.data.couchbase.consistency=eventually-consistent");
		SpringBootCouchbaseDataConfiguration configuration = this.context
				.getBean(SpringBootCouchbaseDataConfiguration.class);
		assertThat(configuration.getDefaultConsistency())
				.isEqualTo(Consistency.EVENTUALLY_CONSISTENT);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void entityScanShouldSetInitialEntitySet() throws Exception {
		load(EntityScanConfig.class);
		CouchbaseMappingContext mappingContext = this.context
				.getBean(CouchbaseMappingContext.class);
		Set<Class<?>> initialEntitySet = (Set<Class<?>>) ReflectionTestUtils
				.getField(mappingContext, "initialEntitySet");
		assertThat(initialEntitySet).containsOnly(City.class);
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(context, environment);
		if (config != null) {
			context.register(config);
		}
		context.register(PropertyPlaceholderAutoConfiguration.class,
				CouchbaseAutoConfiguration.class, CouchbaseDataAutoConfiguration.class);
		context.refresh();
		this.context = context;
	}

	@Configuration
	@Import(CouchbaseTestConfigurer.class)
	static class ValidatorConfiguration {

		@Bean
		public Validator myValidator() {
			return mock(Validator.class);
		}

	}

	@Configuration
	static class CustomCouchbaseConfiguration extends AbstractCouchbaseDataConfiguration {

		@Override
		protected CouchbaseConfigurer couchbaseConfigurer() {
			return new CouchbaseTestConfigurer();
		}

		@Override
		protected Consistency getDefaultConsistency() {
			return Consistency.STRONGLY_CONSISTENT;
		}

	}

	@Configuration
	@EntityScan("org.springframework.boot.autoconfigure.data.couchbase.city")
	@Import(CustomCouchbaseConfiguration.class)
	static class EntityScanConfig {

	}

}
