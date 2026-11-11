/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.jmx;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.actuate.endpoint.InvocationContext;
import org.springframework.boot.actuate.endpoint.OperationType;

/**
 * Test {@link JmxOperation} implementation.
 *
 * @author Phillip Webb
 */
public class TestJmxOperation implements JmxOperation {

	private final OperationType operationType;

	private final @Nullable Function<Map<String, Object>, @Nullable Object> invoke;

	private final List<JmxOperationParameter> parameters;

	public TestJmxOperation() {
		this.operationType = OperationType.READ;
		this.invoke = null;
		this.parameters = Collections.emptyList();
	}

	public TestJmxOperation(OperationType operationType) {
		this.operationType = operationType;
		this.invoke = null;
		this.parameters = Collections.emptyList();
	}

	public TestJmxOperation(Function<Map<String, Object>, @Nullable Object> invoke) {
		this.operationType = OperationType.READ;
		this.invoke = invoke;
		this.parameters = Collections.emptyList();
	}

	public TestJmxOperation(List<JmxOperationParameter> parameters) {
		this.operationType = OperationType.READ;
		this.invoke = null;
		this.parameters = parameters;
	}

	@Override
	public OperationType getType() {
		return this.operationType;
	}

	@Override
	public @Nullable Object invoke(InvocationContext context) {
		return (this.invoke != null) ? this.invoke.apply(context.getArguments()) : "result";
	}

	@Override
	public String getName() {
		return "testOperation";
	}

	@Override
	public Class<?> getOutputType() {
		return String.class;
	}

	@Override
	public String getDescription() {
		return "Test JMX operation";
	}

	@Override
	public List<JmxOperationParameter> getParameters() {
		return this.parameters;
	}

}
