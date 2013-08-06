/*
 * Copyright 2012-2013 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory {@link AuditEventRepository} implementation.
 * 
 * @author Dave Syer
 */
public class InMemoryAuditEventRepository implements AuditEventRepository {

	private int capacity = 100;

	private Map<String, List<AuditEvent>> events = new HashMap<String, List<AuditEvent>>();

	/**
	 * @param capacity the capacity to set
	 */
	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	@Override
	public List<AuditEvent> find(String principal, Date after) {
		synchronized (this.events) {
			return Collections.unmodifiableList(getEvents(principal));
		}
	}

	private List<AuditEvent> getEvents(String principal) {
		if (!this.events.containsKey(principal)) {
			this.events.put(principal, new ArrayList<AuditEvent>());
		}
		return this.events.get(principal);
	}

	@Override
	public void add(AuditEvent event) {
		synchronized (this.events) {
			List<AuditEvent> list = getEvents(event.getPrincipal());
			while (list.size() >= this.capacity) {
				list.remove(0);
			}
			list.add(event);
		}
	}

}
