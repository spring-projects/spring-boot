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

package org.springframework.boot.autoconfigure.ldap.embedded;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.LdapTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EmbeddedLdapAutoConfiguration}
 *
 * @author Eddú Meléndez
 */
public class EmbeddedLdapAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(EmbeddedLdapAutoConfiguration.class));

	@Test
	public void testSetDefaultPort() {
		this.contextRunner
				.withPropertyValues("spring.ldap.embedded.port:1234",
						"spring.ldap.embedded.base-dn:dc=spring,dc=org")
				.run((context) -> {
					InMemoryDirectoryServer server = context
							.getBean(InMemoryDirectoryServer.class);
					assertThat(server.getListenPort()).isEqualTo(1234);
				});
	}

	@Test
	public void testRandomPortWithEnvironment() {
		this.contextRunner
				.withPropertyValues("spring.ldap.embedded.base-dn:dc=spring,dc=org")
				.run((context) -> {
					InMemoryDirectoryServer server = context
							.getBean(InMemoryDirectoryServer.class);
					assertThat(server.getListenPort()).isEqualTo(context.getEnvironment()
							.getProperty("local.ldap.port", Integer.class));
				});
	}

	@Test
	public void testRandomPortWithValueAnnotation() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.ldap.embedded.base-dn:dc=spring,dc=org")
				.applyTo(context);
		context.register(EmbeddedLdapAutoConfiguration.class,
				LdapClientConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		context.refresh();
		LDAPConnection connection = context.getBean(LDAPConnection.class);
		assertThat(connection.getConnectedPort()).isEqualTo(
				context.getEnvironment().getProperty("local.ldap.port", Integer.class));
	}

	@Test
	public void testSetCredentials() {
		this.contextRunner
				.withPropertyValues("spring.ldap.embedded.base-dn:dc=spring,dc=org",
						"spring.ldap.embedded.credential.username:uid=root",
						"spring.ldap.embedded.credential.password:boot")
				.run((context) -> {
					InMemoryDirectoryServer server = context
							.getBean(InMemoryDirectoryServer.class);
					BindResult result = server.bind("uid=root", "boot");
					assertThat(result).isNotNull();
				});
	}

	@Test
	public void testSetPartitionSuffix() {
		this.contextRunner
				.withPropertyValues("spring.ldap.embedded.base-dn:dc=spring,dc=org")
				.run((context) -> {
					InMemoryDirectoryServer server = context
							.getBean(InMemoryDirectoryServer.class);
					assertThat(server.getBaseDNs())
							.containsExactly(new DN("dc=spring,dc=org"));
				});
	}

	@Test
	public void testSetLdifFile() {
		this.contextRunner
				.withPropertyValues("spring.ldap.embedded.base-dn:dc=spring,dc=org")
				.run((context) -> {
					InMemoryDirectoryServer server = context
							.getBean(InMemoryDirectoryServer.class);
					assertThat(server
							.countEntriesBelow("ou=company1,c=Sweden,dc=spring,dc=org"))
									.isEqualTo(5);
				});
	}

	@Test
	public void testQueryEmbeddedLdap() {
		this.contextRunner
				.withPropertyValues("spring.ldap.embedded.base-dn:dc=spring,dc=org")
				.withConfiguration(AutoConfigurations.of(LdapAutoConfiguration.class))
				.run((context) -> {
					assertThat(context.getBeanNamesForType(LdapTemplate.class).length)
							.isEqualTo(1);
					LdapTemplate ldapTemplate = context.getBean(LdapTemplate.class);
					assertThat(ldapTemplate.list("ou=company1,c=Sweden,dc=spring,dc=org"))
							.hasSize(4);
				});
	}

	@Test
	public void testDisableSchemaValidation() {
		this.contextRunner
				.withPropertyValues("spring.ldap.embedded.validation.enabled:false",
						"spring.ldap.embedded.base-dn:dc=spring,dc=org")
				.run((context) -> {
					InMemoryDirectoryServer server = context
							.getBean(InMemoryDirectoryServer.class);
					assertThat(server.getSchema()).isNull();
				});
	}

	@Test
	public void testCustomSchemaValidation() {
		this.contextRunner.withPropertyValues(
				"spring.ldap.embedded.validation.schema:classpath:custom-schema.ldif",
				"spring.ldap.embedded.ldif:classpath:custom-schema-sample.ldif",
				"spring.ldap.embedded.base-dn:dc=spring,dc=org").run((context) -> {
					InMemoryDirectoryServer server = context
							.getBean(InMemoryDirectoryServer.class);

					assertThat(server.getSchema().getObjectClass("exampleAuxiliaryClass"))
							.isNotNull();
					assertThat(
							server.getSchema().getAttributeType("exampleAttributeName"))
									.isNotNull();
				});
	}

	@Test
	public void testMultiBaseDn() {
		this.contextRunner
				.withPropertyValues(
						"spring.ldap.embedded.ldif:classpath:schema-multi-basedn.ldif",
						"spring.ldap.embedded.base-dn[0]:dc=spring,dc=org",
						"spring.ldap.embedded.base-dn[1]:dc=pivotal,dc=io")
				.run((context) -> {
					InMemoryDirectoryServer server = context
							.getBean(InMemoryDirectoryServer.class);
					assertThat(server
							.countEntriesBelow("ou=company1,c=Sweden,dc=spring,dc=org"))
									.isEqualTo(5);
					assertThat(server.countEntriesBelow("c=Sweden,dc=pivotal,dc=io"))
							.isEqualTo(2);
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class LdapClientConfiguration {

		@Bean
		public LDAPConnection ldapConnection(@Value("${local.ldap.port}") int port)
				throws LDAPException {
			LDAPConnection con = new LDAPConnection();
			con.connect("localhost", port);
			return con;
		}

	}

}
