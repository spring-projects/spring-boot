/*
 * Copyright 2012-2022 the original author or authors.
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

import java.util.Set;

import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.Show;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension;
import org.springframework.boot.actuate.env.EnvironmentEndpoint.EnvironmentDescriptor;
import org.springframework.boot.actuate.env.EnvironmentEndpoint.EnvironmentEntryDescriptor;
import org.springframework.lang.Nullable;

/**
 * {@link EndpointWebExtension @EndpointWebExtension} for the {@link EnvironmentEndpoint}.
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @since 2.0.0
 */
@EndpointWebExtension(endpoint = EnvironmentEndpoint.class)
public class EnvironmentEndpointWebExtension {

	private final EnvironmentEndpoint delegate;

	private final Show showValues;

	private final Set<String> roles;

	public EnvironmentEndpointWebExtension(EnvironmentEndpoint delegate, Show showValues, Set<String> roles) {
		this.delegate = delegate;
		this.showValues = showValues;
		this.roles = roles;
	}

	@ReadOperation
	public EnvironmentDescriptor environment(SecurityContext securityContext, @Nullable String pattern) {
		boolean showUnsanitized = this.showValues.isShown(securityContext, this.roles);
		return this.delegate.getEnvironmentDescriptor(pattern, showUnsanitized);
	}

	@ReadOperation
	public WebEndpointResponse<EnvironmentEntryDescriptor> environmentEntry(SecurityContext securityContext,
			@Selector String toMatch) {
		boolean showUnsanitized = this.showValues.isShown(securityContext, this.roles);
		EnvironmentEntryDescriptor descriptor = this.delegate.getEnvironmentEntryDescriptor(toMatch, showUnsanitized);
		return (descriptor.getProperty() != null) ? new WebEndpointResponse<>(descriptor, WebEndpointResponse.STATUS_OK)
				: new WebEndpointResponse<>(WebEndpointResponse.STATUS_NOT_FOUND);
	}

}
