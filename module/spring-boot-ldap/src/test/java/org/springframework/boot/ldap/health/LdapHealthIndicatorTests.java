/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ldap.health;

import org.junit.jupiter.api.Test;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.ldap.CommunicationException;
import org.springframework.ldap.core.ContextExecutor;
import org.springframework.ldap.core.LdapTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LdapHealthIndicator}
 *
 * @author Eddú Meléndez
 */
class LdapHealthIndicatorTests {

	@Test
	@SuppressWarnings("unchecked")
	void ldapIsUp() {
		LdapTemplate ldapTemplate = mock(LdapTemplate.class);
		given(ldapTemplate.executeReadOnly((ContextExecutor<String>) any())).willReturn("3");
		LdapHealthIndicator healthIndicator = new LdapHealthIndicator(ldapTemplate);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("version", "3");
		then(ldapTemplate).should().executeReadOnly((ContextExecutor<String>) any());
	}

	@Test
	@SuppressWarnings("unchecked")
	void ldapIsUpWhenVersionIsUnavailable() {
		LdapTemplate ldapTemplate = mock(LdapTemplate.class);
		given(ldapTemplate.executeReadOnly((ContextExecutor<String>) any())).willReturn(null);
		LdapHealthIndicator healthIndicator = new LdapHealthIndicator(ldapTemplate);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).doesNotContainKey("version");
		then(ldapTemplate).should().executeReadOnly((ContextExecutor<String>) any());
	}

	@Test
	@SuppressWarnings("unchecked")
	void ldapIsDown() {
		LdapTemplate ldapTemplate = mock(LdapTemplate.class);
		given(ldapTemplate.executeReadOnly((ContextExecutor<String>) any()))
			.willThrow(new CommunicationException(new javax.naming.CommunicationException("Connection failed")));
		LdapHealthIndicator healthIndicator = new LdapHealthIndicator(ldapTemplate);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat((String) health.getDetails().get("error")).contains("Connection failed");
		then(ldapTemplate).should().executeReadOnly((ContextExecutor<String>) any());
	}

}
