/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.bind;

import org.jspecify.annotations.Nullable;

/**
 * Binder that can be used by {@link DataObjectBinder} implementations to bind the data
 * object properties.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
interface DataObjectPropertyBinder {

	/**
	 * Bind the given property.
	 * @param propertyName the property name (in lowercase dashed form, e.g.
	 * {@code first-name})
	 * @param target the target bindable
	 * @return the bound value or {@code null}
	 */
	@Nullable Object bindProperty(String propertyName, Bindable<?> target);

}
