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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Manage the lifecycle of an {@link ApplicationContext}. Such helper is best used as a
 * field of a test class, describing the shared configuration required for the test:
 *
 * <pre class="code">
 * public class FooAutoConfigurationTests {
 *
 *     private final ContextLoader contextLoader = new ContextLoader()
 *             .autoConfig(FooAutoConfiguration.class).env("spring.foo=bar");
 *
 * }</pre>
 *
 * <p>
 * The initialization above makes sure to register {@code FooAutoConfiguration} for all
 * tests and set the {@code spring.foo} property to {@code bar} unless specified
 * otherwise.
 *
 * <p>
 * Based on the configuration above, a specific test can simulate what would happen if the
 * user customizes a property and/or provides its own configuration:
 *
 * <pre class="code">
 * public class FooAutoConfigurationTests {
 *
 *     &#064;Test
 *     public someTest() {
 *         this.contextLoader.config(UserConfig.class).env("spring.foo=biz")
 *                 .load(context -&gt; {
 *            			// assertions using the context
 *         });
 *     }
 *
 * }</pre>
 *
 * <p>
 * The test above includes an extra {@code UserConfig} class that is guaranteed to be
 * processed <strong>before</strong> any auto-configuration. Also, {@code spring.foo} has
 * been overwritten to {@code biz}. The {@link #load(ContextConsumer) load} method takes a
 * consumer that can use the context to assert its state. Upon completion, the context is
 * automatically closed.
 *
 * <p>
 * If a failure scenario has to be tested, {@link #loadAndFail(Consumer)} can be used
 * instead: it expects the startup of the context to fail and call the {@link Consumer}
 * with the exception for further assertions.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class ContextLoader {

	private final Map<String, String> systemProperties = new HashMap<>();

	private final List<String> env = new ArrayList<>();

	private final Set<Class<?>> userConfigurations = new LinkedHashSet<>();

	private final LinkedList<Class<?>> autoConfigurations = new LinkedList<>();

	private ClassLoader classLoader;

	/**
	 * Set the specified system property prior to loading the context and restore its
	 * previous value once the consumer has been invoked and the context closed. If the
	 * {@code value} is {@code null} this removes any prior customization for that key.
	 * @param key the system property
	 * @param value the value (can be null to remove any existing customization)
	 * @return this instance
	 */
	public ContextLoader systemProperty(String key, String value) {
		Assert.notNull(key, "Key must not be null");
		if (value != null) {
			this.systemProperties.put(key, value);
		}
		else {
			this.systemProperties.remove(key);
		}
		return this;
	}

	/**
	 * Add the specified property pairs. Key-value pairs can be specified with colon (":")
	 * or equals ("=") separators. Override matching keys that might have been specified
	 * previously.
	 * @param pairs the key-value pairs for properties that need to be added to the
	 * environment
	 * @return this instance
	 */
	public ContextLoader env(String... pairs) {
		if (!ObjectUtils.isEmpty(pairs)) {
			this.env.addAll(Arrays.asList(pairs));
		}
		return this;
	}

	/**
	 * Add the specified user configuration classes.
	 * @param configs the user configuration classes to add
	 * @return this instance
	 */
	public ContextLoader config(Class<?>... configs) {
		if (!ObjectUtils.isEmpty(configs)) {
			this.userConfigurations.addAll(Arrays.asList(configs));
		}
		return this;
	}

	/**
	 * Add the specified auto-configuration classes.
	 * @param autoConfigurations the auto-configuration classes to add
	 * @return this instance
	 */
	public ContextLoader autoConfig(Class<?>... autoConfigurations) {
		if (!ObjectUtils.isEmpty(autoConfigurations)) {
			this.autoConfigurations.addAll(Arrays.asList(autoConfigurations));
		}
		return this;
	}

	/**
	 * Add the specified auto-configurations at the beginning (in that order) so that it
	 * is applied before any other existing auto-configurations, but after any user
	 * configuration. If {@code A} and {@code B} are specified, {@code A} will be
	 * processed, then {@code B} and finally the rest of the existing auto-configuration.
	 * @param autoConfigurations the auto-configuration to add
	 * @return this instance
	 */
	public ContextLoader autoConfigFirst(Class<?>... autoConfigurations) {
		this.autoConfigurations.addAll(0, Arrays.asList(autoConfigurations));
		return this;
	}

	/**
	 * Customize the {@link ClassLoader} that the {@link ApplicationContext} should use.
	 * Customizing the {@link ClassLoader} is an effective manner to hide resources from
	 * the classpath.
	 * @param classLoader the classloader to use (can be null to use the default)
	 * @return this instance
	 * @see HidePackagesClassLoader
	 */
	public ContextLoader classLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
		return this;
	}

	/**
	 * Create and refresh a new {@link ApplicationContext} based on the current state of
	 * this loader. The context is consumed by the specified {@link ContextConsumer} and
	 * closed upon completion.
	 * @param consumer the consumer of the created {@link ApplicationContext}
	 */
	public void load(ContextConsumer consumer) {
		try (ApplicationContextLifecycleHandler handler = new ApplicationContextLifecycleHandler()) {
			try {
				ConfigurableApplicationContext ctx = handler.load();
				consumer.accept(ctx);
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
	 * @param <T> the expected type of the failure
	 */
	public <T extends Throwable> void loadAndFail(Class<T> exceptionType,
			Consumer<T> consumer) {
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

	private ConfigurableApplicationContext createApplicationContext() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		if (this.classLoader != null) {
			ctx.setClassLoader(this.classLoader);
		}
		if (!ObjectUtils.isEmpty(this.env)) {
			TestPropertyValues.of(this.env.toArray(new String[this.env.size()]))
					.applyTo(ctx);
		}
		if (!ObjectUtils.isEmpty(this.userConfigurations)) {
			ctx.register(this.userConfigurations
					.toArray(new Class<?>[this.userConfigurations.size()]));
		}
		if (!ObjectUtils.isEmpty(this.autoConfigurations)) {
			LinkedHashSet<Class<?>> linkedHashSet = new LinkedHashSet<>(
					this.autoConfigurations);
			ctx.register(
					linkedHashSet.toArray(new Class<?>[this.autoConfigurations.size()]));
		}
		return ctx;
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
					ContextLoader.this.systemProperties);
		}

		public ConfigurableApplicationContext load() {
			setCustomSystemProperties();
			ConfigurableApplicationContext context = createApplicationContext();
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
