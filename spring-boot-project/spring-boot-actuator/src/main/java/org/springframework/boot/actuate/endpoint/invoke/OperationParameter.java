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

package org.springframework.boot.actuate.endpoint.invoke;

/**
 * A single operation parameter.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public interface OperationParameter {

	/**
	 * Returns the parameter name.
	 * @return the name
	 */
	String getName();

	/**
	 * Returns the parameter type.
	 * @return the type
	 */
	Class<?> getType();

	/**
	 * Return if the parameter is mandatory (does not accept null values).
	 * @return if the parameter is mandatory
	 */
	boolean isMandatory();

}
