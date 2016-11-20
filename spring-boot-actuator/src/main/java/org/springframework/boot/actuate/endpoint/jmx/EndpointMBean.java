/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.jmx;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Simple wrapper around {@link Endpoint} implementations to enable JMX export.
 *
 * @author Christian Dupuis
 * @author Andy Wilkinson
 * @author Vedran Pavic
 */
@ManagedResource
public class EndpointMBean extends EndpointMBeanSupport {

	private final Endpoint<?> endpoint;

	/**
	 * Create a new {@link EndpointMBean} instance.
	 * @param beanName the bean name
	 * @param endpoint the endpoint to wrap
	 * @param objectMapper the {@link ObjectMapper} used to convert the payload
	 */
	public EndpointMBean(String beanName, Endpoint<?> endpoint,
			ObjectMapper objectMapper) {
		super(objectMapper);
		Assert.notNull(beanName, "BeanName must not be null");
		Assert.notNull(endpoint, "Endpoint must not be null");
		this.endpoint = endpoint;
	}

	@ManagedAttribute(description = "Returns the class of the underlying endpoint")
	public String getEndpointClass() {
		return ClassUtils.getQualifiedName(this.endpoint.getClass());
	}

	@ManagedAttribute(description = "Indicates whether the underlying endpoint exposes sensitive information")
	public boolean isSensitive() {
		return this.endpoint.isSensitive();
	}

	public Endpoint<?> getEndpoint() {
		return this.endpoint;
	}

}
