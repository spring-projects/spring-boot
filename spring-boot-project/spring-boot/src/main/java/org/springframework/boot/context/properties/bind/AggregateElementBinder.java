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

package org.springframework.boot.context.properties.bind;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;

/**
 * Binder that can be used by {@link AggregateBinder} implementations to recursively bind
 * elements.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
@FunctionalInterface
interface AggregateElementBinder {

	/**
	 * Bind the given name to a target bindable.
	 * @param name the name to bind
	 * @param target the target bindable
	 * @return a bound object or {@code null}
	 */
	default Object bind(ConfigurationPropertyName name, Bindable<?> target) {
		return bind(name, target, null);
	}

	/**
	 * Bind the given name to a target bindable using optionally limited to a single
	 * source.
	 * @param name the name to bind
	 * @param target the target bindable
	 * @param source the source of the elements or {@code null} to use all sources
	 * @return a bound object or {@code null}
	 */
	Object bind(ConfigurationPropertyName name, Bindable<?> target,
			ConfigurationPropertySource source);

}
