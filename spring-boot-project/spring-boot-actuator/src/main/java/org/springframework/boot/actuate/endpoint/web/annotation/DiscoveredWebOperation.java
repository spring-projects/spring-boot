/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.web.annotation;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.annotation.AbstractDiscoveredOperation;
import org.springframework.boot.actuate.endpoint.annotation.DiscoveredOperationMethod;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.actuate.endpoint.web.WebOperationRequestPredicate;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.ClassUtils;

/**
 * A discovered {@link WebOperation web operation}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class DiscoveredWebOperation extends AbstractDiscoveredOperation implements WebOperation {

	private static final boolean REACTIVE_STREAMS_PRESENT = ClassUtils.isPresent(
			"org.reactivestreams.Publisher",
			DiscoveredWebOperation.class.getClassLoader());

	private final String id;

	private final boolean blocking;

	private final WebOperationRequestPredicate requestPredicate;

	DiscoveredWebOperation(EndpointId endpointId,
			DiscoveredOperationMethod operationMethod, OperationInvoker invoker,
			WebOperationRequestPredicate requestPredicate) {
		super(operationMethod, invoker);
		Method method = operationMethod.getMethod();
		this.id = getId(endpointId, method);
		this.blocking = getBlocking(method);
		this.requestPredicate = requestPredicate;
	}

	private String getId(EndpointId endpointId, Method method) {
		return endpointId + Stream.of(method.getParameters()).filter(this::hasSelector)
				.map(this::dashName).collect(Collectors.joining());
	}

	private boolean hasSelector(Parameter parameter) {
		return parameter.getAnnotation(Selector.class) != null;
	}

	private String dashName(Parameter parameter) {
		return "-" + parameter.getName();
	}

	private boolean getBlocking(Method method) {
		return !REACTIVE_STREAMS_PRESENT
				|| !Publisher.class.isAssignableFrom(method.getReturnType());
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public boolean isBlocking() {
		return this.blocking;
	}

	@Override
	public WebOperationRequestPredicate getRequestPredicate() {
		return this.requestPredicate;
	}

	@Override
	protected void appendFields(ToStringCreator creator) {
		creator.append("id", this.id).append("blocking", this.blocking)
				.append("requestPredicate", this.requestPredicate);
	}

}
