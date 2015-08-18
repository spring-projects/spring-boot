/*
 * Copyright 2012-2015 the original author or authors.
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

/**
 * In-memory {@link AuditEventRepository} implementation.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
public class InMemoryAuditEventRepository implements AuditEventRepository {

	private static final int DEFAULT_CAPACITY = 4000;

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
	 * @param capacity the capacity to set
	 */
	public synchronized void setCapacity(int capacity) {
		this.events = new AuditEvent[capacity];
	}

	@Override
	public synchronized List<AuditEvent> find(String principal, Date after) {
		LinkedList<AuditEvent> events = new LinkedList<AuditEvent>();
		for (int i = 0; i < this.events.length; i++) {
			int index = ((this.tail + this.events.length - i) % this.events.length);
			AuditEvent event = this.events[index];
			if (event == null) {
				break;
			}
			if (isMatch(event, principal, after)) {
				events.addFirst(event);
			}
		}
		return events;
	}

	private boolean isMatch(AuditEvent auditEvent, String principal, Date after) {
		return (principal == null || auditEvent.getPrincipal().equals(principal))
				&& (after == null || auditEvent.getTimestamp().compareTo(after) >= 0);
	}

	@Override
	public synchronized void add(AuditEvent event) {
		this.tail = (this.tail + 1) % this.events.length;
		this.events[this.tail] = event;
	}

}
