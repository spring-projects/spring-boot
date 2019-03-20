/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.jmx;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Base for adapters that convert {@link Endpoint} implementations to {@link JmxEndpoint}.
 *
 * @author Christian Dupuis
 * @author Andy Wilkinson
 * @author Vedran Pavic
 * @author Phillip Webb
 * @see JmxEndpoint
 * @see DataEndpointMBean
 */
public abstract class EndpointMBean implements JmxEndpoint {

	private final DataConverter dataConverter;

	private final Endpoint<?> endpoint;

	/**
	 * Create a new {@link EndpointMBean} instance.
	 * @param beanName the bean name
	 * @param endpoint the endpoint to wrap
	 * @param objectMapper the {@link ObjectMapper} used to convert the payload
	 */
	public EndpointMBean(String beanName, Endpoint<?> endpoint,
			ObjectMapper objectMapper) {
		this.dataConverter = new DataConverter(objectMapper);
		Assert.notNull(beanName, "BeanName must not be null");
		Assert.notNull(endpoint, "Endpoint must not be null");
		this.endpoint = endpoint;
	}

	@ManagedAttribute(description = "Returns the class of the underlying endpoint")
	public String getEndpointClass() {
		return ClassUtils.getQualifiedName(getEndpointType());
	}

	@Override
	public boolean isEnabled() {
		return this.endpoint.isEnabled();
	}

	@ManagedAttribute(description = "Indicates whether the underlying endpoint exposes sensitive information")
	public boolean isSensitive() {
		return this.endpoint.isSensitive();
	}

	@Override
	public String getIdentity() {
		return ObjectUtils.getIdentityHexString(getEndpoint());
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Class<? extends Endpoint> getEndpointType() {
		return getEndpoint().getClass();
	}

	public Endpoint<?> getEndpoint() {
		return this.endpoint;
	}

	/**
	 * Convert the given data into JSON.
	 * @param data the source data
	 * @return the JSON representation
	 */
	protected Object convert(Object data) {
		return this.dataConverter.convert(data);
	}

}
