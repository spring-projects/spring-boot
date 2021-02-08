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

package org.springframework.boot.actuate.trace.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * In-memory implementation of {@link HttpTraceRepository}.
 *
 * @author Dave Syer
 * @author Olivier Bourgain
 * @since 2.0.0
 */
public class InMemoryHttpTraceRepository implements HttpTraceRepository {

	private int capacity = 100;

	private boolean reverse = true;

	private final List<HttpTrace> traces = new LinkedList<>();

	/**
	 * Flag to say that the repository lists traces in reverse order.
	 * @param reverse flag value (default true)
	 */
	public void setReverse(boolean reverse) {
		synchronized (this.traces) {
			this.reverse = reverse;
		}
	}

	/**
	 * Set the capacity of the in-memory repository.
	 * @param capacity the capacity
	 */
	public void setCapacity(int capacity) {
		synchronized (this.traces) {
			this.capacity = capacity;
		}
	}

	@Override
	public List<HttpTrace> findAll() {
		synchronized (this.traces) {
			return Collections.unmodifiableList(new ArrayList<>(this.traces));
		}
	}

	@Override
	public void add(HttpTrace trace) {
		synchronized (this.traces) {
			while (this.traces.size() >= this.capacity) {
				this.traces.remove(this.reverse ? this.capacity - 1 : 0);
			}
			if (this.reverse) {
				this.traces.add(0, trace);
			}
			else {
				this.traces.add(trace);
			}
		}
	}

}
