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

package org.springframework.boot.actuate.endpoint.invoke;

import java.util.stream.Stream;

/**
 * A collection of {@link OperationParameter operation parameters}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public interface OperationParameters extends Iterable<OperationParameter> {

	/**
	 * Return {@code true} if there is at least one parameter.
	 * @return if there are parameters
	 */
	default boolean hasParameters() {
		return getParameterCount() > 0;
	}

	/**
	 * Return the total number of parameters.
	 * @return the total number of parameters
	 */
	int getParameterCount();

	/**
	 * Return if any of the contained parameters are
	 * {@link OperationParameter#isMandatory() mandatory}.
	 * @return if any parameters are mandatory
	 */
	default boolean hasMandatoryParameter() {
		return stream().anyMatch(OperationParameter::isMandatory);
	}

	/**
	 * Return the parameter at the specified index.
	 * @param index the parameter index
	 * @return the parameter
	 */
	OperationParameter get(int index);

	/**
	 * Return a stream of the contained parameters.
	 * @return a stream of the parameters
	 */
	Stream<OperationParameter> stream();

}
