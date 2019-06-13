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

package org.springframework.boot.actuate.endpoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

	private boolean enabledByDefault;

	private List<O> operations;

	/**
	 * Create a new {@link AbstractExposableEndpoint} instance.
	 * @param id the endpoint id
	 * @param enabledByDefault if the endpoint is enabled by default
	 * @param operations the endpoint operations
	 */
	public AbstractExposableEndpoint(EndpointId id, boolean enabledByDefault, Collection<? extends O> operations) {
		Assert.notNull(id, "ID must not be null");
		Assert.notNull(operations, "Operations must not be null");
		this.id = id;
		this.enabledByDefault = enabledByDefault;
		this.operations = Collections.unmodifiableList(new ArrayList<>(operations));
	}

	@Override
	public EndpointId getEndpointId() {
		return this.id;
	}

	@Override
	public boolean isEnableByDefault() {
		return this.enabledByDefault;
	}

	@Override
	public Collection<O> getOperations() {
		return this.operations;
	}

}
