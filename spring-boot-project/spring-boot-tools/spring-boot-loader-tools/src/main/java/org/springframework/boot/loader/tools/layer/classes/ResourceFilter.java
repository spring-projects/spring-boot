/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.loader.tools.layer.classes;

/**
 * A filter that can tell if a resource has been included or excluded.
 *
 * @author Madhura Bhave
 * @since 2.3.0
 */
public interface ResourceFilter {

	/**
	 * Return true if the resource is included by the filter.
	 * @param resourceName the resource name
	 * @return true if the resource is included
	 */
	boolean isResourceIncluded(String resourceName);

	/**
	 * Return true if the resource is included by the filter.
	 * @param resourceName the resource name
	 * @return true if the resource is excluded
	 */
	boolean isResourceExcluded(String resourceName);

}
