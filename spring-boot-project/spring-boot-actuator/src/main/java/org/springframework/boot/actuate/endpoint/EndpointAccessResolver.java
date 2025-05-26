/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

/**
 * Resolver for the permitted level of {@link Access access} to an endpoint.
 *
 * @author Andy Wilkinson
 * @since 3.4.0
 */
public interface EndpointAccessResolver {

	/**
	 * Resolves the permitted level of access for the endpoint with the given
	 * {@code endpointId} and {@code defaultAccess}.
	 * @param endpointId the ID of the endpoint
	 * @param defaultAccess the default access level of the endpoint
	 * @return the permitted level of access, never {@code null}
	 */
	Access accessFor(EndpointId endpointId, Access defaultAccess);

}
