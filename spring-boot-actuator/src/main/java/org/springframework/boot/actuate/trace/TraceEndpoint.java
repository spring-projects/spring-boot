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

package org.springframework.boot.actuate.trace;

import java.util.List;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.util.Assert;

/**
 * {@link Endpoint} to expose {@link Trace} information.
 *
 * @author Dave Syer
 * @since 2.0.0
 */
@Endpoint(id = "trace")
public class TraceEndpoint {

	private final TraceRepository repository;

	/**
	 * Create a new {@link TraceEndpoint} instance.
	 * @param repository the trace repository
	 */
	public TraceEndpoint(TraceRepository repository) {
		Assert.notNull(repository, "Repository must not be null");
		this.repository = repository;
	}

	@ReadOperation
	public TraceDescriptor traces() {
		return new TraceDescriptor(this.repository.findAll());
	}

	/**
	 * A description of an application's {@link Trace} entries. Primarily intended for
	 * serialization to JSON.
	 */
	public static final class TraceDescriptor {

		private final List<Trace> traces;

		private TraceDescriptor(List<Trace> traces) {
			this.traces = traces;
		}

		public List<Trace> getTraces() {
			return this.traces;
		}

	}

}
