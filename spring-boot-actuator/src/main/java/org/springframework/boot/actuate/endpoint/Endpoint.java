/*
 * Copyright 2012-2013 the original author or authors.
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

import org.springframework.http.MediaType;

/**
 * An endpoint that can be used to expose useful information to operations. Usually
 * exposed via Spring MVC but could also be exposed using some other technique.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
public interface Endpoint<T> {

	/**
	 * Returns the path of the endpoint. Must start with '/' and should not include
	 * wildcards.
	 */
	String getPath();

	/**
	 * Returns if the endpoint is sensitive, i.e. may return data that the average user
	 * should not see. Mappings can use this as a security hint.
	 */
	boolean isSensitive();

	/**
	 * Returns the {@link MediaType}s that this endpoint produces or {@code null}.
	 */
	MediaType[] getProduces();

	/**
	 * Called to invoke the endpoint.
	 * @return the results of the invocation
	 */
	T invoke();

}
