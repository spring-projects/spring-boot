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

package org.springframework.boot.ldap.testcontainers;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.ldap.LLdapContainer;
import org.testcontainers.utility.MountableFile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.ldap.autoconfigure.LdapAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.PemTrustStore;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.Ssl;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the SSL bundle support of {@link LLdapContainerConnectionDetailsFactory},
 * using an LLDAP container configured for LDAPS.
 *
 * @author Moritz Halbritter
 */
@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
class LLdapContainerConnectionDetailsFactorySslIntegrationTests {

	private static final String CERTIFICATES = "org/springframework/boot/ldap/autoconfigure/ssl/";

	private static final int LDAPS_PORT = 6360;

	@Ssl
	@PemTrustStore("classpath:" + CERTIFICATES + "ca.crt")
	@Container
	@ServiceConnection
	static final LLdapContainer lldap = ldapsContainer();

	@Autowired
	private LdapTemplate ldapTemplate;

	private static LLdapContainer ldapsContainer() {
		LLdapContainer container = TestImage.container(LLdapContainer.class);
		container.addExposedPorts(LDAPS_PORT);
		return container.withEnv("LLDAP_LDAPS_OPTIONS__ENABLED", "true")
			.withEnv("LLDAP_LDAPS_OPTIONS__CERT_FILE", "/certs/server.crt")
			.withEnv("LLDAP_LDAPS_OPTIONS__KEY_FILE", "/certs/server.key")
			.withCopyFileToContainer(MountableFile.forClasspathResource(CERTIFICATES + "server.crt"),
					"/certs/server.crt")
			.withCopyFileToContainer(MountableFile.forClasspathResource(CERTIFICATES + "server.key"),
					"/certs/server.key");
	}

	@Test
	void shouldSearchOverLdaps() {
		List<String> cn = this.ldapTemplate.search(LdapQueryBuilder.query().where("objectClass").is("inetOrgPerson"),
				(AttributesMapper<String>) (attributes) -> attributes.get("cn").get().toString());
		assertThat(cn).singleElement().isEqualTo("Administrator");
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(LdapAutoConfiguration.class)
	static class TestConfiguration {

	}

}
