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

import org.springframework.boot.loader.tools.Layer;

/**
 * A strategy used to match a resource to a layer.
 *
 * @author Madhura Bhave
 * @since 2.3.0
 */
public interface ResourceStrategy {

	/**
	 * Return a {@link Layer} for the given resource. If no matching layer is found,
	 * {@code null} is returned.
	 * @param resourceName the name of the resource
	 * @return the matching layer or {@code null}
	 */
	Layer getMatchingLayer(String resourceName);

}
