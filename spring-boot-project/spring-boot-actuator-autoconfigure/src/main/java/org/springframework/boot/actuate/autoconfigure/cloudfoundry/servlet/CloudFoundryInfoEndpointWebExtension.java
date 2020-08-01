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
package org.springframework.boot.actuate.autoconfigure.cloudfoundry.servlet;

import java.util.Map;

import org.springframework.boot.actuate.autoconfigure.cloudfoundry.EndpointCloudFoundryExtension;
import org.springframework.boot.actuate.endpoint.annotation.EndpointExtension;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.info.InfoEndpoint;

/**
 * {@link EndpointExtension @EndpointExtension} for the {@link InfoEndpoint} that always
 * exposes full git details.
 *
 * @author Madhura Bhave
 * @since 2.2.0
 */
@EndpointCloudFoundryExtension(endpoint = InfoEndpoint.class)
public class CloudFoundryInfoEndpointWebExtension {

	private final InfoEndpoint delegate;

	public CloudFoundryInfoEndpointWebExtension(InfoEndpoint delegate) {
		this.delegate = delegate;
	}

	@ReadOperation
	public Map<String, Object> info() {
		return this.delegate.info();
	}

}
