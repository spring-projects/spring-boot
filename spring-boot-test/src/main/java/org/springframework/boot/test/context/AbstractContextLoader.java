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

package org.springframework.boot.test.context;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigRegistry;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base implementation of {@link ContextLoader}.
 *
 * @param <T> the type of the context to be loaded
 * @param <L> the type of the loader
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
class AbstractContextLoader<T extends ConfigurableApplicationContext, L extends AbstractContextLoader<T, ?>>
		implements ContextLoader {

	private final Map<String, String> systemProperties = new LinkedHashMap<>();

	private final List<String> env = new ArrayList<>();

	private final Set<Class<?>> userConfigurations = new LinkedHashSet<>();

	private final LinkedList<Class<?>> autoConfigurations = new LinkedList<>();

	private final Supplier<T> contextSupplier;

	private ClassLoader classLoader;

	protected AbstractContextLoader(Supplier<T> contextSupplier) {
		this.contextSupplier = contextSupplier;
	}

	/**
	 * Set the specified system property prior to loading the context and restore its
	 * previous value once the consumer has been invoked and the context closed. If the
	 * {@code value} is {@code null} this removes any prior customization for that key.
	 * @param key the system property
	 * @param value the value (can be null to remove any existing customization)
	 * @return this instance
	 */
	@Override
	public L systemProperty(String key, String value) {
		Assert.notNull(key, "Key must not be null");
		if (value != null) {
			this.systemProperties.put(key, value);
		}
		else {
			this.systemProperties.remove(key);
		}
		return self();
	}

	/**
	 * Add the specified property pairs. Key-value pairs can be specified with colon (":")
	 * or equals ("=") separators. Override matching keys that might have been specified
	 * previously.
	 * @param pairs the key-value pairs for properties that need to be added to the
	 * environment
	 * @return this instance
	 */
	@Override
	public L env(String... pairs) {
		if (!ObjectUtils.isEmpty(pairs)) {
			this.env.addAll(Arrays.asList(pairs));
		}
		return self();
	}

	/**
	 * Add the specified user configuration classes.
	 * @param configs the user configuration classes to add
	 * @return this instance
	 */
	@Override
	public L config(Class<?>... configs) {
		if (!ObjectUtils.isEmpty(configs)) {
			this.userConfigurations.addAll(Arrays.asList(configs));
		}
		return self();
	}

	/**
	 * Add the specified auto-configuration classes.
	 * @param autoConfigurations the auto-configuration classes to add
	 * @return this instance
	 */
	@Override
	public L autoConfig(Class<?>... autoConfigurations) {
		if (!ObjectUtils.isEmpty(autoConfigurations)) {
			this.autoConfigurations.addAll(Arrays.asList(autoConfigurations));
		}
		return self();
	}

	/**
	 * Add the specified auto-configurations at the beginning (in that order) so that it
	 * is applied before any other existing auto-configurations, but after any user
	 * configuration. If {@code A} and {@code B} are specified, {@code A} will be
	 * processed, then {@code B} and finally the rest of the existing auto-configuration.
	 * @param autoConfigurations the auto-configuration to add
	 * @return this instance
	 */
	@Override
	public L autoConfigFirst(Class<?>... autoConfigurations) {
		this.autoConfigurations.addAll(0, Arrays.asList(autoConfigurations));
		return self();
	}

	/**
	 * Customize the {@link ClassLoader} that the {@link ApplicationContext} should use.
	 * Customizing the {@link ClassLoader} is an effective manner to hide resources from
	 * the classpath.
	 * @param classLoader the classloader to use (can be null to use the default)
	 * @return this instance
	 * @see HidePackagesClassLoader
	 */
	@Override
	public L classLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
		return self();
	}

	@SuppressWarnings("unchecked")
	protected final L self() {
		return (L) this;
	}

	/**
	 * Create and refresh a new {@link ApplicationContext} based on the current state of
	 * this loader. The context is consumed by the specified {@link ContextConsumer} and
	 * closed upon completion.
	 * @param consumer the consumer of the created {@link ApplicationContext}
	 */
	@Override
	public void load(ContextConsumer consumer) {
		doLoad(consumer::accept);
	}

	protected void doLoad(ContextHandler<T> contextHandler) {
		try (ApplicationContextLifecycleHandler handler = new ApplicationContextLifecycleHandler()) {
			try {
				T ctx = handler.load();
				contextHandler.handle(ctx);
			}
			catch (RuntimeException ex) {
				throw ex;
			}
			catch (Throwable ex) {
				throw new IllegalStateException(
						"An unexpected error occurred: " + ex.getMessage(), ex);
			}
		}
	}

	/**
	 * Create and refresh a new {@link ApplicationContext} based on the current state of
	 * this loader that this expected to fail. If the context does not fail, an
	 * {@link AssertionError} is thrown. Otherwise the exception is consumed by the
	 * specified {@link Consumer} with no expectation on the type of the exception.
	 * @param consumer the consumer of the failure
	 */
	@Override
	public void loadAndFail(Consumer<Throwable> consumer) {
		loadAndFail(Throwable.class, consumer);
	}

	/**
	 * Create and refresh a new {@link ApplicationContext} based on the current state of
	 * this loader that this expected to fail. If the context does not fail, an
	 * {@link AssertionError} is thrown. If the exception does not match the specified
	 * {@code exceptionType}, an {@link AssertionError} is thrown as well. If the
	 * exception type matches, it is consumed by the specified {@link Consumer}.
	 * @param exceptionType the expected type of the failure
	 * @param consumer the consumer of the failure
	 * @param <E> the expected type of the failure
	 */
	@Override
	public <E extends Throwable> void loadAndFail(Class<E> exceptionType,
			Consumer<E> consumer) {
		try (ApplicationContextLifecycleHandler handler = new ApplicationContextLifecycleHandler()) {
			handler.load();
			throw new AssertionError("ApplicationContext should have failed");
		}
		catch (Throwable ex) {
			assertThat(ex).as("Wrong application context failure exception")
					.isInstanceOf(exceptionType);
			consumer.accept(exceptionType.cast(ex));
		}
	}

	private T configureApplicationContext() {
		T context = AbstractContextLoader.this.contextSupplier.get();
		if (this.classLoader != null) {
			Assert.isInstanceOf(DefaultResourceLoader.class, context);
			((DefaultResourceLoader) context).setClassLoader(this.classLoader);
		}
		if (!ObjectUtils.isEmpty(this.env)) {
			TestPropertyValues.of(this.env.toArray(new String[this.env.size()]))
					.applyTo(context);
		}
		if (!ObjectUtils.isEmpty(this.userConfigurations)) {
			((AnnotationConfigRegistry) context).register(this.userConfigurations
					.toArray(new Class<?>[this.userConfigurations.size()]));
		}
		if (!ObjectUtils.isEmpty(this.autoConfigurations)) {
			LinkedHashSet<Class<?>> linkedHashSet = new LinkedHashSet<>(
					this.autoConfigurations);
			((AnnotationConfigRegistry) context).register(
					linkedHashSet.toArray(new Class<?>[this.autoConfigurations.size()]));
		}
		return context;
	}

	/**
	 * An internal callback interface that handles a concrete {@link ApplicationContext}
	 * type.
	 * @param <T> the type of the application context
	 */
	protected interface ContextHandler<T> {

		void handle(T context) throws Throwable;

	}

	/**
	 * Handles the lifecycle of the {@link ApplicationContext}.
	 */
	private class ApplicationContextLifecycleHandler implements Closeable {

		private final Map<String, String> customSystemProperties;

		private final Map<String, String> previousSystemProperties = new HashMap<>();

		private ConfigurableApplicationContext context;

		ApplicationContextLifecycleHandler() {
			this.customSystemProperties = new HashMap<>(
					AbstractContextLoader.this.systemProperties);
		}

		public T load() {
			setCustomSystemProperties();
			T context = configureApplicationContext();
			context.refresh();
			this.context = context;
			return context;
		}

		@Override
		public void close() {
			try {
				if (this.context != null) {
					this.context.close();
				}
			}
			finally {
				unsetCustomSystemProperties();
			}
		}

		private void setCustomSystemProperties() {
			this.customSystemProperties.forEach((key, value) -> {
				String previous = System.setProperty(key, value);
				this.previousSystemProperties.put(key, previous);
			});
		}

		private void unsetCustomSystemProperties() {
			this.previousSystemProperties.forEach((key, value) -> {
				if (value != null) {
					System.setProperty(key, value);
				}
				else {
					System.clearProperty(key);
				}
			});
		}

	}

}
