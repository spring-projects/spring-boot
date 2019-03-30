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

package org.springframework.boot.actuate.endpoint.jmx;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.boot.actuate.endpoint.InvocationContext;
import org.springframework.boot.actuate.endpoint.OperationType;

/**
 * Test {@link JmxOperation} implementation.
 *
 * @author Phillip Webb
 */
public class TestJmxOperation implements JmxOperation {

	private final OperationType operationType;

	private final Function<Map<String, Object>, Object> invoke;

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

	public TestJmxOperation(Function<Map<String, Object>, Object> invoke) {
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
	public Object invoke(InvocationContext context) {
		return (this.invoke != null) ? this.invoke.apply(context.getArguments())
				: "result";
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
