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

package org.springframework.boot.actuate.endpoint;

import java.util.Collection;
import java.util.Collections;

import org.springframework.util.Assert;

/**
 * Information describing an endpoint.
 *
 * @param <T> the type of the endpoint's operations
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class EndpointInfo<T extends Operation> {

	private final String id;

	private final boolean enableByDefault;

	private final Collection<T> operations;

	/**
	 * Creates a new {@code EndpointInfo} describing an endpoint with the given {@code id}
	 * and {@code operations}.
	 * @param id the id of the endpoint
	 * @param enableByDefault if the endpoint is enabled by default
	 * @param operations the operations of the endpoint
	 */
	public EndpointInfo(String id, boolean enableByDefault, Collection<T> operations) {
		Assert.hasText(id, "ID must not be empty");
		Assert.notNull(operations, "Operations must not be null");
		this.id = id;
		this.enableByDefault = enableByDefault;
		this.operations = Collections.unmodifiableCollection(operations);
	}

	/**
	 * Returns the id of the endpoint.
	 * @return the id
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * Returns if the endpoint is enabled by default.
	 * @return if the endpoint is enabled by default
	 */
	public boolean isEnableByDefault() {
		return this.enableByDefault;
	}

	/**
	 * Returns the operations of the endpoint.
	 * @return the operations
	 */
	public Collection<T> getOperations() {
		return this.operations;
	}

}
