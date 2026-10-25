/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ldap.docker.compose;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.docker.compose.core.ConnectionPorts;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.ldap.docker.compose.LLdapDockerComposeConnectionDetailsFactory.LLdapDockerComposeConnectionDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LLdapDockerComposeConnectionDetailsFactory}.
 *
 * @author Iram Tazim Hoque
 */
class LLdapDockerComposeConnectionDetailsFactoryTests {

	@Test
	void createConnectionDetailsWithDefaults() {
		RunningService runningService = mockRunningService();
		given(runningService.env()).willReturn(Collections.emptyMap());
		LLdapDockerComposeConnectionDetails connectionDetails = new LLdapDockerComposeConnectionDetails(runningService);
		assertThat(connectionDetails.getUsername()).isEqualTo("cn=admin,ou=people,dc=example,dc=com");
		assertThat(connectionDetails.getPassword()).isEqualTo("password");
		assertThat(connectionDetails.getBase()).isEqualTo("dc=example,dc=com");
		assertThat(connectionDetails.getUrls()).containsExactly("ldap://localhost:30001");
	}

	@Test
	void createConnectionDetailsWhenHasUserDn() {
		RunningService runningService = mockRunningService();
		given(runningService.env()).willReturn(Map.of("LLDAP_LDAP_USER_DN", "root"));
		LLdapDockerComposeConnectionDetails connectionDetails = new LLdapDockerComposeConnectionDetails(runningService);
		assertThat(connectionDetails.getUsername()).isEqualTo("cn=root,ou=people,dc=example,dc=com");
	}

	@Test
	void createConnectionDetailsWhenHasUserDnAndBaseDn() {
		RunningService runningService = mockRunningService();
		given(runningService.env())
			.willReturn(Map.of("LLDAP_LDAP_USER_DN", "root", "LLDAP_LDAP_BASE_DN", "dc=springframework,dc=org"));
		LLdapDockerComposeConnectionDetails connectionDetails = new LLdapDockerComposeConnectionDetails(runningService);
		assertThat(connectionDetails.getUsername()).isEqualTo("cn=root,ou=people,dc=springframework,dc=org");
		assertThat(connectionDetails.getBase()).isEqualTo("dc=springframework,dc=org");
	}

	private static RunningService mockRunningService() {
		RunningService service = mock(RunningService.class);
		given(service.labels()).willReturn(Collections.emptyMap());
		ConnectionPorts connectionPorts = mock(ConnectionPorts.class);
		given(service.ports()).willReturn(connectionPorts);
		given(service.host()).willReturn("localhost");
		given(connectionPorts.get(3890)).willReturn(30001);
		return service;
	}

}
