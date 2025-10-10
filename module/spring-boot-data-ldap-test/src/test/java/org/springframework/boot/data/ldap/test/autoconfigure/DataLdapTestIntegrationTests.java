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

package org.springframework.boot.data.ldap.test.autoconfigure;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Sample test for {@link DataLdapTest @DataLdapTest}
 *
 * @author Eddú Meléndez
 */
@DataLdapTest
@TestPropertySource(properties = { "spring.ldap.embedded.base-dn=dc=spring,dc=org",
		"spring.ldap.embedded.ldif=classpath:org/springframework/boot/data/ldap/test/autoconfigure/schema.ldif" })
class DataLdapTestIntegrationTests {

	@Autowired
	private LdapTemplate ldapTemplate;

	@Autowired
	private ExampleRepository exampleRepository;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void testRepository() {
		LdapQuery ldapQuery = LdapQueryBuilder.query().where("cn").is("Bob Smith");
		Optional<ExampleEntry> entry = this.exampleRepository.findOne(ldapQuery);
		assertThat(entry).isPresent();
		assertThat(entry.get().getDn())
			.isEqualTo(LdapUtils.newLdapName("cn=Bob Smith,ou=company1,c=Sweden,dc=spring,dc=org"));
		assertThat(this.ldapTemplate.findOne(ldapQuery, ExampleEntry.class).getDn())
			.isEqualTo(LdapUtils.newLdapName("cn=Bob Smith,ou=company1,c=Sweden,dc=spring,dc=org"));
	}

	@Test
	void didNotInjectExampleService() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
			.isThrownBy(() -> this.applicationContext.getBean(ExampleService.class));
	}

}
