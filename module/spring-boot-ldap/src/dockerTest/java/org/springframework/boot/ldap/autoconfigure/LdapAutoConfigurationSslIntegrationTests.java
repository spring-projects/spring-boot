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

package org.springframework.boot.ldap.autoconfigure;

import java.security.cert.CertPathBuilderException;
import java.security.cert.CertificateException;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testcontainers.service.connection.PemTrustStore;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.Ssl;
import org.springframework.boot.testsupport.container.OpenLdapContainer;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;

/**
 * Integration tests for SSL bundle support in {@link LdapAutoConfiguration}, using a real
 * OpenLDAP server configured for LDAPS.
 *
 * @author Moritz Halbritter
 */
@Testcontainers(disabledWithoutDocker = true)
class LdapAutoConfigurationSslIntegrationTests {

	private static final String CERTIFICATES = "org/springframework/boot/ldap/autoconfigure/ssl/";

	private static final String CA_CERTIFICATE = "classpath:" + CERTIFICATES + "ca.crt";

	// Certificates issued to 'other.example.com' rather than to the host the container is
	// reached on, used to check that the hostname is verified.
	private static final String OTHER_HOST_CERTIFICATES = CERTIFICATES + "otherhost/";

	private static final String OTHER_HOST_CA_CERTIFICATE = "classpath:" + CERTIFICATES + "otherhost/ca.crt";

	// Without a connect timeout, the SSL handshake is performed lazily on the first
	// write, and a certificate validation failure then surfaces as a generic
	// SocketException instead of the underlying cause.
	private static final String CONNECT_TIMEOUT_PROPERTY = "spring.ldap.baseEnvironment.com.sun.jndi.ldap.connect.timeout=5000";

	private static OpenLdapContainer openLdapContainer() {
		return openLdapContainer(CERTIFICATES);
	}

	private static OpenLdapContainer openLdapContainer(String certificates) {
		OpenLdapContainer container = TestImage.container(OpenLdapContainer.class);
		container.addExposedPorts(636);
		return container.withEnv("LDAP_TLS_VERIFY_CLIENT", "never")
			// The image restarts slapd internally once the TLS config has been applied,
			// so the default "port is listening" wait strategy resolves too early.
			.waitingFor(Wait.forLogMessage(".*slapd starting.*\\n", 1))
			.withCopyFileToContainer(MountableFile.forClasspathResource(certificates + "server.crt"),
					"/container/service/slapd/assets/certs/ldap.crt")
			.withCopyFileToContainer(MountableFile.forClasspathResource(certificates + "server.key"),
					"/container/service/slapd/assets/certs/ldap.key")
			.withCopyFileToContainer(MountableFile.forClasspathResource(certificates + "ca.crt"),
					"/container/service/slapd/assets/certs/ca.crt");
	}

	@Nested
	@SpringJUnitConfig
	@TestPropertySource(properties = CONNECT_TIMEOUT_PROPERTY)
	class WhenServerCertificateIsTrusted {

		@Ssl
		@PemTrustStore(CA_CERTIFICATE)
		@Container
		@ServiceConnection
		static final OpenLdapContainer openLdap = openLdapContainer();

		@Autowired
		private LdapTemplate ldapTemplate;

		@Test
		void shouldSearchOverLdaps() {
			List<String> dc = this.ldapTemplate.search(LdapQueryBuilder.query().where("objectclass").is("dcObject"),
					(AttributesMapper<String>) (attributes) -> attributes.get("dc").get().toString());
			assertThat(dc).singleElement().isEqualTo("example");
		}

		@Configuration(proxyBeanMethods = false)
		@ImportAutoConfiguration(LdapAutoConfiguration.class)
		static class TestConfiguration {

		}

	}

	@Nested
	class WhenServerCertificateIsNotTrusted {

		@Container
		private final OpenLdapContainer openLdap = openLdapContainer();

		@Test
		void shouldFailToSearchOverLdaps() {
			ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(LdapAutoConfiguration.class, SslAutoConfiguration.class))
				.withPropertyValues(
						"spring.ldap.urls:ldaps://" + this.openLdap.getHost() + ":" + this.openLdap.getMappedPort(636),
						"spring.ldap.base:dc=example,dc=org", "spring.ldap.username:cn=admin,dc=example,dc=org",
						"spring.ldap.password:admin", "spring.ldap.ssl.enabled:true", CONNECT_TIMEOUT_PROPERTY);
			contextRunner.run((context) -> {
				LdapTemplate ldapTemplate = context.getBean(LdapTemplate.class);
				assertThatException()
					.isThrownBy(() -> ldapTemplate.search(LdapQueryBuilder.query().where("objectclass").is("dcObject"),
							(AttributesMapper<String>) (attributes) -> attributes.get("dc").get().toString()))
					.withRootCauseInstanceOf(CertPathBuilderException.class);
			});
		}

	}

	@Nested
	@SpringJUnitConfig
	@TestPropertySource(properties = CONNECT_TIMEOUT_PROPERTY)
	class WhenServerCertificateIsForADifferentHost {

		@Ssl
		@PemTrustStore(OTHER_HOST_CA_CERTIFICATE)
		@Container
		@ServiceConnection
		static final OpenLdapContainer openLdap = openLdapContainer(OTHER_HOST_CERTIFICATES);

		@Autowired
		private LdapTemplate ldapTemplate;

		@Test
		void shouldFailToSearchOverLdaps() {
			// The certificate chain is trusted, so reaching the server on a host the
			// certificate has not been issued to can only fail on hostname verification.
			assertThatException()
				.isThrownBy(() -> this.ldapTemplate.search(LdapQueryBuilder.query().where("objectclass").is("dcObject"),
						(AttributesMapper<String>) (attributes) -> attributes.get("dc").get().toString()))
				.withRootCauseInstanceOf(CertificateException.class)
				.havingRootCause()
				.withMessageContaining(openLdap.getHost());
		}

		@Configuration(proxyBeanMethods = false)
		@ImportAutoConfiguration(LdapAutoConfiguration.class)
		static class TestConfiguration {

		}

	}

}
