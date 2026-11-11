/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.authorization.event.AuthorizationEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AuthorizationAuditListener}.
 */
class AuthorizationAuditListenerTests {

	private final AuthorizationAuditListener listener = new AuthorizationAuditListener();

	private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

	@BeforeEach
	void init() {
		this.listener.setApplicationEventPublisher(this.publisher);
	}

	@Test
	void authorizationDeniedEvent() {
		AuthorizationResult decision = new AuthorizationDecision(false);
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken("spring",
				"password");
		authentication.setDetails("details");
		AuthorizationDeniedEvent<?> authorizationEvent = new AuthorizationDeniedEvent<>(() -> authentication, "",
				decision);
		AuditEvent auditEvent = handleAuthorizationEvent(authorizationEvent).getAuditEvent();
		assertThat(auditEvent.getPrincipal()).isEqualTo("spring");
		assertThat(auditEvent.getType()).isEqualTo(AuthorizationAuditListener.AUTHORIZATION_FAILURE);
		assertThat(auditEvent.getData()).containsEntry("details", "details");
	}

	@Test
	void authorizationDeniedEventWhenAuthenticationIsNotAvailable() {
		AuthorizationResult decision = new AuthorizationDecision(false);
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken("spring",
				"password");
		authentication.setDetails("details");
		AuthorizationDeniedEvent<?> authorizationEvent = new AuthorizationDeniedEvent<>(() -> {
			throw new RuntimeException("No authentication");
		}, "", decision);
		AuditEvent auditEvent = handleAuthorizationEvent(authorizationEvent).getAuditEvent();
		assertThat(auditEvent.getPrincipal()).isEqualTo("<unknown>");
		assertThat(auditEvent.getType()).isEqualTo(AuthorizationAuditListener.AUTHORIZATION_FAILURE);
		assertThat(auditEvent.getData()).doesNotContainKey("details");
	}

	@Test
	void authorizationDeniedEventWhenAuthenticationDoesNotHaveDetails() {
		AuthorizationResult decision = new AuthorizationDecision(false);
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken("spring",
				"password");
		AuthorizationDeniedEvent<?> authorizationEvent = new AuthorizationDeniedEvent<>(() -> authentication, "",
				decision);
		AuditEvent auditEvent = handleAuthorizationEvent(authorizationEvent).getAuditEvent();
		assertThat(auditEvent.getPrincipal()).isEqualTo("spring");
		assertThat(auditEvent.getType()).isEqualTo(AuthorizationAuditListener.AUTHORIZATION_FAILURE);
		assertThat(auditEvent.getData()).doesNotContainKey("details");
	}

	private AuditApplicationEvent handleAuthorizationEvent(AuthorizationEvent event) {
		ArgumentCaptor<AuditApplicationEvent> eventCaptor = ArgumentCaptor.forClass(AuditApplicationEvent.class);
		this.listener.onApplicationEvent(event);
		then(this.publisher).should().publishEvent(eventCaptor.capture());
		return eventCaptor.getValue();
	}

}
