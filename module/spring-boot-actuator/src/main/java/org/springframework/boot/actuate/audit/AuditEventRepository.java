/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.audit;

import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Repository for {@link AuditEvent}s.
 *
 * @author Dave Syer
 * @author Vedran Pavic
 * @since 1.0.0
 */
public interface AuditEventRepository {

	/**
	 * Log an event.
	 * @param event the audit event to log
	 */
	void add(AuditEvent event);

	/**
	 * Find audit events of specified type relating to the specified principal that
	 * occurred {@link Instant#isAfter(Instant) after} the time provided.
	 * @param principal the principal name to search for (or {@code null} if unrestricted)
	 * @param after time after which an event must have occurred (or {@code null} if
	 * unrestricted)
	 * @param type the event type to search for (or {@code null} if unrestricted)
	 * @return audit events of specified type relating to the principal
	 * @since 1.4.0
	 */
	List<AuditEvent> find(@Nullable String principal, @Nullable Instant after, @Nullable String type);

}
