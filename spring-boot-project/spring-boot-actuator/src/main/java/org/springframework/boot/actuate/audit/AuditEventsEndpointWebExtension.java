/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.audit;

import java.util.Date;

import org.springframework.boot.actuate.audit.AuditEventsEndpoint.AuditEventsDescriptor;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension;
import org.springframework.lang.Nullable;

/**
 * {@link EndpointWebExtension} for the {@link AuditEventsEndpoint}.
 *
 * @author Vedran Pavic
 * @since 2.0.0
 */
@EndpointWebExtension(endpoint = AuditEventsEndpoint.class)
public class AuditEventsEndpointWebExtension {

	private final AuditEventsEndpoint delegate;

	public AuditEventsEndpointWebExtension(AuditEventsEndpoint delegate) {
		this.delegate = delegate;
	}

	@ReadOperation
	public WebEndpointResponse<AuditEventsDescriptor> eventsWithPrincipalDateAfterAndType(
			@Nullable String principal, Date after, @Nullable String type) {
		AuditEventsDescriptor auditEvents = this.delegate
				.eventsWithPrincipalDateAfterAndType(principal, after, type);
		return new WebEndpointResponse<>(auditEvents);
	}

}
