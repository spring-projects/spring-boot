/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.audit.listener;

import java.util.Date;
import java.util.Map;

import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.util.Assert;

/**
 * Spring {@link ApplicationEvent} to encapsulate {@link AuditEvent}s.
 *
 * @author Dave Syer
 */
public class AuditApplicationEvent extends ApplicationEvent {

	private final AuditEvent auditEvent;

	/**
	 * Create a new {@link AuditApplicationEvent} that wraps a newly created
	 * {@link AuditEvent}.
	 * @param principal the principal
	 * @param type the event type
	 * @param data the event data
	 * @see AuditEvent#AuditEvent(String, String, Map)
	 */
	public AuditApplicationEvent(String principal, String type,
			Map<String, Object> data) {
		this(new AuditEvent(principal, type, data));
	}

	/**
	 * Create a new {@link AuditApplicationEvent} that wraps a newly created
	 * {@link AuditEvent}.
	 * @param principal the principal
	 * @param type the event type
	 * @param data the event data
	 * @see AuditEvent#AuditEvent(String, String, String...)
	 */
	public AuditApplicationEvent(String principal, String type, String... data) {
		this(new AuditEvent(principal, type, data));
	}

	/**
	 * Create a new {@link AuditApplicationEvent} that wraps a newly created
	 * {@link AuditEvent}.
	 * @param timestamp the time stamp
	 * @param principal the principal
	 * @param type the event type
	 * @param data the event data
	 * @see AuditEvent#AuditEvent(Date, String, String, Map)
	 */
	public AuditApplicationEvent(Date timestamp, String principal, String type,
			Map<String, Object> data) {
		this(new AuditEvent(timestamp, principal, type, data));
	}

	/**
	 * Create a new {@link AuditApplicationEvent} that wraps the specified
	 * {@link AuditEvent}.
	 * @param auditEvent the source of this event
	 */
	public AuditApplicationEvent(AuditEvent auditEvent) {
		super(auditEvent);
		Assert.notNull(auditEvent, "AuditEvent must not be null");
		this.auditEvent = auditEvent;
	}

	/**
	 * Get the audit event.
	 * @return the audit event
	 */
	public AuditEvent getAuditEvent() {
		return this.auditEvent;
	}

}
