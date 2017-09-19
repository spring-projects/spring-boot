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

package org.springframework.boot.actuate.autoconfigure.endpoint.web;

import java.util.List;

/**
 * Interface that provides path information for web mapped endpoints.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public interface EndpointPathProvider {

	/**
	 * Return all mapped endpoint paths.
	 * @return all paths
	 */
	List<String> getPaths();

	/**
	 * Return the path for the endpoint with the specified ID.
	 * @param id the endpoint ID
	 * @return the path of the endpoint or {@code null}
	 */
	String getPath(String id);

}
