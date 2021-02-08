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

package org.springframework.boot.actuate.endpoint.jmx;

/**
 * Maps an operation's response to a JMX-friendly form.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public interface JmxOperationResponseMapper {

	/**
	 * Map the response type to its JMX compliant counterpart.
	 * @param responseType the operation's response type
	 * @return the JMX compliant type
	 */
	Class<?> mapResponseType(Class<?> responseType);

	/**
	 * Map the operation's response so that it can be consumed by a JMX compliant client.
	 * @param response the operation's response
	 * @return the {@code response}, in a JMX compliant format
	 */
	Object mapResponse(Object response);

}
