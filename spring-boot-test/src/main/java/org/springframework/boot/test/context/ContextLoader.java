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

import java.util.function.Consumer;

import org.springframework.boot.web.reactive.context.GenericReactiveWebApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * Manage the lifecycle of an {@link ApplicationContext}. Such helper is best used as a
 * field of a test class, describing the shared configuration required for the test:
 *
 * <pre class="code">
 * public class FooAutoConfigurationTests {
 *
 *     private final ContextLoader contextLoader = ContextLoader.standard()
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
 * Web environment can easily be simulated using the {@link #servletWeb()} and
 * {@link #reactiveWeb()} factory methods.
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
public interface ContextLoader {

	/**
	 * Creates a {@code ContextLoader} that will load a standard
	 * {@link AnnotationConfigApplicationContext}.
	 *
	 * @return the context loader
	 */
	static StandardContextLoader standard() {
		return new StandardContextLoader(AnnotationConfigApplicationContext::new);
	}

	/**
	 * Creates a {@code ContextLoader} that will load a
	 * {@link AnnotationConfigWebApplicationContext}.
	 *
	 * @return the context loader
	 */
	static ServletWebContextLoader servletWeb() {
		return new ServletWebContextLoader(() -> {
			AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
			context.setServletContext(new MockServletContext());
			return context;
		});
	}

	/**
	 * Creates a {@code ContextLoader} that will load a
	 * {@link GenericReactiveWebApplicationContext}.
	 *
	 * @return the context loader
	 */
	static ReactiveWebContextLoader reactiveWeb() {
		return new ReactiveWebContextLoader(GenericReactiveWebApplicationContext::new);
	}

	/**
	 * Set the specified system property prior to loading the context and restore its
	 * previous value once the consumer has been invoked and the context closed. If the
	 * {@code value} is {@code null} this removes any prior customization for that key.
	 * @param key the system property
	 * @param value the value (can be null to remove any existing customization)
	 * @return this instance
	 */
	ContextLoader systemProperty(String key, String value);

	/**
	 * Add the specified property pairs. Key-value pairs can be specified with colon (":")
	 * or equals ("=") separators. Override matching keys that might have been specified
	 * previously.
	 * @param pairs the key-value pairs for properties that need to be added to the
	 * environment
	 * @return this instance
	 */
	ContextLoader env(String... pairs);

	/**
	 * Add the specified user configuration classes.
	 * @param configs the user configuration classes to add
	 * @return this instance
	 */
	ContextLoader config(Class<?>... configs);

	/**
	 * Add the specified auto-configuration classes.
	 * @param autoConfigurations the auto-configuration classes to add
	 * @return this instance
	 */
	ContextLoader autoConfig(Class<?>... autoConfigurations);

	/**
	 * Add the specified auto-configurations at the beginning (in that order) so that it
	 * is applied before any other existing auto-configurations, but after any user
	 * configuration. If {@code A} and {@code B} are specified, {@code A} will be
	 * processed, then {@code B} and finally the rest of the existing auto-configuration.
	 * @param autoConfigurations the auto-configuration to add
	 * @return this instance
	 */
	ContextLoader autoConfigFirst(Class<?>... autoConfigurations);

	/**
	 * Customize the {@link ClassLoader} that the {@link ApplicationContext} should use.
	 * Customizing the {@link ClassLoader} is an effective manner to hide resources from
	 * the classpath.
	 * @param classLoader the classloader to use (can be null to use the default)
	 * @return this instance
	 * @see HidePackagesClassLoader
	 */
	ContextLoader classLoader(ClassLoader classLoader);

	/**
	 * Create and refresh a new {@link ApplicationContext} based on the current state of
	 * this loader. The context is consumed by the specified {@code consumer} and closed
	 * upon completion.
	 * @param consumer the consumer of the created {@link ApplicationContext}
	 */
	void load(ContextConsumer consumer);

	/**
	 * Create and refresh a new {@link ApplicationContext} based on the current state of
	 * this loader that this expected to fail. If the context does not fail, an
	 * {@link AssertionError} is thrown. Otherwise the exception is consumed by the
	 * specified {@link Consumer} with no expectation on the type of the exception.
	 * @param consumer the consumer of the failure
	 */
	void loadAndFail(Consumer<Throwable> consumer);

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
	<E extends Throwable> void loadAndFail(Class<E> exceptionType, Consumer<E> consumer);

}
