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
package org.springframework.bootstrap.service.security;

import java.util.HashMap;
import java.util.Map;

import org.springframework.bootstrap.service.audit.AuditEvent;
import org.springframework.bootstrap.service.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.security.access.event.AbstractAuthorizationEvent;
import org.springframework.security.access.event.AuthenticationCredentialsNotFoundEvent;
import org.springframework.security.access.event.AuthorizationFailureEvent;

/**
 * @author Dave Syer
 * 
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
		Map<String, Object> data = new HashMap<String, Object>();
		if (event instanceof AuthenticationCredentialsNotFoundEvent) {
			data.put("type", ((AuthenticationCredentialsNotFoundEvent) event)
					.getCredentialsNotFoundException().getClass().getName());
			data.put("message", ((AuthenticationCredentialsNotFoundEvent) event)
					.getCredentialsNotFoundException().getMessage());
			publish(new AuditEvent("<unknown>", "AUTHENTICATION_FAILURE", data));
		} else if (event instanceof AuthorizationFailureEvent) {
			data.put("type", ((AuthorizationFailureEvent) event)
					.getAccessDeniedException().getClass().getName());
			data.put("message", ((AuthorizationFailureEvent) event)
					.getAccessDeniedException().getMessage());
			publish(new AuditEvent(((AuthorizationFailureEvent) event)
					.getAuthentication().getName(), "AUTHORIZATION_FAILURE", data));
		}
	}

	private void publish(AuditEvent event) {
		if (this.publisher != null) {
			this.publisher.publishEvent(new AuditApplicationEvent(event));
		}
	}

}
