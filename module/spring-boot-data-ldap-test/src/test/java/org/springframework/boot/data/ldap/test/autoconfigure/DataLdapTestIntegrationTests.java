/*
 * Copyright 2012-present the original author or authors.
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
