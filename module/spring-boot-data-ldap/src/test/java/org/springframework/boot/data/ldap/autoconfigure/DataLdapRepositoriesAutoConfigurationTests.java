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

package org.springframework.boot.data.ldap.autoconfigure;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.data.ldap.autoconfigure.domain.empty.EmptyDataPackage;
import org.springframework.boot.data.ldap.autoconfigure.domain.person.Person;
import org.springframework.boot.data.ldap.autoconfigure.domain.person.PersonRepository;
import org.springframework.boot.ldap.autoconfigure.LdapAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.ldap.repository.config.EnableLdapRepositories;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataLdapRepositoriesAutoConfiguration}
 *
 * @author Eddú Meléndez
 */
class DataLdapRepositoriesAutoConfigurationTests {

	private @Nullable AnnotationConfigApplicationContext context;

	@AfterEach
	void close() {
		if (getContext() != null) {
			getContext().close();
		}
	}

	@Test
	void testDefaultRepositoryConfiguration() {
		load(TestConfiguration.class);
		assertThat(getContext().getBean(PersonRepository.class)).isNotNull();
	}

	@Test
	void testNoRepositoryConfiguration() {
		load(EmptyConfiguration.class);
		assertThat(getContext().getBeanNamesForType(PersonRepository.class)).isEmpty();
	}

	@Test
	void doesNotTriggerDefaultRepositoryDetectionIfCustomized() {
		load(CustomizedConfiguration.class);
		assertThat(getContext().getBean(PersonRepository.class)).isNotNull();
	}

	private void load(Class<?>... configurationClasses) {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.ldap.urls:ldap://localhost:389").applyTo(this.context);
		this.context.register(configurationClasses);
		this.context.register(LdapAutoConfiguration.class, DataLdapRepositoriesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
	}

	private AnnotationConfigApplicationContext getContext() {
		AnnotationConfigApplicationContext context = this.context;
		assertThat(context).isNotNull();
		return context;
	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(Person.class)
	static class TestConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(EmptyDataPackage.class)
	static class EmptyConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(DataLdapRepositoriesAutoConfigurationTests.class)
	@EnableLdapRepositories(basePackageClasses = PersonRepository.class)
	static class CustomizedConfiguration {

	}

}
