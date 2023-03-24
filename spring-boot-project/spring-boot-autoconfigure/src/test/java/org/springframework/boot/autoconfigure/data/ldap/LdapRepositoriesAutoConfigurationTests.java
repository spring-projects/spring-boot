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

package org.springframework.boot.autoconfigure.data.ldap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.alt.ldap.PersonLdapRepository;
import org.springframework.boot.autoconfigure.data.empty.EmptyDataPackage;
import org.springframework.boot.autoconfigure.data.ldap.person.Person;
import org.springframework.boot.autoconfigure.data.ldap.person.PersonRepository;
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.ldap.repository.config.EnableLdapRepositories;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LdapRepositoriesAutoConfiguration}
 *
 * @author Eddú Meléndez
 */
class LdapRepositoriesAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@AfterEach
	void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void testDefaultRepositoryConfiguration() {
		load(TestConfiguration.class);
		assertThat(this.context.getBean(PersonRepository.class)).isNotNull();
	}

	@Test
	void testNoRepositoryConfiguration() {
		load(EmptyConfiguration.class);
		assertThat(this.context.getBeanNamesForType(PersonRepository.class)).isEmpty();
	}

	@Test
	void doesNotTriggerDefaultRepositoryDetectionIfCustomized() {
		load(CustomizedConfiguration.class);
		assertThat(this.context.getBean(PersonLdapRepository.class)).isNotNull();
	}

	private void load(Class<?>... configurationClasses) {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.ldap.urls:ldap://localhost:389").applyTo(this.context);
		this.context.register(configurationClasses);
		this.context.register(LdapAutoConfiguration.class, LdapRepositoriesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
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
	@TestAutoConfigurationPackage(LdapRepositoriesAutoConfigurationTests.class)
	@EnableLdapRepositories(basePackageClasses = PersonLdapRepository.class)
	static class CustomizedConfiguration {

	}

}
