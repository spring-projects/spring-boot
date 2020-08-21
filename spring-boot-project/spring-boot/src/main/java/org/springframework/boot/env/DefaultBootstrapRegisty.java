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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Default implementation of {@link BootstrapRegistry}.
 *
 * @author Phillip Webb
 * @since 2.4.0
 */
public class DefaultBootstrapRegisty implements BootstrapRegistry {

	private final Map<Class<?>, DefaultRegistration<?>> registrations = new HashMap<>();

	@Override
	public <T> T get(Class<T> type, Supplier<T> instanceSupplier) {
		return get(type, instanceSupplier, null);
	}

	@Override
	public <T> T get(Class<T> type, Supplier<T> instanceSupplier,
			BiConsumer<ConfigurableApplicationContext, T> onApplicationContextPreparedAction) {
		Registration<T> registration = getRegistration(type);
		if (registration != null) {
			return registration.get();
		}
		registration = register(type, instanceSupplier);
		registration.onApplicationContextPrepared(onApplicationContextPreparedAction);
		return registration.get();
	}

	@Override
	public <T> Registration<T> register(Class<T> type, Supplier<T> instanceSupplier) {
		DefaultRegistration<T> registration = new DefaultRegistration<>(instanceSupplier);
		this.registrations.put(type, registration);
		return registration;
	}

	@Override
	public <T> boolean isRegistered(Class<T> type) {
		return getRegistration(type) != null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Registration<T> getRegistration(Class<T> type) {
		return (Registration<T>) this.registrations.get(type);
	}

	/**
	 * Method to be called when the {@link ApplicationContext} is prepared.
	 * @param applicationContext the prepared context
	 */
	public void applicationContextPrepared(ConfigurableApplicationContext applicationContext) {
		this.registrations.values()
				.forEach((registration) -> registration.applicationContextPrepared(applicationContext));
	}

	/**
	 * Clear the registry to reclaim memory.
	 */
	public void clear() {
		this.registrations.clear();
	}

	/**
	 * Default implementation of {@link Registration}.
	 */
	private static class DefaultRegistration<T> implements Registration<T> {

		private Supplier<T> instanceSupplier;

		private volatile T instance;

		private List<BiConsumer<ConfigurableApplicationContext, T>> applicationContextPreparedActions = new ArrayList<>();

		DefaultRegistration(Supplier<T> instanceSupplier) {
			this.instanceSupplier = instanceSupplier;
		}

		@Override
		public T get() {
			T instance = this.instance;
			if (instance == null) {
				synchronized (this.instanceSupplier) {
					instance = this.instanceSupplier.get();
					this.instance = instance;
				}
			}
			return instance;
		}

		@Override
		public void onApplicationContextPrepared(BiConsumer<ConfigurableApplicationContext, T> action) {
			if (action != null) {
				this.applicationContextPreparedActions.add(action);
			}
		}

		/**
		 * Method called when the {@link ApplicationContext} is prepared.
		 * @param applicationContext the prepared context
		 */
		void applicationContextPrepared(ConfigurableApplicationContext applicationContext) {
			this.applicationContextPreparedActions.forEach((consumer) -> consumer.accept(applicationContext, get()));
		}

	}

}
