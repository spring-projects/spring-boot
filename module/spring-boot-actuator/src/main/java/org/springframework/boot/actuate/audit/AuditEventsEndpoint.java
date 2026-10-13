/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.audit;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
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

	public AuditEventsEndpoint(AuditEventRepository auditEventRepository) {
		Assert.notNull(auditEventRepository, "'auditEventRepository' must not be null");
		this.auditEventRepository = auditEventRepository;
	}

	@ReadOperation
	public AuditEventsDescriptor events(@Nullable String principal, @Nullable OffsetDateTime after,
			@Nullable String type) {
		List<AuditEvent> events = this.auditEventRepository.find(principal, getInstant(after), type);
		return new AuditEventsDescriptor(events);
	}

	private @Nullable Instant getInstant(@Nullable OffsetDateTime offsetDateTime) {
		return (offsetDateTime != null) ? offsetDateTime.toInstant() : null;
	}

	/**
	 * Description of an application's {@link AuditEvent audit events}.
	 */
	public static final class AuditEventsDescriptor implements OperationResponseBody {

		private final List<AuditEvent> events;

		private AuditEventsDescriptor(List<AuditEvent> events) {
			this.events = events;
		}

		public List<AuditEvent> getEvents() {
			return this.events;
		}

	}

}
