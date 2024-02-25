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

package org.springframework.boot;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.util.Assert;

/**
 * Default {@link ConfigurableBootstrapContext} implementation.
 *
 * @author Phillip Webb
 * @since 2.4.0
 */
public class DefaultBootstrapContext implements ConfigurableBootstrapContext {

	private final Map<Class<?>, InstanceSupplier<?>> instanceSuppliers = new HashMap<>();

	private final Map<Class<?>, Object> instances = new HashMap<>();

	private final ApplicationEventMulticaster events = new SimpleApplicationEventMulticaster();

	/**
	 * Registers a type with its corresponding instance supplier.
	 * @param <T> the type of the class being registered
	 * @param type the class being registered
	 * @param instanceSupplier the instance supplier for the class being registered
	 */
	@Override
	public <T> void register(Class<T> type, InstanceSupplier<T> instanceSupplier) {
		register(type, instanceSupplier, true);
	}

	/**
	 * Registers a new instance supplier for the specified type if it is not already
	 * registered.
	 * @param <T> the type of the instance to be registered
	 * @param type the class object representing the type of the instance
	 * @param instanceSupplier the supplier function that provides the instance
	 */
	@Override
	public <T> void registerIfAbsent(Class<T> type, InstanceSupplier<T> instanceSupplier) {
		register(type, instanceSupplier, false);
	}

	/**
	 * Registers a type with its corresponding instance supplier in the bootstrap context.
	 * @param <T> the type of the instance to be registered
	 * @param type the class object representing the type to be registered
	 * @param instanceSupplier the instance supplier that provides instances of the
	 * specified type
	 * @param replaceExisting a flag indicating whether to replace an existing
	 * registration for the same type
	 * @throws IllegalArgumentException if the type or instance supplier is null
	 * @throws IllegalStateException if an instance has already been created for the
	 * specified type and replaceExisting is false
	 */
	private <T> void register(Class<T> type, InstanceSupplier<T> instanceSupplier, boolean replaceExisting) {
		Assert.notNull(type, "Type must not be null");
		Assert.notNull(instanceSupplier, "InstanceSupplier must not be null");
		synchronized (this.instanceSuppliers) {
			boolean alreadyRegistered = this.instanceSuppliers.containsKey(type);
			if (replaceExisting || !alreadyRegistered) {
				Assert.state(!this.instances.containsKey(type), () -> type.getName() + " has already been created");
				this.instanceSuppliers.put(type, instanceSupplier);
			}
		}
	}

	/**
	 * Checks if a given type is registered in the DefaultBootstrapContext.
	 * @param type the type to check if registered
	 * @return true if the type is registered, false otherwise
	 */
	@Override
	public <T> boolean isRegistered(Class<T> type) {
		synchronized (this.instanceSuppliers) {
			return this.instanceSuppliers.containsKey(type);
		}
	}

	/**
	 * Retrieves the registered instance supplier for the specified type.
	 * @param type the class type of the instance
	 * @return the instance supplier for the specified type
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> InstanceSupplier<T> getRegisteredInstanceSupplier(Class<T> type) {
		synchronized (this.instanceSuppliers) {
			return (InstanceSupplier<T>) this.instanceSuppliers.get(type);
		}
	}

	/**
	 * Adds a close listener to the bootstrap context.
	 * @param listener the application listener to be added
	 */
	@Override
	public void addCloseListener(ApplicationListener<BootstrapContextClosedEvent> listener) {
		this.events.addApplicationListener(listener);
	}

	/**
	 * Retrieves an instance of the specified type from the bootstrap context.
	 * @param type the class object representing the type of the instance to retrieve
	 * @return an instance of the specified type
	 * @throws IllegalStateException if the specified type has not been registered
	 */
	@Override
	public <T> T get(Class<T> type) throws IllegalStateException {
		return getOrElseThrow(type, () -> new IllegalStateException(type.getName() + " has not been registered"));
	}

	/**
	 * Returns the value associated with the specified type if present in the bootstrap
	 * context, otherwise returns the specified default value.
	 * @param type the class object representing the type of the value to retrieve
	 * @param other the default value to return if the specified type is not present
	 * @param <T> the type of the value to retrieve
	 * @return the value associated with the specified type if present, otherwise the
	 * default value
	 */
	@Override
	public <T> T getOrElse(Class<T> type, T other) {
		return getOrElseSupply(type, () -> other);
	}

	/**
	 * Returns an instance of the specified type if available, otherwise returns the
	 * result of the supplied Supplier.
	 * @param type the class of the instance to retrieve
	 * @param other the Supplier to use if the instance is not available
	 * @return an instance of the specified type if available, otherwise the result of the
	 * supplied Supplier
	 */
	@Override
	public <T> T getOrElseSupply(Class<T> type, Supplier<T> other) {
		synchronized (this.instanceSuppliers) {
			InstanceSupplier<?> instanceSupplier = this.instanceSuppliers.get(type);
			return (instanceSupplier != null) ? getInstance(type, instanceSupplier) : other.get();
		}
	}

	/**
	 * Retrieves an instance of the specified type from the instance suppliers map, or
	 * throws an exception if the instance supplier is not found.
	 * @param <T> the type of the instance to retrieve
	 * @param <X> the type of the exception to throw
	 * @param type the class object representing the type of the instance to retrieve
	 * @param exceptionSupplier a supplier that provides an exception to throw if the
	 * instance supplier is not found
	 * @return an instance of the specified type
	 * @throws X if the instance supplier is not found
	 */
	@Override
	public <T, X extends Throwable> T getOrElseThrow(Class<T> type, Supplier<? extends X> exceptionSupplier) throws X {
		synchronized (this.instanceSuppliers) {
			InstanceSupplier<?> instanceSupplier = this.instanceSuppliers.get(type);
			if (instanceSupplier == null) {
				throw exceptionSupplier.get();
			}
			return getInstance(type, instanceSupplier);
		}
	}

	/**
	 * Retrieves an instance of the specified type from the bootstrap context.
	 * @param <T> the type of the instance to retrieve
	 * @param type the class object representing the type of the instance
	 * @param instanceSupplier the supplier used to create the instance if it doesn't
	 * exist
	 * @return the instance of the specified type
	 */
	@SuppressWarnings("unchecked")
	private <T> T getInstance(Class<T> type, InstanceSupplier<?> instanceSupplier) {
		T instance = (T) this.instances.get(type);
		if (instance == null) {
			instance = (T) instanceSupplier.get(this);
			if (instanceSupplier.getScope() == Scope.SINGLETON) {
				this.instances.put(type, instance);
			}
		}
		return instance;
	}

	/**
	 * Method to be called when {@link BootstrapContext} is closed and the
	 * {@link ApplicationContext} is prepared.
	 * @param applicationContext the prepared context
	 */
	public void close(ConfigurableApplicationContext applicationContext) {
		this.events.multicastEvent(new BootstrapContextClosedEvent(this, applicationContext));
	}

}
