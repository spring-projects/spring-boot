/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.autoconfigure.ldap.embedded;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EmbeddedLdapAutoConfiguration}
 *
 * @author Eddú Meléndez
 */
class EmbeddedLdapAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(EmbeddedLdapAutoConfiguration.class));

	@Test
	void testSetDefaultPort() {
		this.contextRunner
			.withPropertyValues("spring.ldap.embedded.port:1234", "spring.ldap.embedded.base-dn:dc=spring,dc=org")
			.run((context) -> {
				InMemoryDirectoryServer server = context.getBean(InMemoryDirectoryServer.class);
				assertThat(server.getListenPort()).isEqualTo(1234);
			});
	}

	@Test
	void testRandomPortWithEnvironment() {
		this.contextRunner.withPropertyValues("spring.ldap.embedded.base-dn:dc=spring,dc=org").run((context) -> {
			InMemoryDirectoryServer server = context.getBean(InMemoryDirectoryServer.class);
			assertThat(server.getListenPort())
				.isEqualTo(context.getEnvironment().getProperty("local.ldap.port", Integer.class));
		});
	}

	@Test
	void testRandomPortWithValueAnnotation() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.ldap.embedded.base-dn:dc=spring,dc=org").applyTo(context);
		context.register(EmbeddedLdapAutoConfiguration.class, LdapClientConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		context.refresh();
		LDAPConnection connection = context.getBean(LDAPConnection.class);
		assertThat(connection.getConnectedPort())
			.isEqualTo(context.getEnvironment().getProperty("local.ldap.port", Integer.class));
	}

	@Test
	void testSetCredentials() {
		this.contextRunner.withPropertyValues("spring.ldap.embedded.base-dn:dc=spring,dc=org",
				"spring.ldap.embedded.credential.username:uid=root", "spring.ldap.embedded.credential.password:boot")
			.run((context) -> {
				InMemoryDirectoryServer server = context.getBean(InMemoryDirectoryServer.class);
				BindResult result = server.bind("uid=root", "boot");
				assertThat(result).isNotNull();
			});
	}

	@Test
	void testSetPartitionSuffix() {
		this.contextRunner.withPropertyValues("spring.ldap.embedded.base-dn:dc=spring,dc=org").run((context) -> {
			InMemoryDirectoryServer server = context.getBean(InMemoryDirectoryServer.class);
			assertThat(server.getBaseDNs()).containsExactly(new DN("dc=spring,dc=org"));
		});
	}

	@Test
	@WithSchemaLdifResource
	void testSetLdifFile() {
		this.contextRunner.withPropertyValues("spring.ldap.embedded.base-dn:dc=spring,dc=org").run((context) -> {
			InMemoryDirectoryServer server = context.getBean(InMemoryDirectoryServer.class);
			assertThat(server.countEntriesBelow("ou=company1,c=Sweden,dc=spring,dc=org")).isEqualTo(5);
		});
	}

	@Test
	@WithSchemaLdifResource
	void testQueryEmbeddedLdap() {
		this.contextRunner.withPropertyValues("spring.ldap.embedded.base-dn:dc=spring,dc=org")
			.withConfiguration(AutoConfigurations.of(LdapAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasSingleBean(LdapTemplate.class);
				LdapTemplate ldapTemplate = context.getBean(LdapTemplate.class);
				assertThat(ldapTemplate.list("ou=company1,c=Sweden,dc=spring,dc=org")).hasSize(4);
			});
	}

	@Test
	void testDisableSchemaValidation() {
		this.contextRunner
			.withPropertyValues("spring.ldap.embedded.validation.enabled:false",
					"spring.ldap.embedded.base-dn:dc=spring,dc=org")
			.run((context) -> {
				InMemoryDirectoryServer server = context.getBean(InMemoryDirectoryServer.class);
				assertThat(server.getSchema()).isNull();
			});
	}

	@Test
	@WithResource(name = "custom-schema.ldif", content = """
			dn: cn=schema
			attributeTypes: ( 1.3.6.1.4.1.32473.1.1.1
			  NAME 'exampleAttributeName'
			  DESC 'An example attribute type definition'
			  EQUALITY caseIgnoreMatch
			  ORDERING caseIgnoreOrderingMatch
			  SUBSTR caseIgnoreSubstringsMatch
			  SYNTAX 1.3.6.1.4.1.1466.115.121.1.15
			  SINGLE-VALUE
			  X-ORIGIN 'Managing Schema Document' )
			objectClasses: ( 1.3.6.1.4.1.32473.1.2.2
			  NAME 'exampleAuxiliaryClass'
			  DESC 'An example auxiliary object class definition'
			  SUP top
			  AUXILIARY
			  MAY exampleAttributeName
			  X-ORIGIN 'Managing Schema Document' )
			""")
	@WithResource(name = "custom-schema-sample.ldif", content = """
			dn: dc=spring,dc=org
			objectclass: top
			objectclass: domain
			objectclass: extensibleObject
			objectClass: exampleAuxiliaryClass
			dc: spring
			exampleAttributeName: exampleAttributeName
			""")
	void testCustomSchemaValidation() {
		this.contextRunner
			.withPropertyValues("spring.ldap.embedded.validation.schema:classpath:custom-schema.ldif",
					"spring.ldap.embedded.ldif:classpath:custom-schema-sample.ldif",
					"spring.ldap.embedded.base-dn:dc=spring,dc=org")
			.run((context) -> {
				InMemoryDirectoryServer server = context.getBean(InMemoryDirectoryServer.class);

				assertThat(server.getSchema().getObjectClass("exampleAuxiliaryClass")).isNotNull();
				assertThat(server.getSchema().getAttributeType("exampleAttributeName")).isNotNull();
			});
	}

	@Test
	@WithResource(name = "schema-multi-basedn.ldif", content = """
			dn: dc=spring,dc=org
			objectclass: top
			objectclass: domain
			objectclass: extensibleObject
			dc: spring

			dn: ou=groups,dc=spring,dc=org
			objectclass: top
			objectclass: organizationalUnit
			ou: groups

			dn: cn=ROLE_USER,ou=groups,dc=spring,dc=org
			objectclass: top
			objectclass: groupOfUniqueNames
			cn: ROLE_USER
			uniqueMember: cn=Some Person,ou=company1,c=Sweden,dc=spring,dc=org
			uniqueMember: cn=Some Person2,ou=company1,c=Sweden,dc=spring,dc=org
			uniqueMember: cn=Some Person,ou=company1,c=Sweden,dc=spring,dc=org
			uniqueMember: cn=Some Person3,ou=company1,c=Sweden,dc=spring,dc=org

			dn: cn=ROLE_ADMIN,ou=groups,dc=spring,dc=org
			objectclass: top
			objectclass: groupOfUniqueNames
			cn: ROLE_ADMIN
			uniqueMember: cn=Some Person2,ou=company1,c=Sweden,dc=spring,dc=org

			dn: c=Sweden,dc=spring,dc=org
			objectclass: top
			objectclass: country
			c: Sweden
			description: The country of Sweden

			dn: ou=company1,c=Sweden,dc=spring,dc=org
			objectclass: top
			objectclass: organizationalUnit
			ou: company1
			description: First company in Sweden

			dn: cn=Some Person,ou=company1,c=Sweden,dc=spring,dc=org
			objectclass: top
			objectclass: person
			objectclass: organizationalPerson
			objectclass: inetOrgPerson
			uid: some.person
			userPassword: password
			cn: Some Person
			sn: Person
			description: Sweden, Company1, Some Person
			telephoneNumber: +46 555-123456

			dn: cn=Some Person2,ou=company1,c=Sweden,dc=spring,dc=org
			objectclass: top
			objectclass: person
			objectclass: organizationalPerson
			objectclass: inetOrgPerson
			uid: some.person2
			userPassword: password
			cn: Some Person2
			sn: Person2
			description: Sweden, Company1, Some Person2
			telephoneNumber: +46 555-654321

			dn: cn=Some Person3,ou=company1,c=Sweden,dc=spring,dc=org
			objectclass: top
			objectclass: person
			objectclass: organizationalPerson
			objectclass: inetOrgPerson
			uid: some.person3
			userPassword: password
			cn: Some Person3
			sn: Person3
			description: Sweden, Company1, Some Person3
			telephoneNumber: +46 555-123654

			dn: cn=Some Person4,ou=company1,c=Sweden,dc=spring,dc=org
			objectclass: top
			objectclass: person
			objectclass: organizationalPerson
			objectclass: inetOrgPerson
			uid: some.person4
			userPassword: password
			cn: Some Person
			sn: Person
			description: Sweden, Company1, Some Person
			telephoneNumber: +46 555-456321

			dn: dc=vmware,dc=com
			objectclass: top
			objectclass: domain
			objectclass: extensibleObject
			dc: vmware

			dn: ou=groups,dc=vmware,dc=com
			objectclass: top
			objectclass: organizationalUnit
			ou: groups

			dn: c=Sweden,dc=vmware,dc=com
			objectclass: top
			objectclass: country
			c: Sweden
			description:The country of Sweden

			dn: cn=Some Random Person,c=Sweden,dc=vmware,dc=com
			objectclass: top
			objectclass: person
			objectclass: organizationalPerson
			objectclass: inetOrgPerson
			uid: some.random.person
			userPassword: password
			cn: Some Random Person
			sn: Person
			description: Sweden, VMware, Some Random Person
			telephoneNumber: +46 555-123456
			""")
	void testMultiBaseDn() {
		this.contextRunner.withPropertyValues("spring.ldap.embedded.ldif:classpath:schema-multi-basedn.ldif",
				"spring.ldap.embedded.base-dn[0]:dc=spring,dc=org", "spring.ldap.embedded.base-dn[1]:dc=vmware,dc=com")
			.run((context) -> {
				InMemoryDirectoryServer server = context.getBean(InMemoryDirectoryServer.class);
				assertThat(server.countEntriesBelow("ou=company1,c=Sweden,dc=spring,dc=org")).isEqualTo(5);
				assertThat(server.countEntriesBelow("c=Sweden,dc=vmware,dc=com")).isEqualTo(2);
			});
	}

	@Test
	void ldapContextSourceWithCredentialsIsCreated() {
		this.contextRunner.withPropertyValues("spring.ldap.embedded.base-dn:dc=spring,dc=org",
				"spring.ldap.embedded.credential.username:uid=root", "spring.ldap.embedded.credential.password:boot")
			.run((context) -> {
				LdapContextSource ldapContextSource = context.getBean(LdapContextSource.class);
				assertThat(ldapContextSource.getUrls()).isNotEmpty();
				assertThat(ldapContextSource.getUserDn()).isEqualTo("uid=root");
			});
	}

	@Test
	void ldapContextSourceWithoutCredentialsIsCreated() {
		this.contextRunner.withPropertyValues("spring.ldap.embedded.base-dn:dc=spring,dc=org").run((context) -> {
			LdapContextSource ldapContextSource = context.getBean(LdapContextSource.class);
			assertThat(ldapContextSource.getUrls()).isNotEmpty();
			assertThat(ldapContextSource.getUserDn()).isEmpty();
		});
	}

	@Test
	void ldapContextWithoutSpringLdapIsNotCreated() {
		this.contextRunner.withPropertyValues("spring.ldap.embedded.base-dn:dc=spring,dc=org")
			.withClassLoader(new FilteredClassLoader(ContextSource.class))
			.run((context) -> {
				assertThat(context).hasNotFailed();
				assertThat(context).doesNotHaveBean(LdapContextSource.class);
			});
	}

	@Test
	void ldapContextIsCreatedWithBase() {
		this.contextRunner
			.withPropertyValues("spring.ldap.embedded.base-dn:dc=spring,dc=org", "spring.ldap.base:dc=spring,dc=org")
			.run((context) -> {
				LdapContextSource ldapContextSource = context.getBean(LdapContextSource.class);
				assertThat(ldapContextSource.getBaseLdapPathAsString()).isEqualTo("dc=spring,dc=org");
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class LdapClientConfiguration {

		@Bean
		LDAPConnection ldapConnection(@Value("${local.ldap.port}") int port) throws LDAPException {
			LDAPConnection con = new LDAPConnection();
			con.connect("localhost", port);
			return con;
		}

	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@WithResource(name = "schema.ldif", content = """
			dn: dc=spring,dc=org
			objectclass: top
			objectclass: domain
			objectclass: extensibleObject
			dc: spring

			dn: ou=groups,dc=spring,dc=org
			objectclass: top
			objectclass: organizationalUnit
			ou: groups

			dn: cn=ROLE_USER,ou=groups,dc=spring,dc=org
			objectclass: top
			objectclass: groupOfUniqueNames
			cn: ROLE_USER
			uniqueMember: cn=Some Person,ou=company1,c=Sweden,dc=spring,dc=org
			uniqueMember: cn=Some Person2,ou=company1,c=Sweden,dc=spring,dc=org
			uniqueMember: cn=Some Person,ou=company1,c=Sweden,dc=spring,dc=org
			uniqueMember: cn=Some Person3,ou=company1,c=Sweden,dc=spring,dc=org

			dn: cn=ROLE_ADMIN,ou=groups,dc=spring,dc=org
			objectclass: top
			objectclass: groupOfUniqueNames
			cn: ROLE_ADMIN
			uniqueMember: cn=Some Person2,ou=company1,c=Sweden,dc=spring,dc=org

			dn: c=Sweden,dc=spring,dc=org
			objectclass: top
			objectclass: country
			c: Sweden
			description: The country of Sweden

			dn: ou=company1,c=Sweden,dc=spring,dc=org
			objectclass: top
			objectclass: organizationalUnit
			ou: company1
			description: First company in Sweden

			dn: cn=Some Person,ou=company1,c=Sweden,dc=spring,dc=org
			objectclass: top
			objectclass: person
			objectclass: organizationalPerson
			objectclass: inetOrgPerson
			uid: some.person
			userPassword: password
			cn: Some Person
			sn: Person
			description: Sweden, Company1, Some Person
			telephoneNumber: +46 555-123456

			dn: cn=Some Person2,ou=company1,c=Sweden,dc=spring,dc=org
			objectclass: top
			objectclass: person
			objectclass: organizationalPerson
			objectclass: inetOrgPerson
			uid: some.person2
			userPassword: password
			cn: Some Person2
			sn: Person2
			description: Sweden, Company1, Some Person2
			telephoneNumber: +46 555-654321

			dn: cn=Some Person3,ou=company1,c=Sweden,dc=spring,dc=org
			objectclass: top
			objectclass: person
			objectclass: organizationalPerson
			objectclass: inetOrgPerson
			uid: some.person3
			userPassword: password
			cn: Some Person3
			sn: Person3
			description: Sweden, Company1, Some Person3
			telephoneNumber: +46 555-123654

			dn: cn=Some Person4,ou=company1,c=Sweden,dc=spring,dc=org
			objectclass: top
			objectclass: person
			objectclass: organizationalPerson
			objectclass: inetOrgPerson
			uid: some.person4
			userPassword: password
			cn: Some Person
			sn: Person
			description: Sweden, Company1, Some Person
			telephoneNumber: +46 555-456321
			""")
	@interface WithSchemaLdifResource {

	}

}
