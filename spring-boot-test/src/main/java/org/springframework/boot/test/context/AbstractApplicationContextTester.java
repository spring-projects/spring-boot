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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.boot.context.annotation.Configurations;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Tester utility design to manage the lifecycle of an {@link ApplicationContext} and
 * provide AssertJ style assertions. The test is best used as a field of a test class,
 * describing the shared configuration required for the test:
 *
 * <pre class="code">
 * public class MyContextTests {
 *     private final ApplicationContextTester context = new ApplicationContextTester()
 *             .withPropertyValues("spring.foo=bar")
 *             .withUserConfiguration(MyConfiguration.class);
 * }</pre>
 *
 * <p>
 * The initialization above makes sure to register {@code MyConfiguration} for all tests
 * and set the {@code spring.foo} property to {@code bar} unless specified otherwise.
 * <p>
 * Based on the configuration above, a specific test can simulate what will happen when
 * the context runs, perhaps with overridden property values:
 *
 * <pre class="code">
 * &#064;Test
 * public someTest() {
 *     this.context.withPropertyValues("spring.foo=biz").run((loaded) -&gt; {
 *         assertThat(loaded).containsSingleBean(MyBean.class);
 *         // other assertions
 *     });
 * }</pre>
 * <p>
 * The test above has changed the {@code spring.foo} property to {@code biz} and is
 * asserting that the context contains a single {@code MyBean} bean. The
 * {@link #run(ContextConsumer) run} method takes a {@link ContextConsumer} that can apply
 * assertions to the context. Upon completion, the context is automatically closed.
 * <p>
 * If the application context fails to start the {@code #run(ContextConsumer)} method is
 * called with a "failed" application context. Calls to the context will throw an
 * {@link IllegalStateException} and assertions that expect a running context will fail.
 * The {@link ApplicationContextAssert#getFailure() getFailure()} assertion can be used if
 * further checks are required on the cause of the failure: <pre class="code">
 * &#064;Test
 * public someTest() {
 *     this.context.withPropertyValues("spring.foo=fails").run((loaded) -&gt; {
 *         assertThat(loaded).getFailure().hasCauseInstanceOf(BadPropertyException.class);
 *         // other assertions
 *     });
 * }</pre>
 * <p>
 *
 * @param <SELF> The "self" type for this tester
 * @param <C> The context type
 * @param <A> The application context assertion provider
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.0.0
 * @see ApplicationContextTester
 * @see WebApplicationContextTester
 * @see ReactiveWebApplicationContextTester
 * @see ApplicationContextAssert
 */
abstract class AbstractApplicationContextTester<SELF extends AbstractApplicationContextTester<SELF, C, A>, C extends ConfigurableApplicationContext, A extends AssertProviderApplicationContext<C>> {

	private final Supplier<C> contextFactory;

	private final TestPropertyValues environmentProperties;

	private final TestPropertyValues systemProperties;

	private ClassLoader classLoader;

	private ApplicationContext parent;

	private final List<Configurations> configurations = new ArrayList<>();

	/**
	 * Create a new {@link AbstractApplicationContextTester} instance.
	 * @param contextFactory the factory used to create the actual context
	 */
	protected AbstractApplicationContextTester(Supplier<C> contextFactory) {
		Assert.notNull(contextFactory, "ContextFactory must not be null");
		this.contextFactory = contextFactory;
		this.environmentProperties = TestPropertyValues.empty();
		this.systemProperties = TestPropertyValues.empty();
	}

	/**
	 * Add the specified {@link Environment} property pairs. Key-value pairs can be
	 * specified with colon (":") or equals ("=") separators. Override matching keys that
	 * might have been specified previously.
	 * @param pairs the key-value pairs for properties that need to be added to the
	 * environment
	 * @return this instance
	 * @see TestPropertyValues
	 * @see #withSystemProperties(String...)
	 */
	public SELF withPropertyValues(String... pairs) {
		Arrays.stream(pairs).forEach(this.environmentProperties::and);
		return self();
	}

	/**
	 * Add the specified {@link Environment} property.
	 * @param name the name of the property
	 * @param value the value of the property
	 * @return this instance
	 * @see TestPropertyValues
	 * @see #withSystemProperties(String...)
	 */
	public SELF withPropertyValue(String name, String value) {
		this.environmentProperties.and(name, value);
		return self();
	}

	/**
	 * Add the specified {@link System} property pairs. Key-value pairs can be specified
	 * with colon (":") or equals ("=") separators. System properties are added before the
	 * context is {@link #run(ContextConsumer) run} and restored when the context is
	 * closed.
	 * @param pairs the key-value pairs for properties that need to be added to the system
	 * @return this instance
	 * @see TestPropertyValues
	 * @see #withSystemProperties(String...)
	 */
	public SELF withSystemProperties(String... pairs) {
		Arrays.stream(pairs).forEach(this.systemProperties::and);
		return self();
	}

	/**
	 * Add the specified {@link System} property. System properties are added before the
	 * context is {@link #run(ContextConsumer) run} and restored when the context is
	 * closed.
	 * @param name the property name
	 * @param value the property value
	 * @return this instance
	 * @see TestPropertyValues
	 * @see #withSystemProperties(String...)
	 */
	public SELF withSystemProperty(String name, String value) {
		this.systemProperties.and(name, value);
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
	public SELF withClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
		return self();
	}

	/**
	 * Configure the {@link ConfigurableApplicationContext#setParent(ApplicationContext)
	 * parent} of the {@link ApplicationContext}.
	 * @param parent the parent
	 * @return this instance
	 */
	public SELF withParent(ApplicationContext parent) {
		this.parent = parent;
		return self();
	}

	/**
	 * Register the specified user configuration classes with the
	 * {@link ApplicationContext}.
	 * @param configurationClasses the user configuration classes to add
	 * @return this instance
	 */
	public SELF withUserConfiguration(Class<?>... configurationClasses) {
		return withConfiguration(UserConfigurations.of(configurationClasses));
	}

	/**
	 * Register the specified configuration classes with the {@link ApplicationContext}.
	 * @param configurations the configurations to add
	 * @return this instance
	 */
	public SELF withConfiguration(Configurations configurations) {
		Assert.notNull(configurations, "Configurations must not be null");
		this.configurations.add(configurations);
		return self();
	}

	@SuppressWarnings("unchecked")
	protected final SELF self() {
		return (SELF) this;
	}

	/**
	 * Create and refresh a new {@link ApplicationContext} based on the current state of
	 * this loader. The context is consumed by the specified {@code consumer} and closed
	 * upon completion.
	 * @param consumer the consumer of the created {@link ApplicationContext}
	 */
	public void run(ContextConsumer<? super A> consumer) {
		this.systemProperties.applyToSystemProperties(() -> {
			try (A context = createAssertableContext()) {
				accept(consumer, context);
			}
			return null;
		});
	}

	@SuppressWarnings("unchecked")
	private A createAssertableContext() {
		ResolvableType resolvableType = ResolvableType
				.forClass(AbstractApplicationContextTester.class, getClass());
		Class<A> assertType = (Class<A>) resolvableType.resolveGeneric(1);
		Class<C> contextType = (Class<C>) resolvableType.resolveGeneric(2);
		return AssertProviderApplicationContext.get(assertType, contextType,
				this::createAndLoadContext);
	}

	private C createAndLoadContext() {
		C context = this.contextFactory.get();
		try {
			configureContext(context);
			return context;
		}
		catch (RuntimeException ex) {
			context.close();
			throw ex;
		}
	}

	private void configureContext(C context) {
		if (this.parent != null) {
			context.setParent(this.parent);
		}
		if (this.classLoader != null) {
			Assert.isInstanceOf(DefaultResourceLoader.class, context);
			((DefaultResourceLoader) context).setClassLoader(this.classLoader);
		}
		this.environmentProperties.applyTo(context);
		Class<?>[] classes = Configurations.getClasses(this.configurations);
		if (classes.length > 0) {
			((AnnotationConfigRegistry) context).register(classes);
		}
		context.refresh();
	}

	private void accept(ContextConsumer<? super A> consumer, A context) {
		try {
			consumer.accept(context);
		}
		catch (Throwable ex) {
			ReflectionUtils.rethrowRuntimeException(ex);
		}
	}

}
