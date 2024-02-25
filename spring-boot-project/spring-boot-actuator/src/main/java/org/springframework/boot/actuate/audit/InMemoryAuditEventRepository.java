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

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

import org.springframework.util.Assert;

/**
 * In-memory {@link AuditEventRepository} implementation.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Vedran Pavic
 * @since 1.0.0
 */
public class InMemoryAuditEventRepository implements AuditEventRepository {

	private static final int DEFAULT_CAPACITY = 1000;

	private final Object monitor = new Object();

	/**
	 * Circular buffer of the event with tail pointing to the last element.
	 */
	private AuditEvent[] events;

	private volatile int tail = -1;

	/**
     * Constructs a new InMemoryAuditEventRepository with the default capacity.
     */
    public InMemoryAuditEventRepository() {
		this(DEFAULT_CAPACITY);
	}

	/**
     * Constructs a new InMemoryAuditEventRepository with the specified capacity.
     * 
     * @param capacity the maximum number of AuditEvents that can be stored in the repository
     */
    public InMemoryAuditEventRepository(int capacity) {
		this.events = new AuditEvent[capacity];
	}

	/**
	 * Set the capacity of this event repository.
	 * @param capacity the capacity
	 */
	public void setCapacity(int capacity) {
		synchronized (this.monitor) {
			this.events = new AuditEvent[capacity];
		}
	}

	/**
     * Adds an AuditEvent to the repository.
     * 
     * @param event the AuditEvent to be added (must not be null)
     */
    @Override
	public void add(AuditEvent event) {
		Assert.notNull(event, "AuditEvent must not be null");
		synchronized (this.monitor) {
			this.tail = (this.tail + 1) % this.events.length;
			this.events[this.tail] = event;
		}
	}

	/**
     * Finds audit events based on the given criteria.
     * 
     * @param principal the principal associated with the audit events
     * @param after the minimum timestamp of the audit events
     * @param type the type of the audit events
     * @return a list of audit events that match the given criteria
     */
    @Override
	public List<AuditEvent> find(String principal, Instant after, String type) {
		LinkedList<AuditEvent> events = new LinkedList<>();
		synchronized (this.monitor) {
			for (int i = 0; i < this.events.length; i++) {
				AuditEvent event = resolveTailEvent(i);
				if (event != null && isMatch(principal, after, type, event)) {
					events.addFirst(event);
				}
			}
		}
		return events;
	}

	/**
     * Checks if the given audit event matches the specified criteria.
     * 
     * @param principal the principal to match (null to ignore)
     * @param after the timestamp to match (null to ignore)
     * @param type the type to match (null to ignore)
     * @param event the audit event to check
     * @return true if the audit event matches the criteria, false otherwise
     */
    private boolean isMatch(String principal, Instant after, String type, AuditEvent event) {
		boolean match = true;
		match = match && (principal == null || event.getPrincipal().equals(principal));
		match = match && (after == null || event.getTimestamp().isAfter(after));
		match = match && (type == null || event.getType().equals(type));
		return match;
	}

	/**
     * Resolves the tail event from the InMemoryAuditEventRepository based on the given offset.
     * 
     * @param offset the offset value to determine the position of the tail event
     * @return the tail event resolved from the repository
     */
    private AuditEvent resolveTailEvent(int offset) {
		int index = ((this.tail + this.events.length - offset) % this.events.length);
		return this.events[index];
	}

}
