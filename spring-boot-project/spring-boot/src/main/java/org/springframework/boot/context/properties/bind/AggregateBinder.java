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

import java.util.function.Supplier;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;

/**
 * Internal strategy used by {@link Binder} to bind aggregates (Maps, Lists, Arrays).
 *
 * @param <T> the type being bound
 * @author Phillip Webb
 * @author Madhura Bhave
 */
abstract class AggregateBinder<T> {

	private final BindContext context;

	AggregateBinder(BindContext context) {
		this.context = context;
	}

	/**
	 * Determine if recursive binding is supported.
	 * @param source the configuration property source or {@code null} for all sources.
	 * @return if recursive binding is supported
	 */
	protected abstract boolean isAllowRecursiveBinding(
			ConfigurationPropertySource source);

	/**
	 * Perform binding for the aggregate.
	 * @param name the configuration property name to bind
	 * @param target the target to bind
	 * @param elementBinder an element binder
	 * @return the bound aggregate or null
	 */
	@SuppressWarnings("unchecked")
	public final Object bind(ConfigurationPropertyName name, Bindable<?> target,
			AggregateElementBinder elementBinder) {
		Object result = bindAggregate(name, target, elementBinder);
		Supplier<?> value = target.getValue();
		if (result == null || value == null || value.get() == null) {
			return result;
		}
		return merge((T) value.get(), (T) result);
	}

	/**
	 * Perform the actual aggregate binding.
	 * @param name the configuration property name to bind
	 * @param target the target to bind
	 * @param elementBinder an element binder
	 * @return the bound result
	 */
	protected abstract Object bindAggregate(ConfigurationPropertyName name,
			Bindable<?> target, AggregateElementBinder elementBinder);

	/**
	 * Merge any additional elements into the existing aggregate.
	 * @param existing the existing value
	 * @param additional the additional elements to merge
	 * @return the merged result
	 */
	protected abstract T merge(T existing, T additional);

	/**
	 * Return the context being used by this binder.
	 * @return the context
	 */
	protected final BindContext getContext() {
		return this.context;
	}

	/**
	 * Internal class used to supply the aggregate and cache the value.
	 * @param <T> The aggregate type
	 */
	protected static class AggregateSupplier<T> {

		private final Supplier<T> supplier;

		private T supplied;

		public AggregateSupplier(Supplier<T> supplier) {
			this.supplier = supplier;
		}

		public T get() {
			if (this.supplied == null) {
				this.supplied = this.supplier.get();
			}
			return this.supplied;
		}

		public boolean wasSupplied() {
			return this.supplied != null;
		}

	}

}
