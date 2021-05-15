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

import java.util.Map;

import org.springframework.boot.actuate.endpoint.http.ApiVersion;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
import org.springframework.util.Assert;

/**
 * The context for the {@link OperationInvoker invocation of an operation}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.0.0
 */
public class InvocationContext {

	private final SecurityContext securityContext;

	private final Map<String, Object> arguments;

	private final ApiVersion apiVersion;

	/**
	 * Creates a new context for an operation being invoked by the given
	 * {@code securityContext} with the given available {@code arguments}.
	 * @param securityContext the current security context. Never {@code null}
	 * @param arguments the arguments available to the operation. Never {@code null}
	 */
	public InvocationContext(SecurityContext securityContext, Map<String, Object> arguments) {
		this(null, securityContext, arguments);
	}

	/**
	 * Creates a new context for an operation being invoked by the given
	 * {@code securityContext} with the given available {@code arguments}.
	 * @param apiVersion the API version or {@code null} to use the latest
	 * @param securityContext the current security context. Never {@code null}
	 * @param arguments the arguments available to the operation. Never {@code null}
	 * @since 2.2.0
	 */
	public InvocationContext(ApiVersion apiVersion, SecurityContext securityContext, Map<String, Object> arguments) {
		Assert.notNull(securityContext, "SecurityContext must not be null");
		Assert.notNull(arguments, "Arguments must not be null");
		this.apiVersion = (apiVersion != null) ? apiVersion : ApiVersion.LATEST;
		this.securityContext = securityContext;
		this.arguments = arguments;
	}

	/**
	 * Return the API version in use.
	 * @return the apiVersion the API version
	 * @since 2.2.0
	 */
	public ApiVersion getApiVersion() {
		return this.apiVersion;
	}

	/**
	 * Return the security context to use for the invocation.
	 * @return the security context
	 */
	public SecurityContext getSecurityContext() {
		return this.securityContext;
	}

	/**
	 * Return the invocation arguments.
	 * @return the arguments
	 */
	public Map<String, Object> getArguments() {
		return this.arguments;
	}

}
