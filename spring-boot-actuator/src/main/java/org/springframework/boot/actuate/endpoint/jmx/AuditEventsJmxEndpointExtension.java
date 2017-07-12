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

package org.springframework.boot.actuate.endpoint.jmx;

import java.util.Date;
import java.util.List;

import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.endpoint.AuditEventsEndpoint;
import org.springframework.boot.endpoint.ReadOperation;
import org.springframework.boot.endpoint.jmx.JmxEndpointExtension;

/**
 * JMX-specific extension of the {@link AuditEventsEndpoint}.
 *
 * @author Vedran Pavic
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@JmxEndpointExtension(endpoint = AuditEventsEndpoint.class)
public class AuditEventsJmxEndpointExtension {

	private final AuditEventsEndpoint delegate;

	public AuditEventsJmxEndpointExtension(AuditEventsEndpoint delegate) {
		this.delegate = delegate;
	}

	@ReadOperation
	public List<AuditEvent> eventsWithDateAfter(Date dateAfter) {
		return this.delegate.eventsWithPrincipalDateAfterAndType(null, dateAfter, null);
	}

	@ReadOperation
	public List<AuditEvent> eventsWithPrincipalAndDateAfter(String principal,
			Date dateAfter) {
		return this.delegate.eventsWithPrincipalDateAfterAndType(principal, dateAfter,
				null);
	}

}
