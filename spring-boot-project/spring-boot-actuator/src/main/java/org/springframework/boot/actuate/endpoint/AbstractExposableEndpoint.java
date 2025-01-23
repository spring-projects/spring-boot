/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.util.Collection;
import java.util.List;

import org.springframework.util.Assert;

/**
 * Abstract base class for {@link ExposableEndpoint} implementations.
 *
 * @param <O> the operation type.
 * @author Phillip Webb
 * @since 2.0.0
 */
public abstract class AbstractExposableEndpoint<O extends Operation> implements ExposableEndpoint<O> {

	private final EndpointId id;

	private final Access defaultAccess;

	private final List<O> operations;

	/**
	 * Create a new {@link AbstractExposableEndpoint} instance.
	 * @param id the endpoint id
	 * @param enabledByDefault if the endpoint is enabled by default
	 * @param operations the endpoint operations
	 * @deprecated since 3.4.0 for removal in 3.6.0 in favor of
	 * {@link #AbstractExposableEndpoint(EndpointId, Access, Collection)}
	 */
	@Deprecated(since = "3.4.0", forRemoval = true)
	public AbstractExposableEndpoint(EndpointId id, boolean enabledByDefault, Collection<? extends O> operations) {
		this(id, (enabledByDefault) ? Access.UNRESTRICTED : Access.READ_ONLY, operations);
	}

	/**
	 * Create a new {@link AbstractExposableEndpoint} instance.
	 * @param id the endpoint id
	 * @param defaultAccess access to the endpoint that is permitted by default
	 * @param operations the endpoint operations
	 * @since 3.4.0
	 */
	public AbstractExposableEndpoint(EndpointId id, Access defaultAccess, Collection<? extends O> operations) {
		Assert.notNull(id, "'id' must not be null");
		Assert.notNull(operations, "'operations' must not be null");
		this.id = id;
		this.defaultAccess = defaultAccess;
		this.operations = List.copyOf(operations);
	}

	@Override
	public EndpointId getEndpointId() {
		return this.id;
	}

	@Override
	@SuppressWarnings("removal")
	@Deprecated(since = "3.4.0", forRemoval = true)
	public boolean isEnableByDefault() {
		return this.defaultAccess != Access.NONE;
	}

	@Override
	public Access getDefaultAccess() {
		return this.defaultAccess;
	}

	@Override
	public Collection<O> getOperations() {
		return this.operations;
	}

}
