/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.security;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureExpiredEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.switchuser.AuthenticationSwitchUserEvent;

import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link AuthenticationAuditListener}.
 */
public class AuthenticationAuditListenerTests {

	private final AuthenticationAuditListener listener = new AuthenticationAuditListener();

	private final ApplicationEventPublisher publisher = mock(
			ApplicationEventPublisher.class);

	@Before
	public void init() {
		this.listener.setApplicationEventPublisher(this.publisher);
	}

	@Test
	public void testAuthenticationSuccess() {
		this.listener.onApplicationEvent(new AuthenticationSuccessEvent(
				new UsernamePasswordAuthenticationToken("user", "password")));
		verify(this.publisher).publishEvent((ApplicationEvent) anyObject());
	}

	@Test
	public void testOtherAuthenticationSuccess() {
		this.listener.onApplicationEvent(new InteractiveAuthenticationSuccessEvent(
				new UsernamePasswordAuthenticationToken("user", "password"), getClass()));
		// No need to audit this one (it shadows a regular AuthenticationSuccessEvent)
		verify(this.publisher, never()).publishEvent((ApplicationEvent) anyObject());
	}

	@Test
	public void testAuthenticationFailed() {
		this.listener.onApplicationEvent(new AuthenticationFailureExpiredEvent(
				new UsernamePasswordAuthenticationToken("user", "password"),
				new BadCredentialsException("Bad user")));
		verify(this.publisher).publishEvent((ApplicationEvent) anyObject());
	}

	@Test
	public void testAuthenticationSwitch() {
		this.listener.onApplicationEvent(new AuthenticationSwitchUserEvent(
				new UsernamePasswordAuthenticationToken("user", "password"),
				new User("user", "password",
						AuthorityUtils.commaSeparatedStringToAuthorityList("USER"))));
		verify(this.publisher).publishEvent((ApplicationEvent) anyObject());
	}

	@Test
	public void shouldPassDetailsToAuditEventOnAuthenticationFailureEvent()
		throws Exception {
		// given
		final Object details = new Object();
		final AuthenticationFailureExpiredEvent event =
			createAuthenticationFailureEvent(details);

		// when
		this.listener.onApplicationEvent(event);

		// then
		final ArgumentCaptor<AuditApplicationEvent> applicationEventArgumentCaptor =
			ArgumentCaptor.forClass(AuditApplicationEvent.class);
		verify(this.publisher).publishEvent(applicationEventArgumentCaptor.capture());
		final Map<String, Object> eventData =
			applicationEventArgumentCaptor.getValue().getAuditEvent().getData();
		assertThat(eventData, hasEntry("details", details));
	}

	private AuthenticationFailureExpiredEvent createAuthenticationFailureEvent(
		final Object details) {
		final UsernamePasswordAuthenticationToken authentication =
			new UsernamePasswordAuthenticationToken("user", "password");
		authentication.setDetails(details);
		final BadCredentialsException exception = new BadCredentialsException("Bad user");
		return new AuthenticationFailureExpiredEvent(authentication, exception);
	}
}
