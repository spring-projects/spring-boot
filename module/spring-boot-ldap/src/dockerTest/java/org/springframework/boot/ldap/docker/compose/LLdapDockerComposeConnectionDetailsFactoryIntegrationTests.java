/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ldap.docker.compose;

import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.ldap.autoconfigure.LdapConnectionDetails;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link LLdapDockerComposeConnectionDetailsFactory}.
 *
 * @author Eddú Meléndez
 */
class LLdapDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "lldap-compose.yaml", image = TestImage.LLDAP)
	void runCreatesConnectionDetails(LdapConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getUsername()).isEqualTo("cn=admin,ou=people,dc=springframework,dc=org");
		assertThat(connectionDetails.getPassword()).isEqualTo("somepassword");
		assertThat(connectionDetails.getBase()).isEqualTo("dc=springframework,dc=org");
		assertThat(connectionDetails.getUrls()).hasSize(1);
		assertThat(connectionDetails.getUrls()[0]).startsWith("ldap://");
		assertThat(connectionDetails.getSslBundle()).isNull();
	}

	@DockerComposeTest(composeFile = "lldap-ssl-compose.yaml", image = TestImage.LLDAP,
			additionalResources = { "ca.crt", "server.crt", "server.key" })
	void runWithSslCreatesConnectionDetails(LdapConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getUrls()).hasSize(1);
		assertThat(connectionDetails.getUrls()[0]).startsWith("ldaps://");
		SslBundle sslBundle = connectionDetails.getSslBundle();
		assertThat(sslBundle).isNotNull();
		assertThat(sslBundle.createSslContext()).isNotNull();
	}

}
