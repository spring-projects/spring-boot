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
import org.springframework.boot.actuate.endpoint.SessionEndpoint;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * Special endpoint wrapper for {@link SessionEndpoint}.
 *
 * @author Eddú Meléndez
 * @since 1.4.0
 */
@ManagedResource
public class SessionEndpointMBean extends EndpointMBean {

	private SessionEndpoint sessionEndpoint;

	/**
	 * Create a new {@link SessionEndpointMBean} instance.
	 *
	 * @param beanName     the bean name
	 * @param endpoint     the endpoint to wrap
	 * @param objectMapper the {@link ObjectMapper} used to convert the payload
	 */
	public SessionEndpointMBean(String beanName, Endpoint<?> endpoint, ObjectMapper objectMapper) {
		super(beanName, endpoint, objectMapper);
		this.sessionEndpoint = (SessionEndpoint) getEndpoint();
	}

	@ManagedOperation(description = "Find current user's sessions")
	@ManagedOperationParameter(name = "username", description = "Application's username")
	public Object findSessionsByUsername(String username) {
		return convert(this.sessionEndpoint.result(username));
	}

	@ManagedOperation(description = "Delete session by id")
	@ManagedOperationParameter(name = "sessionId", description = "Web session id")
	public boolean deleteSessionBySessionId(String sessionId) {
		return this.sessionEndpoint.delete(sessionId);
	}

}
