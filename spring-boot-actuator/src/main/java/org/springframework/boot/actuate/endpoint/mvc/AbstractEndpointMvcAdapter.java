/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.mvc;

import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;

/**
 * Abstract base class for {@link MvcEndpoint} implementations.
 *
 * @param <E> the delegate endpoint
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 1.3.0
 */
public abstract class AbstractEndpointMvcAdapter<E extends Endpoint<?>>
		implements NamedMvcEndpoint {

	private final E delegate;

	/**
	 * Endpoint URL path.
	 */
	private String path;

	/**
	 * Create a new {@link EndpointMvcAdapter}.
	 * @param delegate the underlying {@link Endpoint} to adapt.
	 */
	public AbstractEndpointMvcAdapter(E delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
	}

	protected Object invoke() {
		if (!this.delegate.isEnabled()) {
			// Shouldn't happen - shouldn't be registered when delegate's disabled
			return getDisabledResponse();
		}
		return this.delegate.invoke();
	}

	public E getDelegate() {
		return this.delegate;
	}

	@Override
	public String getName() {
		return this.delegate.getId();
	}

	@Override
	public String getPath() {
		return (this.path != null) ? this.path : "/" + this.delegate.getId();
	}

	public void setPath(String path) {
		while (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		this.path = path;
	}

	@Override
	public boolean isSensitive() {
		return this.delegate.isSensitive();
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Class<? extends Endpoint> getEndpointType() {
		return this.delegate.getClass();
	}

	/**
	 * Returns the response that should be returned when the endpoint is disabled.
	 * @return the response to be returned when the endpoint is disabled
	 * @since 1.2.4
	 * @see Endpoint#isEnabled()
	 */
	protected ResponseEntity<?> getDisabledResponse() {
		return MvcEndpoint.DISABLED_RESPONSE;
	}

}
