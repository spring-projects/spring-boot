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

package org.springframework.bootstrap.actuate.trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * In-memory implementation of {@link TraceRepository}.
 * 
 * @author Dave Syer
 */
public class InMemoryTraceRepository implements TraceRepository {

	private int capacity = 100;

	private List<Trace> traces = new ArrayList<Trace>();

	/**
	 * @param capacity the capacity to set
	 */
	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	@Override
	public List<Trace> findAll() {
		synchronized (this.traces) {
			return Collections.unmodifiableList(this.traces);
		}
	}

	@Override
	public void add(Map<String, Object> map) {
		Trace trace = new Trace(new Date(), map);
		synchronized (this.traces) {
			while (this.traces.size() >= this.capacity) {
				this.traces.remove(0);
			}
			this.traces.add(trace);
		}
	}

}
