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

package org.springframework.boot.actuate.env;

import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension;
import org.springframework.boot.actuate.env.EnvironmentEndpoint.EnvironmentEntryDescriptor;

/**
 * {@link EndpointWebExtension} for the {@link EnvironmentEndpoint}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@EndpointWebExtension(endpoint = EnvironmentEndpoint.class)
public class EnvironmentEndpointWebExtension {

	private final EnvironmentEndpoint delegate;

	public EnvironmentEndpointWebExtension(EnvironmentEndpoint delegate) {
		this.delegate = delegate;
	}

	@ReadOperation
	public WebEndpointResponse<EnvironmentEntryDescriptor> environmentEntry(@Selector String toMatch) {
		EnvironmentEntryDescriptor descriptor = this.delegate.environmentEntry(toMatch);
		return new WebEndpointResponse<>(descriptor, getStatus(descriptor));
	}

	private int getStatus(EnvironmentEntryDescriptor descriptor) {
		if (descriptor.getProperty() == null) {
			return WebEndpointResponse.STATUS_NOT_FOUND;
		}
		return WebEndpointResponse.STATUS_OK;
	}

}
