/*
 * Copyright 2012-present the original author or authors.
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
