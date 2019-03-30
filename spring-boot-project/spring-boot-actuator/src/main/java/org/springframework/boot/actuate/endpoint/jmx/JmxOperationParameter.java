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

/**
 * Describes the parameters of an operation on a JMX endpoint.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 2.0.0
 */
public interface JmxOperationParameter {

	/**
	 * Return the name of the operation parameter.
	 * @return the name of the parameter
	 */
	String getName();

	/**
	 * Return the type of the operation parameter.
	 * @return the type
	 */
	Class<?> getType();

	/**
	 * Return the description of the parameter or {@code null} if none is available.
	 * @return the description or {@code null}
	 */
	String getDescription();

}
