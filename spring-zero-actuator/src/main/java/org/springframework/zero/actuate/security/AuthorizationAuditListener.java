/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.zero.actuate.security;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.security.access.event.AbstractAuthorizationEvent;
import org.springframework.security.access.event.AuthenticationCredentialsNotFoundEvent;
import org.springframework.security.access.event.AuthorizationFailureEvent;
import org.springframework.zero.actuate.audit.AuditEvent;
import org.springframework.zero.actuate.audit.listener.AuditApplicationEvent;

/**
 * {@link ApplicationListener} expose Spring Security {@link AbstractAuthorizationEvent
 * authorization events} as {@link AuditEvent}s.
 * 
 * @author Dave Syer
 */
public class AuthorizationAuditListener implements
		ApplicationListener<AbstractAuthorizationEvent>, ApplicationEventPublisherAware {

	private ApplicationEventPublisher publisher;

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@Override
	public void onApplicationEvent(AbstractAuthorizationEvent event) {
		if (event instanceof AuthenticationCredentialsNotFoundEvent) {
			onAuthenticationCredentialsNotFoundEvent((AuthenticationCredentialsNotFoundEvent) event);
		} else if (event instanceof AuthorizationFailureEvent) {
			onAuthorizationFailureEvent((AuthorizationFailureEvent) event);
		}
	}

	private void onAuthenticationCredentialsNotFoundEvent(
			AuthenticationCredentialsNotFoundEvent event) {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("type", event.getCredentialsNotFoundException().getClass().getName());
		data.put("message", event.getCredentialsNotFoundException().getMessage());
		publish(new AuditEvent("<unknown>", "AUTHENTICATION_FAILURE", data));
	}

	private void onAuthorizationFailureEvent(AuthorizationFailureEvent event) {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("type", event.getAccessDeniedException().getClass().getName());
		data.put("message", event.getAccessDeniedException().getMessage());
		publish(new AuditEvent(event.getAuthentication().getName(),
				"AUTHORIZATION_FAILURE", data));
	}

	private void publish(AuditEvent event) {
		if (this.publisher != null) {
			this.publisher.publishEvent(new AuditApplicationEvent(event));
		}
	}

}
