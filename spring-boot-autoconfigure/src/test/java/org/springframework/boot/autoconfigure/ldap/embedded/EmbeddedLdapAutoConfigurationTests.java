/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.ldap.embedded;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.ldap.LdapDataAutoConfiguration;
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration;
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

	private AnnotationConfigApplicationContext context;

	@Before
	public void setup() {
		this.context = new AnnotationConfigApplicationContext();
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testSetDefaultPort() throws LDAPException {
		load("spring.ldap.embedded.port:1234",
				"spring.ldap.embedded.base-dn:dc=spring,dc=org");
		InMemoryDirectoryServer server = this.context
				.getBean(InMemoryDirectoryServer.class);
		assertThat(server.getListenPort()).isEqualTo(1234);
	}

	@Test
	public void testRandomPortWithEnvironment() throws LDAPException {
		load("spring.ldap.embedded.base-dn:dc=spring,dc=org");
		InMemoryDirectoryServer server = this.context
				.getBean(InMemoryDirectoryServer.class);
		assertThat(server.getListenPort()).isEqualTo(this.context.getEnvironment()
				.getProperty("local.ldap.port", Integer.class));
	}

	@Test
	public void testRandomPortWithValueAnnotation() throws LDAPException {
		TestPropertyValues.of("spring.ldap.embedded.base-dn:dc=spring,dc=org")
				.applyTo(this.context);
		this.context.register(EmbeddedLdapAutoConfiguration.class,
				LdapClientConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		LDAPConnection connection = this.context.getBean(LDAPConnection.class);
		assertThat(connection.getConnectedPort()).isEqualTo(this.context.getEnvironment()
				.getProperty("local.ldap.port", Integer.class));
	}

	@Test
	public void testSetCredentials() throws LDAPException {
		load("spring.ldap.embedded.base-dn:dc=spring,dc=org",
				"spring.ldap.embedded.credential.username:uid=root",
				"spring.ldap.embedded.credential.password:boot");
		InMemoryDirectoryServer server = this.context
				.getBean(InMemoryDirectoryServer.class);
		BindResult result = server.bind("uid=root", "boot");
		assertThat(result).isNotNull();
	}

	@Test
	public void testSetPartitionSuffix() throws LDAPException {
		load("spring.ldap.embedded.base-dn:dc=spring,dc=org");
		InMemoryDirectoryServer server = this.context
				.getBean(InMemoryDirectoryServer.class);
		assertThat(server.getBaseDNs()).containsExactly(new DN("dc=spring,dc=org"));
	}

	@Test
	public void testSetLdifFile() throws LDAPException {
		load("spring.ldap.embedded.base-dn:dc=spring,dc=org");
		InMemoryDirectoryServer server = this.context
				.getBean(InMemoryDirectoryServer.class);
		assertThat(server.countEntriesBelow("ou=company1,c=Sweden,dc=spring,dc=org"))
				.isEqualTo(5);
	}

	@Test
	public void testQueryEmbeddedLdap() throws LDAPException {
		TestPropertyValues.of("spring.ldap.embedded.base-dn:dc=spring,dc=org")
				.applyTo(this.context);
		this.context.register(EmbeddedLdapAutoConfiguration.class,
				LdapAutoConfiguration.class, LdapDataAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(LdapTemplate.class).length)
				.isEqualTo(1);
		LdapTemplate ldapTemplate = this.context.getBean(LdapTemplate.class);
		assertThat(ldapTemplate.list("ou=company1,c=Sweden,dc=spring,dc=org")).hasSize(4);
	}

	@Test
	public void testDisableSchemaValidation() throws LDAPException {
		load("spring.ldap.embedded.validation.enabled:false",
				"spring.ldap.embedded.base-dn:dc=spring,dc=org");
		InMemoryDirectoryServer server = this.context
				.getBean(InMemoryDirectoryServer.class);
		assertThat(server.getSchema()).isNull();
	}

	@Test
	public void testCustomSchemaValidation() throws LDAPException {
		load("spring.ldap.embedded.validation.schema:classpath:custom-schema.ldif",
				"spring.ldap.embedded.ldif:classpath:custom-schema-sample.ldif",
				"spring.ldap.embedded.base-dn:dc=spring,dc=org");
		InMemoryDirectoryServer server = this.context
				.getBean(InMemoryDirectoryServer.class);

		assertThat(server.getSchema().getObjectClass("exampleAuxiliaryClass"))
				.isNotNull();
		assertThat(server.getSchema().getAttributeType("exampleAttributeName"))
				.isNotNull();
	}

	private void load(String... properties) {
		TestPropertyValues.of(properties).applyTo(this.context);
		this.context.register(EmbeddedLdapAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
	}

	@Configuration
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
