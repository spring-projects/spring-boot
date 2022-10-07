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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.authorization.event.AuthorizationEvent;
import org.springframework.security.core.Authentication;

/**
 * Default implementation of {@link AbstractAuthorizationAuditListener}.
 *
 * @author Dave Syer
 * @author Vedran Pavic
 * @since 1.0.0
 */
public class AuthorizationAuditListener extends AbstractAuthorizationAuditListener {

	/**
	 * Authorization failure event type.
	 */
	public static final String AUTHORIZATION_FAILURE = "AUTHORIZATION_FAILURE";

	@Override
	public void onApplicationEvent(AuthorizationEvent event) {
		if (event instanceof AuthorizationDeniedEvent<?> authorizationDeniedEvent) {
			onAuthorizationDeniedEvent(authorizationDeniedEvent);
		}
	}

	private void onAuthorizationDeniedEvent(AuthorizationDeniedEvent<?> event) {
		String name = getName(event.getAuthentication());
		Map<String, Object> data = new LinkedHashMap<>();
		Object details = getDetails(event.getAuthentication());
		if (details != null) {
			data.put("details", details);
		}
		publish(new AuditEvent(name, AUTHORIZATION_FAILURE, data));
	}

	private String getName(Supplier<Authentication> authentication) {
		try {
			return authentication.get().getName();
		}
		catch (Exception ex) {
			return "<unknown>";
		}
	}

	private Object getDetails(Supplier<Authentication> authentication) {
		try {
			return authentication.get().getDetails();
		}
		catch (Exception ex) {
			return null;
		}
	}

}
