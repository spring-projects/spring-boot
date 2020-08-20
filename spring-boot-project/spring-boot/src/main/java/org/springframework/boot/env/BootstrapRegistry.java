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

package org.springframework.boot.env;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

/**
 * A simple object registry that is available during {@link Environment} post-processing
 * up to the point that the {@link ApplicationContext} is prepared. The registry can be
 * used to store objects that may be expensive to create, or need to be shared by
 * different {@link EnvironmentPostProcessor EnvironmentPostProcessors}.
 * <p>
 * The registry uses the object type as a key, meaning that only a single instance of a
 * given class can be stored.
 * <p>
 * Registered instances may optionally use
 * {@link Registration#onApplicationContextPrepared(BiConsumer)
 * onApplicationContextPrepared(...)} to perform an action when the
 * {@link ApplicationContext} is {@link ApplicationPreparedEvent prepared}. For example,
 * an instance may choose to register itself as a regular Spring bean so that it is
 * available for the application to use.
 *
 * @author Phillip Webb
 * @since 2.4.0
 * @see EnvironmentPostProcessor
 */
public interface BootstrapRegistry {

	/**
	 * Get an instance from the registry, creating one if it does not already exist.
	 * @param <T> the instance type
	 * @param type the instance type
	 * @param instanceSupplier a supplier used to create the instance if it doesn't
	 * already exist
	 * @return the registered instance
	 */
	<T> T get(Class<T> type, Supplier<T> instanceSupplier);

	/**
	 * Get an instance from the registry, creating one if it does not already exist.
	 * @param <T> the instance type
	 * @param type the instance type
	 * @param instanceSupplier a supplier used to create the instance if it doesn't
	 * already exist
	 * @param onApplicationContextPreparedAction the action that should be called when the
	 * application context is prepared. This action is ignored if the registration already
	 * exists.
	 * @return the registered instance
	 */
	<T> T get(Class<T> type, Supplier<T> instanceSupplier,
			BiConsumer<ConfigurableApplicationContext, T> onApplicationContextPreparedAction);

	/**
	 * Register an instance with the registry and return a {@link Registration} that can
	 * be used to provide further configuration. This method will replace any existing
	 * registration.
	 * @param <T> the instance type
	 * @param type the instance type
	 * @param instanceSupplier a supplier used to create the instance if it doesn't
	 * already exist
	 * @return an instance registration
	 */
	<T> Registration<T> register(Class<T> type, Supplier<T> instanceSupplier);

	/**
	 * Return if a registration exists for the given type.
	 * @param <T> the instance type
	 * @param type the instance type
	 * @return {@code true} if the type has already been registered
	 */
	<T> boolean isRegistered(Class<T> type);

	/**
	 * Return any existing {@link Registration} for the given type.
	 * @param <T> the instance type
	 * @param type the instance type
	 * @return the existing registration or {@code null}
	 */
	<T> Registration<T> getRegistration(Class<T> type);

	/**
	 * A single registration contained in the registry.
	 *
	 * @param <T> the instance type
	 */
	interface Registration<T> {

		/**
		 * Get or crearte the registered object instance.
		 * @return the object instance
		 */
		T get();

		/**
		 * Add an action that should run when the {@link ApplicationContext} has been
		 * prepared.
		 * @param action the action to run
		 */
		void onApplicationContextPrepared(BiConsumer<ConfigurableApplicationContext, T> action);

	}

}
