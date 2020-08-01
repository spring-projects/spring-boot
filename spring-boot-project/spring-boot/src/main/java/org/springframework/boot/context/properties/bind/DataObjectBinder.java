/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.context.properties.bind;

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
	 * @param name the name being bound
	 * @param target the bindable to bind
	 * @param context the bind context
	 * @param propertyBinder property binder
	 * @param <T> the source type
	 * @return a bound instance or {@code null}
	 */
	<T> T bind(ConfigurationPropertyName name, Bindable<T> target, Context context,
			DataObjectPropertyBinder propertyBinder);

	/**
	 * Return a newly created instance or {@code null} if the {@link DataObjectBinder}
	 * does not support the specified {@link Bindable}.
	 * @param target the bindable to create
	 * @param context the bind context
	 * @param <T> the source type
	 * @return the created instance
	 */
	<T> T create(Bindable<T> target, Context context);

}
