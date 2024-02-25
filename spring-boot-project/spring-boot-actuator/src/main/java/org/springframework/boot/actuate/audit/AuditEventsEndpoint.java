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

package org.springframework.boot.actuate.audit;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link Endpoint @Endpoint} to expose audit events.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@Endpoint(id = "auditevents")
public class AuditEventsEndpoint {

	private final AuditEventRepository auditEventRepository;

	/**
	 * Constructs a new AuditEventsEndpoint with the specified AuditEventRepository.
	 * @param auditEventRepository the AuditEventRepository to be used by the endpoint
	 * (must not be null)
	 * @throws IllegalArgumentException if the auditEventRepository is null
	 */
	public AuditEventsEndpoint(AuditEventRepository auditEventRepository) {
		Assert.notNull(auditEventRepository, "AuditEventRepository must not be null");
		this.auditEventRepository = auditEventRepository;
	}

	/**
	 * Retrieves a list of audit events based on the specified parameters.
	 * @param principal The principal associated with the audit events (optional).
	 * @param after The date and time after which the audit events occurred (optional).
	 * @param type The type of audit events to retrieve (optional).
	 * @return An AuditEventsDescriptor object containing the retrieved audit events.
	 */
	@ReadOperation
	public AuditEventsDescriptor events(@Nullable String principal, @Nullable OffsetDateTime after,
			@Nullable String type) {
		List<AuditEvent> events = this.auditEventRepository.find(principal, getInstant(after), type);
		return new AuditEventsDescriptor(events);
	}

	/**
	 * Converts the given OffsetDateTime object to an Instant object.
	 * @param offsetDateTime the OffsetDateTime object to be converted
	 * @return the Instant object representing the same point on the time-line as the
	 * given OffsetDateTime, or null if the given OffsetDateTime is null
	 */
	private Instant getInstant(OffsetDateTime offsetDateTime) {
		return (offsetDateTime != null) ? offsetDateTime.toInstant() : null;
	}

	/**
	 * Description of an application's {@link AuditEvent audit events}.
	 */
	public static final class AuditEventsDescriptor implements OperationResponseBody {

		private final List<AuditEvent> events;

		/**
		 * Constructs a new AuditEventsDescriptor with the specified list of audit events.
		 * @param events the list of audit events
		 */
		private AuditEventsDescriptor(List<AuditEvent> events) {
			this.events = events;
		}

		/**
		 * Returns the list of AuditEvent objects.
		 * @return the list of AuditEvent objects
		 */
		public List<AuditEvent> getEvents() {
			return this.events;
		}

	}

}
