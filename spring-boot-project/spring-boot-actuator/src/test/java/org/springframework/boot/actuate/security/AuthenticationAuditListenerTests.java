/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AuthenticationFailureExpiredEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.switchuser.AuthenticationSwitchUserEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link AuthenticationAuditListener}.
 */
class AuthenticationAuditListenerTests {

	private final AuthenticationAuditListener listener = new AuthenticationAuditListener();

	private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

	@BeforeEach
	void init() {
		this.listener.setApplicationEventPublisher(this.publisher);
	}

	@Test
	void testAuthenticationSuccess() {
		AuditApplicationEvent event = handleAuthenticationEvent(
				new AuthenticationSuccessEvent(new UsernamePasswordAuthenticationToken("user", "password")));
		assertThat(event.getAuditEvent().getType()).isEqualTo(AuthenticationAuditListener.AUTHENTICATION_SUCCESS);
	}

	@Test
	void testOtherAuthenticationSuccess() {
		this.listener.onApplicationEvent(new InteractiveAuthenticationSuccessEvent(
				new UsernamePasswordAuthenticationToken("user", "password"), getClass()));
		// No need to audit this one (it shadows a regular AuthenticationSuccessEvent)
		then(this.publisher).should(never()).publishEvent(any(ApplicationEvent.class));
	}

	@Test
	void testAuthenticationFailed() {
		AuditApplicationEvent event = handleAuthenticationEvent(new AuthenticationFailureExpiredEvent(
				new UsernamePasswordAuthenticationToken("user", "password"), new BadCredentialsException("Bad user")));
		assertThat(event.getAuditEvent().getType()).isEqualTo(AuthenticationAuditListener.AUTHENTICATION_FAILURE);
	}

	@Test
	void testAuthenticationSwitch() {
		AuditApplicationEvent event = handleAuthenticationEvent(
				new AuthenticationSwitchUserEvent(new UsernamePasswordAuthenticationToken("user", "password"),
						new User("user", "password", AuthorityUtils.commaSeparatedStringToAuthorityList("USER"))));
		assertThat(event.getAuditEvent().getType()).isEqualTo(AuthenticationAuditListener.AUTHENTICATION_SWITCH);
	}

	@Test
	void testAuthenticationSwitchBackToAnonymous() {
		AuditApplicationEvent event = handleAuthenticationEvent(
				new AuthenticationSwitchUserEvent(new UsernamePasswordAuthenticationToken("user", "password"), null));
		assertThat(event.getAuditEvent().getType()).isEqualTo(AuthenticationAuditListener.AUTHENTICATION_SWITCH);
	}

	@Test
	void testDetailsAreIncludedInAuditEvent() {
		Object details = new Object();
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken("user",
				"password");
		authentication.setDetails(details);
		AuditApplicationEvent event = handleAuthenticationEvent(
				new AuthenticationFailureExpiredEvent(authentication, new BadCredentialsException("Bad user")));
		assertThat(event.getAuditEvent().getType()).isEqualTo(AuthenticationAuditListener.AUTHENTICATION_FAILURE);
		assertThat(event.getAuditEvent().getData()).containsEntry("details", details);
	}

	private AuditApplicationEvent handleAuthenticationEvent(AbstractAuthenticationEvent event) {
		ArgumentCaptor<AuditApplicationEvent> eventCaptor = ArgumentCaptor.forClass(AuditApplicationEvent.class);
		this.listener.onApplicationEvent(event);
		then(this.publisher).should().publishEvent(eventCaptor.capture());
		return eventCaptor.getValue();
	}

}
