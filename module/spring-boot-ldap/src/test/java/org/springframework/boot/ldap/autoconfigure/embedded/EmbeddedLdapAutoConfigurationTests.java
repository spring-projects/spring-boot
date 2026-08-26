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

package org.springframework.boot.ldap.autoconfigure.embedded;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.schema.Schema;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.ldap.autoconfigure.LdapAutoConfiguration;
import org.springframework.boot.ldap.autoconfigure.LdapConnectionDetails;
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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link EmbeddedLdapAutoConfiguration}
 *
 * @author Eddú Meléndez
 */
class EmbeddedLdapAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(EmbeddedLdapAutoConfiguration.class, LdapAutoConfiguration.class,
				SslAutoConfiguration.class));

	@Test
	void testSetDefaultPort() {
		this.contextRunner
			.withPropertyValues("spring.ldap.embedded.port:1234", "spring.ldap.embedded.base-dn:dc=spring,dc=org")
			.run((context) -> {
				InMemoryDirectoryServer server = context.getBean(InMemoryDirectoryServer.class);
				assertThat(server.getListenPort()).isEqualTo(1234);
				InMemoryListenerConfig config = server.getConfig().getListenerConfigs().get(0);
				assertThat(config.getListenerName()).isEqualTo("LDAP");
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
		this.contextRunner.withPropertyValues("spring.ldap.embedded.base-dn:dc=spring,dc=org").run((context) -> {
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
				Schema schema = server.getSchema();
				assertThat(schema).isNotNull();
				assertThat(schema.getObjectClass("exampleAuxiliaryClass")).isNotNull();
				assertThat(schema.getAttributeType("exampleAttributeName")).isNotNull();
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

	@Test
	void shouldConfigureLdapsListenerWhenSslBundleIsConfigured() {
		this.contextRunner.withPropertyValues(sslBundleProperties("spring.ldap.embedded.ssl.bundle:test"))
			.run((context) -> {
				InMemoryDirectoryServer server = context.getBean(InMemoryDirectoryServer.class);
				assertThat(server.getConfig().getListenerConfigs().size()).isEqualTo(1);
				InMemoryListenerConfig config = server.getConfig().getListenerConfigs().get(0);
				assertThat(config.getListenerName()).isEqualTo("LDAPS");
				assertThat(server.getConnection("LDAPS").getSSLSession()).isNotNull();
			});
	}

	@Test
	void shouldConfigureLdapListenerWhenSslBundleIsConfiguredButSslIsDisabled() {
		this.contextRunner
			.withPropertyValues(sslBundleProperties("spring.ldap.embedded.ssl.enabled:false",
					"spring.ldap.embedded.ssl.bundle:test"))
			.run((context) -> {
				InMemoryDirectoryServer server = context.getBean(InMemoryDirectoryServer.class);
				assertThat(server.getConfig().getListenerConfigs().size()).isEqualTo(1);
				InMemoryListenerConfig config = server.getConfig().getListenerConfigs().get(0);
				assertThat(config.getListenerName()).isEqualTo("LDAP");
			});
	}

	@Test
	void shouldFailWhenInvalidSslBundleIsConfigured() {
		this.contextRunner
			.withPropertyValues(
					sslBundleProperties("spring.ldap.embedded.ssl.enabled:true", "spring.ldap.embedded.ssl.bundle:foo"))
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context).getFailure().hasMessageContaining("foo");
				assertThat(context).getFailure().hasMessageContaining("cannot be found");
			});
	}

	@Test
	void shouldFailWhenSslIsEnabledWithoutAnSslBundle() {
		this.contextRunner
			.withPropertyValues("spring.ldap.embedded.port:0", "spring.ldap.embedded.base-dn:dc=spring,dc=org",
					"spring.ldap.embedded.ssl.enabled:true")
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context).getFailure().hasMessageContaining("SSL is enabled but no SSL bundle has been set");
			});
	}

	@Test
	@WithSchemaLdifResource
	void shouldConnectOverLdapsWhenSslBundleIsConfigured() {
		this.contextRunner.withPropertyValues(sslBundleProperties("spring.ldap.embedded.ssl.bundle:test"))
			.run((context) -> {
				LdapContextSource contextSource = context.getBean(LdapContextSource.class);
				assertThat(contextSource.getUrls()).allMatch((url) -> url.startsWith("ldaps://"));
				LdapTemplate ldapTemplate = context.getBean(LdapTemplate.class);
				assertThat(ldapTemplate.list("ou=company1,c=Sweden,dc=spring,dc=org")).hasSize(4);
			});
	}

	@Test
	@WithSchemaLdifResource
	void shouldIgnoreClientSslPropertiesMeantForAnotherServer() {
		this.contextRunner
			.withPropertyValues(sslBundleProperties("spring.ldap.embedded.ssl.bundle:test",
					"spring.ldap.urls:ldaps://ldap.example.com:636", "spring.ldap.ssl.bundle:does-not-exist"))
			.run((context) -> {
				LdapContextSource contextSource = context.getBean(LdapContextSource.class);
				assertThat(contextSource.getUrls()).allMatch((url) -> url.startsWith("ldaps://localhost:"));
				LdapTemplate ldapTemplate = context.getBean(LdapTemplate.class);
				assertThat(ldapTemplate.list("ou=company1,c=Sweden,dc=spring,dc=org")).hasSize(4);
			});
	}

	@Test
	void shouldFailWhenSslBundleIsConfiguredAndSocketFactoryIsSetInBaseEnvironment() {
		this.contextRunner
			.withPropertyValues(sslBundleProperties("spring.ldap.embedded.ssl.bundle:test",
					"spring.ldap.baseEnvironment.java.naming.ldap.factory.socket=com.example.MySocketFactory"))
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context).getFailure()
					.hasMessageContaining("Use either an SSL bundle or your own socket factory, not both");
			});
	}

	@Test
	void shouldAllowSocketFactoryInBaseEnvironmentWhenContextSourceIsUserDefined() {
		this.contextRunner
			.withPropertyValues(sslBundleProperties("spring.ldap.embedded.ssl.bundle:test",
					"spring.ldap.baseEnvironment.java.naming.ldap.factory.socket=com.example.MySocketFactory"))
			.withBean("ldapContextSource", LdapContextSource.class, () -> {
				LdapContextSource contextSource = new LdapContextSource();
				contextSource.setUrls(new String[] { "ldaps://localhost:636" });
				return contextSource;
			})
			.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	@WithSchemaLdifResource
	void shouldRegisterHintsForSchemaLdif() {
		RuntimeHints runtimeHints = new RuntimeHints();
		new EmbeddedLdapAutoConfiguration.EmbeddedLdapAutoConfigurationRuntimeHints().registerHints(runtimeHints,
				Thread.currentThread().getContextClassLoader());
		assertThat(RuntimeHintsPredicates.resource().forResource("schema.ldif")).accepts(runtimeHints);
	}

	@Test
	void shouldApplyClientPropertiesThatTheServerDoesNotDecide() {
		this.contextRunner
			.withPropertyValues("spring.ldap.embedded.base-dn:dc=spring,dc=org", "spring.ldap.referral:ignore",
					"spring.ldap.anonymous-read-only:true",
					"spring.ldap.baseEnvironment.java.naming.security.authentication:DIGEST-MD5")
			.run((context) -> {
				LdapContextSource contextSource = context.getBean(LdapContextSource.class);
				assertThat(contextSource).hasFieldOrPropertyWithValue("referral", "ignore");
				assertThat(contextSource.isAnonymousReadOnly()).isTrue();
				assertThat(contextSource).extracting("anonymousEnv", InstanceOfAssertFactories.MAP)
					.containsEntry("java.naming.security.authentication", "DIGEST-MD5");
			});
	}

	@Test
	void shouldBackOffWhenLdapConnectionDetailsBeanIsDefined() {
		this.contextRunner.withPropertyValues("spring.ldap.embedded.base-dn:dc=spring,dc=org")
			.withUserConfiguration(LdapConnectionDetailsConfiguration.class)
			.run((context) -> {
				assertThat(context).doesNotHaveBean(EmbeddedLdapConnectionDetails.class);
				LdapContextSource contextSource = context.getBean(LdapContextSource.class);
				assertThat(contextSource.getUrls()).containsExactly("ldap://ldap.example.com:389");
			});
	}

	@Test
	void sslIsNotEnabledWhenBundleIsEmpty() {
		EmbeddedLdapProperties properties = new EmbeddedLdapProperties();
		properties.getSsl().setBundle("");
		assertThat(properties.getSsl().isEnabled()).isFalse();
	}

	@Test
	@WithSchemaLdifResource
	void authenticationRequiredOperationTypesAreApplied() {
		this.contextRunner.withPropertyValues("spring.ldap.embedded.base-dn=dc=spring,dc=org",
				"spring.ldap.embedded.credential.username=uid=root", "spring.ldap.embedded.credential.password=boot",
				"spring.ldap.embedded.authentication-required-operation-types=search")
			.run((context) -> {
				InMemoryDirectoryServer server = context.getBean(InMemoryDirectoryServer.class);
				try (LDAPConnection connection = new LDAPConnection("localhost", server.getListenPort())) {
					SearchRequest searchRequest = new SearchRequest("dc=spring,dc=org", SearchScope.SUB,
							"(objectClass=*)");

					assertThatExceptionOfType(LDAPException.class).isThrownBy(() -> connection.search(searchRequest))
						.satisfies((ex) -> assertThat(ex.getResultCode())
							.isEqualTo(ResultCode.INSUFFICIENT_ACCESS_RIGHTS));

					connection.bind("uid=root", "boot");
					assertThat(connection.search(searchRequest).getEntryCount()).isGreaterThan(0);
				}
			});
	}

	private String[] sslBundleProperties(String... additionalProperties) {
		String location = "classpath:org/springframework/boot/ldap/autoconfigure/embedded/";
		List<String> propertyValues = new ArrayList<>();
		propertyValues.add("spring.ssl.bundle.jks.test.keystore.password=secret");
		propertyValues.add("spring.ssl.bundle.jks.test.keystore.location=" + location + "localhost.jks");
		propertyValues.add("spring.ssl.bundle.jks.test.truststore.password=secret");
		propertyValues.add("spring.ssl.bundle.jks.test.truststore.location=" + location + "localhost.jks");
		propertyValues.add("spring.ssl.bundle.jks.test.key.alias=spring-boot");
		propertyValues.add("spring.ssl.bundle.jks.test.key.password=password");
		propertyValues.add("spring.ssl.bundle.jks.test.protocol=TLSv1.2");
		propertyValues.add("spring.ldap.embedded.port:0");
		propertyValues.add("spring.ldap.embedded.base-dn:dc=spring,dc=org");
		propertyValues.addAll(List.of(additionalProperties));
		return propertyValues.toArray(String[]::new);
	}

	@Configuration(proxyBeanMethods = false)
	static class LdapConnectionDetailsConfiguration {

		@Bean
		LdapConnectionDetails ldapConnectionDetails() {
			return () -> new String[] { "ldap://ldap.example.com:389" };
		}

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
