/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.web.annotation;

import java.util.stream.Collectors;

import org.reactivestreams.Publisher;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.annotation.AbstractDiscoveredOperation;
import org.springframework.boot.actuate.endpoint.annotation.DiscoveredOperationMethod;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
import org.springframework.boot.actuate.endpoint.invoke.OperationParameter;
import org.springframework.boot.actuate.endpoint.invoke.reflect.OperationMethod;
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
 * @author Moritz Halbritter
 */
class DiscoveredWebOperation extends AbstractDiscoveredOperation implements WebOperation {

	private static final boolean REACTIVE_STREAMS_PRESENT = ClassUtils.isPresent("org.reactivestreams.Publisher",
			DiscoveredWebOperation.class.getClassLoader());

	private final String id;

	private final boolean blocking;

	private final WebOperationRequestPredicate requestPredicate;

	DiscoveredWebOperation(EndpointId endpointId, DiscoveredOperationMethod operationMethod, OperationInvoker invoker,
			WebOperationRequestPredicate requestPredicate) {
		super(operationMethod, invoker);
		this.id = getId(endpointId, operationMethod);
		this.blocking = getBlocking(operationMethod);
		this.requestPredicate = requestPredicate;
	}

	private String getId(EndpointId endpointId, OperationMethod method) {
		return endpointId + method.getParameters()
			.stream()
			.filter(this::hasSelector)
			.map(this::dashName)
			.collect(Collectors.joining());
	}

	private boolean hasSelector(OperationParameter parameter) {
		return parameter.getAnnotation(Selector.class) != null;
	}

	private String dashName(OperationParameter parameter) {
		return "-" + parameter.getName();
	}

	private boolean getBlocking(OperationMethod method) {
		return !REACTIVE_STREAMS_PRESENT || !Publisher.class.isAssignableFrom(method.getMethod().getReturnType());
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
		creator.append("id", this.id)
			.append("blocking", this.blocking)
			.append("requestPredicate", this.requestPredicate);
	}

}
