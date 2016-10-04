/*
 * Copyright 2012-2016 the original author or authors.
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

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.access.event.AuthenticationCredentialsNotFoundEvent;
import org.springframework.security.access.event.AuthorizationFailureEvent;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link AuthenticationAuditListener}.
 */
public class AuthorizationAuditListenerTests {

	private final AuthorizationAuditListener listener = new AuthorizationAuditListener();

	private final ApplicationEventPublisher publisher = mock(
			ApplicationEventPublisher.class);

	@Before
	public void init() {
		this.listener.setApplicationEventPublisher(this.publisher);
	}

	@Test
	public void testAuthenticationCredentialsNotFound() {
		this.listener.onApplicationEvent(new AuthenticationCredentialsNotFoundEvent(this,
				Arrays.<ConfigAttribute>asList(new SecurityConfig("USER")),
				new AuthenticationCredentialsNotFoundException("Bad user")));
		verify(this.publisher).publishEvent((ApplicationEvent) anyObject());
	}

	@Test
	public void testAuthorizationFailure() {
		this.listener.onApplicationEvent(new AuthorizationFailureEvent(this,
				Arrays.<ConfigAttribute>asList(new SecurityConfig("USER")),
				new UsernamePasswordAuthenticationToken("user", "password"),
				new AccessDeniedException("Bad user")));
		verify(this.publisher).publishEvent((ApplicationEvent) anyObject());
	}

	@Test
	public void testDetailsAreIncludedInAuditEvent() throws Exception {
		Object details = new Object();
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				"user", "password");
		authentication.setDetails(details);
		this.listener.onApplicationEvent(new AuthorizationFailureEvent(this,
				Arrays.<ConfigAttribute>asList(new SecurityConfig("USER")),
				authentication, new AccessDeniedException("Bad user")));
		ArgumentCaptor<AuditApplicationEvent> auditApplicationEvent = ArgumentCaptor
				.forClass(AuditApplicationEvent.class);
		verify(this.publisher).publishEvent(auditApplicationEvent.capture());
		assertThat(auditApplicationEvent.getValue().getAuditEvent().getData())
				.containsEntry("details", details);
	}

}
