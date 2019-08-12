/*
 * Copyright 2012-2019 the original author or authors.
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

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.util.Assert;

/**
 * A value object representing an audit event: at a particular time, a particular user or
 * agent carried out an action of a particular type. This object records the details of
 * such an event.
 * <p>
 * Users can inject a {@link AuditEventRepository} to publish their own events or
 * alternatively use Spring's {@link ApplicationEventPublisher} (usually obtained by
 * implementing {@link ApplicationEventPublisherAware}) to publish AuditApplicationEvents
 * (wrappers for AuditEvent).
 *
 * @author Dave Syer
 * @since 1.0.0
 * @see AuditEventRepository
 */
@JsonInclude(Include.NON_EMPTY)
public class AuditEvent implements Serializable {

	private final Instant timestamp;

	private final String principal;

	private final String type;

	private final Map<String, Object> data;

	/**
	 * Create a new audit event for the current time.
	 * @param principal the user principal responsible
	 * @param type the event type
	 * @param data the event data
	 */
	public AuditEvent(String principal, String type, Map<String, Object> data) {
		this(Instant.now(), principal, type, data);
	}

	/**
	 * Create a new audit event for the current time from data provided as name-value
	 * pairs.
	 * @param principal the user principal responsible
	 * @param type the event type
	 * @param data the event data in the form 'key=value' or simply 'key'
	 */
	public AuditEvent(String principal, String type, String... data) {
		this(Instant.now(), principal, type, convert(data));
	}

	/**
	 * Create a new audit event.
	 * @param timestamp the date/time of the event
	 * @param principal the user principal responsible
	 * @param type the event type
	 * @param data the event data
	 */
	public AuditEvent(Instant timestamp, String principal, String type, Map<String, Object> data) {
		Assert.notNull(timestamp, "Timestamp must not be null");
		Assert.notNull(type, "Type must not be null");
		this.timestamp = timestamp;
		this.principal = (principal != null) ? principal : "";
		this.type = type;
		this.data = Collections.unmodifiableMap(data);
	}

	private static Map<String, Object> convert(String[] data) {
		Map<String, Object> result = new HashMap<>();
		for (String entry : data) {
			int index = entry.indexOf('=');
			if (index != -1) {
				result.put(entry.substring(0, index), entry.substring(index + 1));
			}
			else {
				result.put(entry, null);
			}
		}
		return result;
	}

	/**
	 * Returns the date/time that the event was logged.
	 * @return the timestamp
	 */
	public Instant getTimestamp() {
		return this.timestamp;
	}

	/**
	 * Returns the user principal responsible for the event or an empty String if the
	 * principal is not available.
	 * @return the principal
	 */
	public String getPrincipal() {
		return this.principal;
	}

	/**
	 * Returns the type of event.
	 * @return the event type
	 */
	public String getType() {
		return this.type;
	}

	/**
	 * Returns the event data.
	 * @return the event data
	 */
	public Map<String, Object> getData() {
		return this.data;
	}

	@Override
	public String toString() {
		return "AuditEvent [timestamp=" + this.timestamp + ", principal=" + this.principal + ", type=" + this.type
				+ ", data=" + this.data + "]";
	}

}
