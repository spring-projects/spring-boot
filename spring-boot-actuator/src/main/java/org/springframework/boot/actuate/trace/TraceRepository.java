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

package org.springframework.boot.actuate.trace;

import java.util.List;
import java.util.Map;

/**
 * A repository for {@link Trace}s.
 *
 * @author Dave Syer
 */
public interface TraceRepository {

	/**
	 * Find all {@link Trace} objects contained in the repository.
	 * @return the results
	 */
	List<Trace> findAll();

	/**
	 * Add a new {@link Trace} object at the current time.
	 * @param traceInfo trace information
	 * @throws IllegalStateException if trace associated with current thread
	 * was not finished
	 */
	void add(String id, Map<String, Object> traceInfo);

	/**
	 * Returns started {@link Trace} associated with current thread.
	 * @return trace
	 * @throws IllegalStateException if any trace was not associated with current thread
	 */
	Trace current();

	/**
	 * Marks a {@link Trace} with trace identifier as finished and removes it
	 * from the {@link #current()} holder.
	 * @param id trace identifier
	 * @throws IllegalStateException if any trace was not associated with current thread
	 * or its identifier not match to the given
	 */
	void finish(String id);
}
