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
import java.util.LinkedList;
import java.util.List;

import org.springframework.util.Assert;

/**
 * In-memory {@link AuditEventRepository} implementation.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Vedran Pavic
 */
public class InMemoryAuditEventRepository implements AuditEventRepository {

	private static final int DEFAULT_CAPACITY = 1000;

	private final Object monitor = new Object();

	/**
	 * Circular buffer of the event with tail pointing to the last element.
	 */
	private AuditEvent[] events;

	private volatile int tail = -1;

	public InMemoryAuditEventRepository() {
		this(DEFAULT_CAPACITY);
	}

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

	@Override
	public void add(AuditEvent event) {
		Assert.notNull(event, "AuditEvent must not be null");
		synchronized (this.monitor) {
			this.tail = (this.tail + 1) % this.events.length;
			this.events[this.tail] = event;
		}
	}

	@Override
	public List<AuditEvent> find(Date after) {
		return find(null, after, null);
	}

	@Override
	public List<AuditEvent> find(String principal, Date after) {
		return find(principal, after, null);
	}

	@Override
	public List<AuditEvent> find(String principal, Date after, String type) {
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

	private boolean isMatch(String principal, Date after, String type, AuditEvent event) {
		boolean match = true;
		match = match && (principal == null || event.getPrincipal().equals(principal));
		match = match && (after == null || event.getTimestamp().compareTo(after) >= 0);
		match = match && (type == null || event.getType().equals(type));
		return match;
	}

	private AuditEvent resolveTailEvent(int offset) {
		int index = ((this.tail + this.events.length - offset) % this.events.length);
		return this.events[index];
	}

}
