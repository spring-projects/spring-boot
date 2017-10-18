/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;

/**
 * Map method's parameter by name.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 * @see ParameterNameDiscoverer
 */
public class ParameterNameMapper implements Function<Method, Map<String, Parameter>> {

	private final ParameterNameDiscoverer parameterNameDiscoverer;

	public ParameterNameMapper(ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}

	public ParameterNameMapper() {
		this(new DefaultParameterNameDiscoverer());
	}

	/**
	 * Map the {@link Parameter parameters} of the specified {@link Method} by parameter
	 * name.
	 * @param method the method to handle
	 * @return the parameters of the {@code method}, mapped by parameter name
	 */
	@Override
	public Map<String, Parameter> apply(Method method) {
		String[] parameterNames = this.parameterNameDiscoverer.getParameterNames(
				method);
		Map<String, Parameter> parameters = new LinkedHashMap<>();
		if (parameterNames != null) {
			for (int i = 0; i < parameterNames.length; i++) {
				parameters.put(parameterNames[i], method.getParameters()[i]);
			}
			return parameters;
		}
		else {
			throw new IllegalStateException("Failed to extract parameter names for "
					+ method);
		}

	}

}
