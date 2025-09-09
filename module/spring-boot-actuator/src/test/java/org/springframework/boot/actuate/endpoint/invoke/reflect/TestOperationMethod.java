/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.actuate.endpoint.invoke.reflect;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.function.Predicate;

import org.springframework.boot.actuate.endpoint.OperationType;

/**
 * Test {@link OperationMethod}.
 *
 * @author Phillip Webb
 */
public class TestOperationMethod extends OperationMethod {

	public static final Predicate<Parameter> NON_OPTIONAL = (parameter) -> false;

	public TestOperationMethod(Method method, OperationType operationType) {
		this(method, operationType, NON_OPTIONAL);
	}

	public TestOperationMethod(Method method, OperationType operationType, Predicate<Parameter> optionalParameters) {
		super(method, operationType, optionalParameters);
	}

}
