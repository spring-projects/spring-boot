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

package org.springframework.boot.actuate.endpoint.annotation;

import java.util.Collection;

import org.springframework.boot.actuate.endpoint.AbstractExposableEndpoint;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;

/**
 * Abstract base class for {@link ExposableEndpoint endpoints} discovered by a
 * {@link EndpointDiscoverer}.
 *
 * @param <O> the operation type
 * @author Phillip Webb
 * @since 2.0.0
 */
public abstract class AbstractDiscoveredEndpoint<O extends Operation> extends AbstractExposableEndpoint<O>
		implements DiscoveredEndpoint<O> {

	private final EndpointDiscoverer<?, ?> discoverer;

	private final Object endpointBean;

	/**
	 * Create a new {@link AbstractDiscoveredEndpoint} instance.
	 * @param discoverer the discoverer that discovered the endpoint
	 * @param endpointBean the primary source bean
	 * @param id the ID of the endpoint
	 * @param enabledByDefault if the endpoint is enabled by default
	 * @param operations the endpoint operations
	 */
	public AbstractDiscoveredEndpoint(EndpointDiscoverer<?, ?> discoverer, Object endpointBean, EndpointId id,
			boolean enabledByDefault, Collection<? extends O> operations) {
		super(id, enabledByDefault, operations);
		Assert.notNull(discoverer, "Discoverer must not be null");
		Assert.notNull(endpointBean, "EndpointBean must not be null");
		this.discoverer = discoverer;
		this.endpointBean = endpointBean;
	}

	@Override
	public Object getEndpointBean() {
		return this.endpointBean;
	}

	@Override
	public boolean wasDiscoveredBy(Class<? extends EndpointDiscoverer<?, ?>> discoverer) {
		return discoverer.isInstance(this.discoverer);
	}

	@Override
	public String toString() {
		ToStringCreator creator = new ToStringCreator(this).append("discoverer", this.discoverer.getClass().getName())
				.append("endpointBean", this.endpointBean.getClass().getName());
		appendFields(creator);
		return creator.toString();
	}

	protected void appendFields(ToStringCreator creator) {
	}

}
