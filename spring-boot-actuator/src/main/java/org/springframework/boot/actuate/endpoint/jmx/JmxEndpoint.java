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

import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * A strategy for the JMX layer on top of an {@link Endpoint}. Implementations are allowed
 * to use {@code @ManagedAttribute} and the full Spring JMX machinery but should not use
 * the {@link ManagedResource @ManagedResource} annotation. Implementations may be backed
 * by an actual {@link Endpoint} or may be specifically designed for JMX only.
 *
 * @author Phillip Webb
 * @since 1.5.0
 * @see EndpointMBean
 * @see AbstractJmxEndpoint
 */
public interface JmxEndpoint {

	/**
	 * Return if the JMX endpoint is enabled.
	 * @return if the endpoint is enabled
	 */
	boolean isEnabled();

	/**
	 * Return the MBean identity for this endpoint.
	 * @return the MBean identity.
	 */
	String getIdentity();

	/**
	 * Return the type of {@link Endpoint} exposed, or {@code null} if this
	 * {@link JmxEndpoint} exposes information that cannot be represented as a traditional
	 * {@link Endpoint}.
	 * @return the endpoint type
	 */
	@SuppressWarnings("rawtypes")
	Class<? extends Endpoint> getEndpointType();

}
