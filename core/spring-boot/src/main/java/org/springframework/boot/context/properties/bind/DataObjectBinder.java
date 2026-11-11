/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.bind;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.bind.Binder.Context;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;

/**
 * Internal strategy used by {@link Binder} to bind data objects. A data object is an
 * object composed itself of recursively bound properties.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @see JavaBeanBinder
 * @see ValueObjectBinder
 */
interface DataObjectBinder {

	/**
	 * Return a bound instance or {@code null} if the {@link DataObjectBinder} does not
	 * support the specified {@link Bindable}.
	 * @param <T> the source type
	 * @param name the name being bound
	 * @param target the bindable to bind
	 * @param context the bind context
	 * @param propertyBinder property binder
	 * @param fallbackToDefaultValue if an attempt should be made to return a new default
	 * value when no values are bound
	 * @return a bound instance or {@code null}
	 */
	<T> @Nullable T bind(ConfigurationPropertyName name, Bindable<T> target, Context context,
			DataObjectPropertyBinder propertyBinder, boolean fallbackToDefaultValue);

	/**
	 * Return a newly created instance or {@code null} if the {@link DataObjectBinder}
	 * does not support the specified {@link Bindable}.
	 * @param <T> the source type
	 * @param target the bindable to create
	 * @param context the bind context
	 * @return the created instance
	 */
	<T> @Nullable T create(Bindable<T> target, Context context);

	/**
	 * Callback that can be used to add additional suppressed exceptions when an instance
	 * cannot be created.
	 * @param <T> the source type
	 * @param target the bindable that was being created
	 * @param context the bind context
	 * @param exception the exception about to be thrown
	 */
	default <T> void onUnableToCreateInstance(Bindable<T> target, Context context, RuntimeException exception) {
	}

}
