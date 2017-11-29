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

/**
 * Strategy class that can be used to filter discovered endpoints.
 *
 * @author Phillip Webb
 * @param <T> the type of the endpoint's operations
 * @since 2.0.0
 */
@FunctionalInterface
public interface EndpointFilter<T extends Operation> {

	/**
	 * Return {@code true} if the filter matches.
	 * @param info the endpoint info
	 * @param discoverer the endpoint discoverer
	 * @return {@code true} if the filter matches
	 */
	boolean match(EndpointInfo<T> info, EndpointDiscoverer<T> discoverer);

}
