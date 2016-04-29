/*
 * Copyright 2012-2016 the original author or authors.
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

	private static final int DEFAULT_CAPACITY = 4000;

	/**
	 * Circular buffer of the event with tail pointing to the last element.
	 */
	private final AuditEvent[] events;

	private volatile int tail = -1;

	public InMemoryAuditEventRepository() {
		this(DEFAULT_CAPACITY);
	}

	public InMemoryAuditEventRepository(int capacity) {
		this.events = new AuditEvent[capacity];
	}

	@Override
	public List<AuditEvent> find(Date after) {
		LinkedList<AuditEvent> events = new LinkedList<AuditEvent>();
		synchronized (this.events) {
			for (int i = 0; i < this.events.length; i++) {
				AuditEvent event = resolveTailEvent(i);
				if (event == null) {
					break;
				}
				if (isMatch(event, after)) {
					events.addFirst(event);
				}
			}
		}
		return events;
	}

	@Override
	public List<AuditEvent> find(String principal, Date after) {
		Assert.notNull(principal, "Principal must not be null");
		LinkedList<AuditEvent> events = new LinkedList<AuditEvent>();
		synchronized (this.events) {
			for (int i = 0; i < this.events.length; i++) {
				AuditEvent event = resolveTailEvent(i);
				if (event == null) {
					break;
				}
				if (isMatch(event, principal, after)) {
					events.addFirst(event);
				}
			}
		}
		return events;
	}

	@Override
	public List<AuditEvent> find(String principal, String type, Date after) {
		Assert.notNull(principal, "Principal must not be null");
		Assert.notNull(type, "Type must not be null");
		LinkedList<AuditEvent> events = new LinkedList<AuditEvent>();
		synchronized (this.events) {
			for (int i = 0; i < this.events.length; i++) {
				AuditEvent event = resolveTailEvent(i);
				if (event == null) {
					break;
				}
				if (isMatch(event, principal, type, after)) {
					events.addFirst(event);
				}
			}
		}
		return events;
	}

	@Override
	public void add(AuditEvent event) {
		Assert.notNull(event, "AuditEvent must not be null");
		synchronized (this.events) {
			this.tail = (this.tail + 1) % this.events.length;
			this.events[this.tail] = event;
		}
	}

	private AuditEvent resolveTailEvent(int offset) {
		int index = ((this.tail + this.events.length - offset) % this.events.length);
		return this.events[index];
	}

	private boolean isMatch(AuditEvent event, Date after) {
		return (after == null || event.getTimestamp().compareTo(after) >= 0);
	}

	private boolean isMatch(AuditEvent event, String principal, Date after) {
		return (event.getPrincipal().equals(principal) && isMatch(event, after));
	}

	private boolean isMatch(AuditEvent event, String principal, String type, Date after) {
		return (event.getType().equals(type) && isMatch(event, principal, after));
	}

}
