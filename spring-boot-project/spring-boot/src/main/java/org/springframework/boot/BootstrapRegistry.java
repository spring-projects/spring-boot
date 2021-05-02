/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot;

import java.util.function.Supplier;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * A simple object registry that is available during startup and {@link Environment}
 * post-processing up to the point that the {@link ApplicationContext} is prepared.
 * <p>
 * Can be used to register instances that may be expensive to create, or need to be shared
 * before the {@link ApplicationContext} is available.
 * <p>
 * The registry uses {@link Class} as a key, meaning that only a single instance of a
 * given type can be stored.
 * <p>
 * The {@link #addCloseListener(ApplicationListener)} method can be used to add a listener
 * that can perform actions when {@link BootstrapContext} has been closed and the
 * {@link ApplicationContext} is fully prepared. For example, an instance may choose to
 * register itself as a regular Spring bean so that it is available for the application to
 * use.
 *
 * @author Phillip Webb
 * @since 2.4.0
 * @see BootstrapContext
 * @see ConfigurableBootstrapContext
 */
public interface BootstrapRegistry {

	/**
	 * Register a specific type with the registry. If the specified type has already been
	 * registered and has not been obtained as a {@link Scope#SINGLETON singleton}, it
	 * will be replaced.
	 * @param <T> the instance type
	 * @param type the instance type
	 * @param instanceSupplier the instance supplier
	 */
	<T> void register(Class<T> type, InstanceSupplier<T> instanceSupplier);

	/**
	 * Register a specific type with the registry if one is not already present.
	 * @param <T> the instance type
	 * @param type the instance type
	 * @param instanceSupplier the instance supplier
	 */
	<T> void registerIfAbsent(Class<T> type, InstanceSupplier<T> instanceSupplier);

	/**
	 * Return if a registration exists for the given type.
	 * @param <T> the instance type
	 * @param type the instance type
	 * @return {@code true} if the type has already been registered
	 */
	<T> boolean isRegistered(Class<T> type);

	/**
	 * Return any existing {@link InstanceSupplier} for the given type.
	 * @param <T> the instance type
	 * @param type the instance type
	 * @return the registered {@link InstanceSupplier} or {@code null}
	 */
	<T> InstanceSupplier<T> getRegisteredInstanceSupplier(Class<T> type);

	/**
	 * Add an {@link ApplicationListener} that will be called with a
	 * {@link BootstrapContextClosedEvent} when the {@link BootstrapContext} is closed and
	 * the {@link ApplicationContext} has been prepared.
	 * @param listener the listener to add
	 */
	void addCloseListener(ApplicationListener<BootstrapContextClosedEvent> listener);

	/**
	 * Supplier used to provide the actual instance when needed.
	 *
	 * @param <T> the instance type
	 * @see Scope
	 */
	@FunctionalInterface
	interface InstanceSupplier<T> {

		/**
		 * Factory method used to create the instance when needed.
		 * @param context the {@link BootstrapContext} which may be used to obtain other
		 * bootstrap instances.
		 * @return the instance
		 */
		T get(BootstrapContext context);

		/**
		 * Return the scope of the supplied instance.
		 * @return the scope
		 * @since 2.4.2
		 */
		default Scope getScope() {
			return Scope.SINGLETON;
		}

		/**
		 * Return a new {@link InstanceSupplier} with an updated {@link Scope}.
		 * @param scope the new scope
		 * @return a new {@link InstanceSupplier} instance with the new scope
		 * @since 2.4.2
		 */
		default InstanceSupplier<T> withScope(Scope scope) {
			Assert.notNull(scope, "Scope must not be null");
			InstanceSupplier<T> parent = this;
			return new InstanceSupplier<T>() {

				@Override
				public T get(BootstrapContext context) {
					return parent.get(context);
				}

				@Override
				public Scope getScope() {
					return scope;
				}

			};
		}

		/**
		 * Factory method that can be used to create an {@link InstanceSupplier} for a
		 * given instance.
		 * @param <T> the instance type
		 * @param instance the instance
		 * @return a new {@link InstanceSupplier}
		 */
		static <T> InstanceSupplier<T> of(T instance) {
			return (registry) -> instance;
		}

		/**
		 * Factory method that can be used to create an {@link InstanceSupplier} from a
		 * {@link Supplier}.
		 * @param <T> the instance type
		 * @param supplier the supplier that will provide the instance
		 * @return a new {@link InstanceSupplier}
		 */
		static <T> InstanceSupplier<T> from(Supplier<T> supplier) {
			return (registry) -> (supplier != null) ? supplier.get() : null;
		}

	}

	/**
	 * The scope of a instance.
	 * @since 2.4.2
	 */
	enum Scope {

		/**
		 * A singleton instance. The {@link InstanceSupplier} will be called only once and
		 * the same instance will be returned each time.
		 */
		SINGLETON,

		/**
		 * A prototype instance. The {@link InstanceSupplier} will be called whenver an
		 * instance is needed.
		 */
		PROTOTYPE

	}

}
