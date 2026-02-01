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

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.ldap.core.LdapTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Docker-based integration tests for LDAP SSL configuration.
 *
 * @author Massimo Deiana
 */
@Testcontainers
class LdapSslDockerTests {

	private static final String LDAP_IMAGE = "osixia/openldap:1.5.0";

	@Container
	static final GenericContainer<?> ldapContainer = new GenericContainer<>(LDAP_IMAGE)
		.withEnv("LDAP_ORGANISATION", "Example Inc")
		.withEnv("LDAP_DOMAIN", "example.org")
		.withEnv("LDAP_ADMIN_PASSWORD", "admin")
		.withEnv("LDAP_TLS", "true")
		.withEnv("LDAP_TLS_VERIFY_CLIENT", "never")
		.withExposedPorts(389, 636)
		.waitingFor(Wait.forLogMessage(".*slapd starting.*", 1).withStartupTimeout(Duration.ofMinutes(2)));

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(LdapAutoConfiguration.class, SslAutoConfiguration.class));

	@Test
	void ldapConnectionWithoutSsl() {
		this.contextRunner
			.withPropertyValues("spring.ldap.urls=ldap://localhost:" + ldapContainer.getMappedPort(389),
					"spring.ldap.base=dc=example,dc=org", "spring.ldap.username=cn=admin,dc=example,dc=org",
					"spring.ldap.password=admin")
			.run((context) -> {
				assertThat(context).hasSingleBean(LdapTemplate.class);
				LdapTemplate ldapTemplate = context.getBean(LdapTemplate.class);
				Object result = ldapTemplate.lookup("");
				assertThat(result).isNotNull();
			});
	}

	@Test
	void ldapConnectionWithStartTlsConfiguration() {
		this.contextRunner
			.withPropertyValues("spring.ldap.urls=ldap://localhost:" + ldapContainer.getMappedPort(389),
					"spring.ldap.base=dc=example,dc=org", "spring.ldap.username=cn=admin,dc=example,dc=org",
					"spring.ldap.password=admin")
			.run((context) -> {
				assertThat(context).hasSingleBean(LdapTemplate.class);
				LdapTemplate ldapTemplate = context.getBean(LdapTemplate.class);
				Object result = ldapTemplate.lookup("");
				assertThat(result).isNotNull();
			});
	}

}
