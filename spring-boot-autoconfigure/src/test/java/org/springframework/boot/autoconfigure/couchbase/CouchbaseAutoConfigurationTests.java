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

package org.springframework.boot.autoconfigure.couchbase;

import javax.validation.Validator;

import com.couchbase.client.java.Bucket;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.couchbase.config.AbstractCouchbaseConfiguration;
import org.springframework.data.couchbase.core.CouchbaseTemplate;
import org.springframework.data.couchbase.core.mapping.event.ValidatingCouchbaseEventListener;
import org.springframework.data.couchbase.core.query.Consistency;
import org.springframework.data.couchbase.repository.support.IndexManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CouchbaseAutoConfiguration}
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 */
public class CouchbaseAutoConfigurationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void bucketNameIsRequired() {
		load(null);
		assertThat(this.context.getBeansOfType(CouchbaseTemplate.class)).isEmpty();
		assertThat(this.context.getBeansOfType(Bucket.class)).isEmpty();
		assertThat(this.context.getBeansOfType(ValidatingCouchbaseEventListener.class))
				.isEmpty();
	}

	@Test
	public void bucketNameIsNotRequiredIfCustomConfigurationIsSpecified()
			throws Exception {
		load(CouchbaseTestConfiguration.class);
		assertThat(this.context.getBeansOfType(AbstractCouchbaseConfiguration.class))
				.hasSize(1);
		CouchbaseTestConfiguration configuration = this.context
				.getBean(CouchbaseTestConfiguration.class);
		assertThat(this.context.getBean(CouchbaseTemplate.class))
				.isSameAs(configuration.couchbaseTemplate());
		assertThat(this.context.getBean(Bucket.class))
				.isSameAs(configuration.couchbaseClient());
		assertThat(this.context.getBeansOfType(ValidatingCouchbaseEventListener.class))
				.isEmpty();
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
		load(CouchbaseTestConfiguration.class);
		CouchbaseTestConfiguration configuration = this.context
				.getBean(CouchbaseTestConfiguration.class);
		IndexManager indexManager = configuration.indexManager();
		assertThat(indexManager.isIgnoreViews()).isTrue();
		assertThat(indexManager.isIgnoreN1qlPrimary()).isTrue();
		assertThat(indexManager.isIgnoreN1qlSecondary()).isTrue();
	}

	@Test
	public void enableAutoIndex() {
		load(CouchbaseTestConfiguration.class, "spring.data.couchbase.auto-index=true");
		CouchbaseTestConfiguration configuration = this.context
				.getBean(CouchbaseTestConfiguration.class);
		IndexManager indexManager = configuration.indexManager();
		assertThat(indexManager.isIgnoreViews()).isFalse();
		assertThat(indexManager.isIgnoreN1qlPrimary()).isFalse();
		assertThat(indexManager.isIgnoreN1qlSecondary()).isFalse();
	}

	@Test
	public void changeConsistency() {
		load(CouchbaseTestConfiguration.class,
				"spring.data.couchbase.consistency=eventually-consistent");
		CouchbaseTestConfiguration configuration = this.context
				.getBean(CouchbaseTestConfiguration.class);
		assertThat(configuration.getDefaultConsistency())
				.isEqualTo(Consistency.EVENTUALLY_CONSISTENT);
	}

	@Test
	public void overrideCouchbaseOperations() {
		load(CouchbaseTemplateConfiguration.class);
		CouchbaseTemplateConfiguration configuration = this.context
				.getBean(CouchbaseTemplateConfiguration.class);
		assertThat(this.context.getBean(CouchbaseTemplate.class))
				.isSameAs(configuration.myCouchbaseTemplate());
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(context, environment);
		if (config != null) {
			context.register(config);
		}
		context.register(PropertyPlaceholderAutoConfiguration.class,
				CouchbaseAutoConfiguration.class);
		context.refresh();
		this.context = context;
	}

	@Configuration
	@Import(CouchbaseTestConfiguration.class)
	static class ValidatorConfiguration {

		@Bean
		public Validator myValidator() {
			return mock(Validator.class);
		}

	}

	@Configuration
	@Import(CouchbaseTestConfiguration.class)
	static class CouchbaseTemplateConfiguration {

		@Bean(name = "couchbaseTemplate")
		public CouchbaseTemplate myCouchbaseTemplate() {
			return mock(CouchbaseTemplate.class);
		}

	}

}
