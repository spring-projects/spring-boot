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

package org.springframework.boot.loader.tools.layer;

import org.springframework.boot.loader.tools.Layer;

/**
 * Strategy used by {@link CustomLayers} to select the layer of an item.
 *
 * @param <T> the content type
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.3.0
 * @see IncludeExcludeContentSelector
 */
public interface ContentSelector<T> {

	/**
	 * Return the {@link Layer} that the selector represents.
	 * @return the named layer
	 */
	Layer getLayer();

	/**
	 * Returns {@code true} if the specified item is contained in this selection.
	 * @param item the item to test
	 * @return if the item is contained
	 */
	boolean contains(T item);

}
