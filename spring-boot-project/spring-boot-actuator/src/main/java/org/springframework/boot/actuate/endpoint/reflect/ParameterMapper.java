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

package org.springframework.boot.actuate.endpoint.reflect;

/**
 * Maps parameters to the required type when invoking an endpoint.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@FunctionalInterface
public interface ParameterMapper {

	/**
	 * Map the specified {@code input} parameter to the given {@code parameterType}.
	 * @param value a parameter value
	 * @param type the required type of the parameter
	 * @return a value suitable for that parameter
	 * @param <T> the actual type of the parameter
	 * @throws ParameterMappingException when a mapping failure occurs
	 */
	<T> T mapParameter(Object value, Class<T> type);

}
