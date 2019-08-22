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

package org.springframework.boot.web.servlet;

/**
 * Enumeration of filter dispatcher types, identical to
 * {@link javax.servlet.DispatcherType} and used in configuration as the servlet API may
 * not be present.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public enum DispatcherType {

	/**
	 * Apply the filter on "RequestDispatcher.forward()" calls.
	 */
	FORWARD,

	/**
	 * Apply the filter on "RequestDispatcher.include()" calls.
	 */
	INCLUDE,

	/**
	 * Apply the filter on ordinary client calls.
	 */
	REQUEST,

	/**
	 * Apply the filter under calls dispatched from an AsyncContext.
	 */
	ASYNC,

	/**
	 * Apply the filter when an error is handled.
	 */
	ERROR

}
