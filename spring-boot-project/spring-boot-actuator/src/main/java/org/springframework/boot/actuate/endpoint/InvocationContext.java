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

package org.springframework.boot.actuate.endpoint;

import java.util.Map;

import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
import org.springframework.util.Assert;

/**
 * The context for the {@link OperationInvoker invocation of an operation}.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class InvocationContext {

	private final SecurityContext securityContext;

	private final Map<String, Object> arguments;

	/**
	 * Creates a new context for an operation being invoked by the given {@code principal}
	 * with the given available {@code arguments}.
	 * @param securityContext the current security context. Never {@code null}
	 * @param arguments the arguments available to the operation. Never {@code null}
	 */
	public InvocationContext(SecurityContext securityContext,
			Map<String, Object> arguments) {
		Assert.notNull(securityContext, "SecurityContext must not be null");
		Assert.notNull(arguments, "Arguments must not be null");
		this.securityContext = securityContext;
		this.arguments = arguments;
	}

	public SecurityContext getSecurityContext() {
		return this.securityContext;
	}

	public Map<String, Object> getArguments() {
		return this.arguments;
	}

}
