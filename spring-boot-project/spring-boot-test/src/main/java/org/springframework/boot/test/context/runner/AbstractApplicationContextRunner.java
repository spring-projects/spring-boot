/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.test.context.runner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.context.annotation.Configurations;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.ApplicationContextAssert;
import org.springframework.boot.test.context.assertj.ApplicationContextAssertProvider;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigRegistry;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.util.Assert;

/**
 * Utility design to run an {@link ApplicationContext} and provide AssertJ style
 * assertions. The test is best used as a field of a test class, describing the shared
 * configuration required for the test:
 *
 * <pre class="code">
 * public class MyContextTests {
 *     private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
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
 *     this.contextRunner.withPropertyValues("spring.foo=biz").run((context) -&gt; {
 *         assertThat(context).containsSingleBean(MyBean.class);
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
 * @param <SELF> the "self" type for this runner
 * @param <C> the context type
 * @param <A> the application context assertion provider
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.0.0
 * @see ApplicationContextRunner
 * @see WebApplicationContextRunner
 * @see ReactiveWebApplicationContextRunner
 * @see ApplicationContextAssert
 */
public abstract class AbstractApplicationContextRunner<SELF extends AbstractApplicationContextRunner<SELF, C, A>, C extends ConfigurableApplicationContext, A extends ApplicationContextAssertProvider<C>> {

	private final RunnerConfiguration<C> runnerConfiguration;

	private final Function<RunnerConfiguration<C>, SELF> instanceFactory;

	/**
	 * Create a new {@link AbstractApplicationContextRunner} instance.
	 * @param contextFactory the factory used to create the actual context
	 * @param instanceFactory the factory used to create new instance of the runner
	 * @since 2.6.0
	 */
	protected AbstractApplicationContextRunner(Supplier<C> contextFactory,
			Function<RunnerConfiguration<C>, SELF> instanceFactory) {
		Assert.notNull(contextFactory, "ContextFactory must not be null");
		Assert.notNull(contextFactory, "RunnerConfiguration must not be null");
		this.runnerConfiguration = new RunnerConfiguration<>(contextFactory);
		this.instanceFactory = instanceFactory;
	}

	/**
	 * Create a new {@link AbstractApplicationContextRunner} instance.
	 * @param configuration the configuration for the runner to use
	 * @param instanceFactory the factory used to create new instance of the runner
	 * @since 2.6.0
	 */
	protected AbstractApplicationContextRunner(RunnerConfiguration<C> configuration,
			Function<RunnerConfiguration<C>, SELF> instanceFactory) {
		Assert.notNull(configuration, "RunnerConfiguration must not be null");
		Assert.notNull(instanceFactory, "instanceFactory must not be null");
		this.runnerConfiguration = configuration;
		this.instanceFactory = instanceFactory;
	}

	/**
	 * Specify if bean definition overriding, by registering a definition with the same
	 * name as an existing definition, should be allowed.
	 * @param allowBeanDefinitionOverriding if bean overriding is allowed
	 * @return a new instance with the updated bean definition overriding policy
	 * @since 2.3.0
	 * @see DefaultListableBeanFactory#setAllowBeanDefinitionOverriding(boolean)
	 */
	public SELF withAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
		return newInstance(this.runnerConfiguration.withAllowBeanDefinitionOverriding(allowBeanDefinitionOverriding));
	}

	/**
	 * Specify if circular references between beans should be allowed.
	 * @param allowCircularReferences if circular references between beans are allowed
	 * @return a new instance with the updated circular references policy
	 * @since 2.6.0
	 * @see AbstractAutowireCapableBeanFactory#setAllowCircularReferences(boolean)
	 */
	public SELF withAllowCircularReferences(boolean allowCircularReferences) {
		return newInstance(this.runnerConfiguration.withAllowCircularReferences(allowCircularReferences));
	}

	/**
	 * Add an {@link ApplicationContextInitializer} to be called when the context is
	 * created.
	 * @param initializer the initializer to add
	 * @return a new instance with the updated initializers
	 */
	public SELF withInitializer(ApplicationContextInitializer<? super C> initializer) {
		Assert.notNull(initializer, "Initializer must not be null");
		return newInstance(this.runnerConfiguration.withInitializer(initializer));
	}

	/**
	 * Add the specified {@link Environment} property pairs. Key-value pairs can be
	 * specified with colon (":") or equals ("=") separators. Override matching keys that
	 * might have been specified previously.
	 * @param pairs the key-value pairs for properties that need to be added to the
	 * environment
	 * @return a new instance with the updated property values
	 * @see TestPropertyValues
	 * @see #withSystemProperties(String...)
	 */
	public SELF withPropertyValues(String... pairs) {
		return newInstance(this.runnerConfiguration.withPropertyValues(pairs));
	}

	/**
	 * Add the specified {@link System} property pairs. Key-value pairs can be specified
	 * with colon (":") or equals ("=") separators. System properties are added before the
	 * context is {@link #run(ContextConsumer) run} and restored when the context is
	 * closed.
	 * @param pairs the key-value pairs for properties that need to be added to the system
	 * @return a new instance with the updated system properties
	 * @see TestPropertyValues
	 * @see #withSystemProperties(String...)
	 */
	public SELF withSystemProperties(String... pairs) {
		return newInstance(this.runnerConfiguration.withSystemProperties(pairs));
	}

	/**
	 * Customize the {@link ClassLoader} that the {@link ApplicationContext} should use
	 * for resource loading and bean class loading.
	 * @param classLoader the classloader to use (or {@code null} to use the default)
	 * @return a new instance with the updated class loader
	 * @see FilteredClassLoader
	 */
	public SELF withClassLoader(ClassLoader classLoader) {
		return newInstance(this.runnerConfiguration.withClassLoader(classLoader));
	}

	/**
	 * Configure the {@link ConfigurableApplicationContext#setParent(ApplicationContext)
	 * parent} of the {@link ApplicationContext}.
	 * @param parent the parent
	 * @return a new instance with the updated parent
	 */
	public SELF withParent(ApplicationContext parent) {
		return newInstance(this.runnerConfiguration.withParent(parent));
	}

	/**
	 * Register the specified user bean with the {@link ApplicationContext}. The bean name
	 * is generated from the configured {@link BeanNameGenerator} on the underlying
	 * context.
	 * <p>
	 * Such beans are registered after regular {@linkplain #withUserConfiguration(Class[])
	 * user configurations} in the order of registration.
	 * @param type the type of the bean
	 * @param constructorArgs custom argument values to be fed into Spring's constructor
	 * resolution algorithm, resolving either all arguments or just specific ones, with
	 * the rest to be resolved through regular autowiring (may be {@code null} or empty)
	 * @param <T> the type of the bean
	 * @return a new instance with the updated bean
	 */
	public <T> SELF withBean(Class<T> type, Object... constructorArgs) {
		return withBean(null, type, constructorArgs);
	}

	/**
	 * Register the specified user bean with the {@link ApplicationContext}.
	 * <p>
	 * Such beans are registered after regular {@linkplain #withUserConfiguration(Class[])
	 * user configurations} in the order of registration.
	 * @param name the bean name or {@code null} to use a generated name
	 * @param type the type of the bean
	 * @param constructorArgs custom argument values to be fed into Spring's constructor
	 * resolution algorithm, resolving either all arguments or just specific ones, with
	 * the rest to be resolved through regular autowiring (may be {@code null} or empty)
	 * @param <T> the type of the bean
	 * @return a new instance with the updated bean
	 */
	public <T> SELF withBean(String name, Class<T> type, Object... constructorArgs) {
		return newInstance(this.runnerConfiguration.withBean(name, type, constructorArgs));
	}

	/**
	 * Register the specified user bean with the {@link ApplicationContext}. The bean name
	 * is generated from the configured {@link BeanNameGenerator} on the underlying
	 * context.
	 * <p>
	 * Such beans are registered after regular {@linkplain #withUserConfiguration(Class[])
	 * user configurations} in the order of registration.
	 * @param type the type of the bean
	 * @param supplier a supplier for the bean
	 * @param customizers one or more callbacks for customizing the factory's
	 * {@link BeanDefinition}, e.g. setting a lazy-init or primary flag
	 * @param <T> the type of the bean
	 * @return a new instance with the updated bean
	 */
	public <T> SELF withBean(Class<T> type, Supplier<T> supplier, BeanDefinitionCustomizer... customizers) {
		return withBean(null, type, supplier, customizers);
	}

	/**
	 * Register the specified user bean with the {@link ApplicationContext}. The bean name
	 * is generated from the configured {@link BeanNameGenerator} on the underlying
	 * context.
	 * <p>
	 * Such beans are registered after regular {@linkplain #withUserConfiguration(Class[])
	 * user configurations} in the order of registration.
	 * @param name the bean name or {@code null} to use a generated name
	 * @param type the type of the bean
	 * @param supplier a supplier for the bean
	 * @param customizers one or more callbacks for customizing the factory's
	 * {@link BeanDefinition}, e.g. setting a lazy-init or primary flag
	 * @param <T> the type of the bean
	 * @return a new instance with the updated bean
	 */
	public <T> SELF withBean(String name, Class<T> type, Supplier<T> supplier,
			BeanDefinitionCustomizer... customizers) {
		return newInstance(this.runnerConfiguration.withBean(name, type, supplier, customizers));
	}

	/**
	 * Register the specified user configuration classes with the
	 * {@link ApplicationContext}.
	 * @param configurationClasses the user configuration classes to add
	 * @return a new instance with the updated configuration
	 */
	public SELF withUserConfiguration(Class<?>... configurationClasses) {
		return withConfiguration(UserConfigurations.of(configurationClasses));
	}

	/**
	 * Register the specified configuration classes with the {@link ApplicationContext}.
	 * @param configurations the configurations to add
	 * @return a new instance with the updated configuration
	 */
	public SELF withConfiguration(Configurations configurations) {
		Assert.notNull(configurations, "Configurations must not be null");
		return newInstance(this.runnerConfiguration.withConfiguration(configurations));
	}

	/**
	 * Apply customization to this runner.
	 * @param customizer the customizer to call
	 * @return a new instance with the customizations applied
	 */
	@SuppressWarnings("unchecked")
	public SELF with(Function<SELF, SELF> customizer) {
		return customizer.apply((SELF) this);
	}

	/**
	 * Creates a new instance of the AbstractApplicationContextRunner class with the
	 * provided runner configuration.
	 * @param runnerConfiguration the runner configuration to be used for creating the new
	 * instance
	 * @return a new instance of the AbstractApplicationContextRunner class
	 */
	private SELF newInstance(RunnerConfiguration<C> runnerConfiguration) {
		return this.instanceFactory.apply(runnerConfiguration);
	}

	/**
	 * Create and refresh a new {@link ApplicationContext} based on the current state of
	 * this loader. The context is consumed by the specified {@code consumer} and closed
	 * upon completion.
	 * @param consumer the consumer of the created {@link ApplicationContext}
	 * @return this instance
	 */
	@SuppressWarnings("unchecked")
	public SELF run(ContextConsumer<? super A> consumer) {
		withContextClassLoader(this.runnerConfiguration.classLoader, () -> this.runnerConfiguration.systemProperties
			.applyToSystemProperties(() -> consumeAssertableContext(true, consumer)));
		return (SELF) this;
	}

	/**
	 * Prepare a new {@link ApplicationContext} based on the current state of this loader.
	 * The context is consumed by the specified {@code consumer} and closed upon
	 * completion. Unlike {@link #run(ContextConsumer)}, this method does not refresh the
	 * consumed context.
	 * @param consumer the consumer of the created {@link ApplicationContext}
	 * @return this instance
	 * @since 3.0.0
	 */
	@SuppressWarnings("unchecked")
	public SELF prepare(ContextConsumer<? super A> consumer) {
		withContextClassLoader(this.runnerConfiguration.classLoader, () -> this.runnerConfiguration.systemProperties
			.applyToSystemProperties(() -> consumeAssertableContext(false, consumer)));
		return (SELF) this;
	}

	/**
	 * Consumes an assertable context by creating it with the specified refresh flag and
	 * passing it to the provided consumer.
	 * @param refresh a boolean indicating whether the context should be refreshed
	 * @param consumer the consumer to accept the assertable context
	 * @param <A> the type of the assertable context
	 */
	private void consumeAssertableContext(boolean refresh, ContextConsumer<? super A> consumer) {
		try (A context = createAssertableContext(refresh)) {
			accept(consumer, context);
		}
	}

	/**
	 * Executes the specified action with the given class loader as the context class
	 * loader. If the class loader is null, the action is executed without changing the
	 * context class loader.
	 * @param classLoader the class loader to set as the context class loader
	 * @param action the action to be executed
	 */
	private void withContextClassLoader(ClassLoader classLoader, Runnable action) {
		if (classLoader == null) {
			action.run();
		}
		else {
			Thread currentThread = Thread.currentThread();
			ClassLoader previous = currentThread.getContextClassLoader();
			currentThread.setContextClassLoader(classLoader);
			try {
				action.run();
			}
			finally {
				currentThread.setContextClassLoader(previous);
			}
		}
	}

	/**
	 * Creates an assertable context.
	 * @param refresh a boolean value indicating whether the context should be refreshed
	 * @return an instance of the assertable context
	 */
	@SuppressWarnings("unchecked")
	private A createAssertableContext(boolean refresh) {
		ResolvableType resolvableType = ResolvableType.forClass(AbstractApplicationContextRunner.class, getClass());
		Class<A> assertType = (Class<A>) resolvableType.resolveGeneric(1);
		Class<C> contextType = (Class<C>) resolvableType.resolveGeneric(2);
		return ApplicationContextAssertProvider.get(assertType, contextType, () -> createAndLoadContext(refresh));
	}

	/**
	 * Creates and loads the application context.
	 * @param refresh a boolean value indicating whether to refresh the context
	 * @return the created and loaded application context
	 * @throws RuntimeException if an error occurs during configuration or context
	 * creation
	 */
	private C createAndLoadContext(boolean refresh) {
		C context = this.runnerConfiguration.contextFactory.get();
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		if (beanFactory instanceof AbstractAutowireCapableBeanFactory autowireCapableBeanFactory) {
			autowireCapableBeanFactory.setAllowCircularReferences(this.runnerConfiguration.allowCircularReferences);
			if (beanFactory instanceof DefaultListableBeanFactory listableBeanFactory) {
				listableBeanFactory
					.setAllowBeanDefinitionOverriding(this.runnerConfiguration.allowBeanDefinitionOverriding);
			}
		}
		try {
			configureContext(context, refresh);
			return context;
		}
		catch (RuntimeException ex) {
			context.close();
			throw ex;
		}
	}

	/**
	 * Configures the context with the given context and refresh flag.
	 * @param context the context to be configured
	 * @param refresh a flag indicating whether the context should be refreshed
	 */
	private void configureContext(C context, boolean refresh) {
		if (this.runnerConfiguration.parent != null) {
			context.setParent(this.runnerConfiguration.parent);
		}
		if (this.runnerConfiguration.classLoader != null) {
			Assert.isInstanceOf(DefaultResourceLoader.class, context);
			((DefaultResourceLoader) context).setClassLoader(this.runnerConfiguration.classLoader);
		}
		this.runnerConfiguration.environmentProperties.applyTo(context);
		this.runnerConfiguration.beanRegistrations.forEach((registration) -> registration.apply(context));
		this.runnerConfiguration.initializers.forEach((initializer) -> initializer.initialize(context));
		Class<?>[] classes = Configurations.getClasses(this.runnerConfiguration.configurations);
		if (classes.length > 0) {
			((AnnotationConfigRegistry) context).register(classes);
		}
		if (refresh) {
			context.refresh();
		}
	}

	/**
	 * Accepts a consumer function and a context object, and executes the consumer
	 * function with the context object.
	 * @param consumer the consumer function to be executed
	 * @param context the context object to be passed to the consumer function
	 * @throws Throwable if an exception occurs during the execution of the consumer
	 * function
	 */
	private void accept(ContextConsumer<? super A> consumer, A context) {
		try {
			consumer.accept(context);
		}
		catch (Throwable ex) {
			rethrow(ex);
		}
	}

	/**
	 * Rethrows the specified throwable.
	 * @param e the throwable to be rethrown
	 * @throws E the type of throwable to be thrown
	 * @since 1.0
	 */
	@SuppressWarnings("unchecked")
	private <E extends Throwable> void rethrow(Throwable e) throws E {
		throw (E) e;
	}

	/**
	 * A Bean registration to be applied when the context loaded.
	 *
	 * @param <T> the bean type
	 */
	protected static final class BeanRegistration<T> {

		Consumer<GenericApplicationContext> registrar;

		/**
		 * Creates a new instance of BeanRegistration with the specified name, type, and
		 * constructor arguments.
		 * @param name the name of the bean to be registered
		 * @param type the class type of the bean to be registered
		 * @param constructorArgs the constructor arguments to be used when creating the
		 * bean
		 */
		public BeanRegistration(String name, Class<T> type, Object... constructorArgs) {
			this.registrar = (context) -> context.registerBean(name, type, constructorArgs);
		}

		/**
		 * Creates a new instance of BeanRegistration with the specified parameters.
		 * @param name the name of the bean
		 * @param type the class type of the bean
		 * @param supplier the supplier function that provides the bean instance
		 * @param customizers optional customizers to apply to the bean definition
		 */
		public BeanRegistration(String name, Class<T> type, Supplier<T> supplier,
				BeanDefinitionCustomizer... customizers) {
			this.registrar = (context) -> context.registerBean(name, type, supplier, customizers);
		}

		/**
		 * Applies the given ConfigurableApplicationContext to the BeanRegistration.
		 * @param context the ConfigurableApplicationContext to be applied
		 * @throws IllegalArgumentException if the context is not an instance of
		 * GenericApplicationContext
		 */
		public void apply(ConfigurableApplicationContext context) {
			Assert.isInstanceOf(GenericApplicationContext.class, context);
			this.registrar.accept(((GenericApplicationContext) context));
		}

	}

	/**
	 * RunnerConfiguration class.
	 */
	protected static final class RunnerConfiguration<C extends ConfigurableApplicationContext> {

		private final Supplier<C> contextFactory;

		private boolean allowBeanDefinitionOverriding = false;

		private boolean allowCircularReferences = false;

		private List<ApplicationContextInitializer<? super C>> initializers = Collections.emptyList();

		private TestPropertyValues environmentProperties = TestPropertyValues.empty();

		private TestPropertyValues systemProperties = TestPropertyValues.empty();

		private ClassLoader classLoader;

		private ApplicationContext parent;

		private List<BeanRegistration<?>> beanRegistrations = Collections.emptyList();

		private List<Configurations> configurations = Collections.emptyList();

		/**
		 * Constructs a new RunnerConfiguration object with the given context factory.
		 * @param contextFactory the supplier that provides the context object for the
		 * runner
		 */
		private RunnerConfiguration(Supplier<C> contextFactory) {
			this.contextFactory = contextFactory;
		}

		/**
		 * Constructs a new RunnerConfiguration object by copying the values from the
		 * given source object.
		 * @param source the source RunnerConfiguration object to copy the values from
		 */
		private RunnerConfiguration(RunnerConfiguration<C> source) {
			this.contextFactory = source.contextFactory;
			this.allowBeanDefinitionOverriding = source.allowBeanDefinitionOverriding;
			this.allowCircularReferences = source.allowCircularReferences;
			this.initializers = source.initializers;
			this.environmentProperties = source.environmentProperties;
			this.systemProperties = source.systemProperties;
			this.classLoader = source.classLoader;
			this.parent = source.parent;
			this.beanRegistrations = source.beanRegistrations;
			this.configurations = source.configurations;
		}

		/**
		 * Sets whether bean definition overriding is allowed in the runner configuration.
		 * @param allowBeanDefinitionOverriding true if bean definition overriding is
		 * allowed, false otherwise
		 * @return a new RunnerConfiguration object with the updated
		 * allowBeanDefinitionOverriding value
		 */
		private RunnerConfiguration<C> withAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
			RunnerConfiguration<C> config = new RunnerConfiguration<>(this);
			config.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
			return config;
		}

		/**
		 * Sets whether circular references are allowed in the runner configuration.
		 * @param allowCircularReferences true if circular references are allowed, false
		 * otherwise
		 * @return a new RunnerConfiguration object with the updated
		 * allowCircularReferences value
		 */
		private RunnerConfiguration<C> withAllowCircularReferences(boolean allowCircularReferences) {
			RunnerConfiguration<C> config = new RunnerConfiguration<>(this);
			config.allowCircularReferences = allowCircularReferences;
			return config;
		}

		/**
		 * Adds an ApplicationContextInitializer to the RunnerConfiguration.
		 * @param initializer the ApplicationContextInitializer to be added
		 * @return a new RunnerConfiguration with the added initializer
		 * @throws IllegalArgumentException if the initializer is null
		 */
		private RunnerConfiguration<C> withInitializer(ApplicationContextInitializer<? super C> initializer) {
			Assert.notNull(initializer, "Initializer must not be null");
			RunnerConfiguration<C> config = new RunnerConfiguration<>(this);
			config.initializers = add(config.initializers, initializer);
			return config;
		}

		/**
		 * Sets the property values for the RunnerConfiguration.
		 * @param pairs the property key-value pairs to be set
		 * @return a new RunnerConfiguration object with the updated property values
		 */
		private RunnerConfiguration<C> withPropertyValues(String... pairs) {
			RunnerConfiguration<C> config = new RunnerConfiguration<>(this);
			config.environmentProperties = config.environmentProperties.and(pairs);
			return config;
		}

		/**
		 * Sets the system properties for the runner configuration.
		 * @param pairs the key-value pairs of system properties to be set
		 * @return a new RunnerConfiguration object with the updated system properties
		 */
		private RunnerConfiguration<C> withSystemProperties(String... pairs) {
			RunnerConfiguration<C> config = new RunnerConfiguration<>(this);
			config.systemProperties = config.systemProperties.and(pairs);
			return config;
		}

		/**
		 * Sets the class loader for the runner configuration.
		 * @param classLoader the class loader to be set
		 * @return a new RunnerConfiguration object with the specified class loader
		 */
		private RunnerConfiguration<C> withClassLoader(ClassLoader classLoader) {
			RunnerConfiguration<C> config = new RunnerConfiguration<>(this);
			config.classLoader = classLoader;
			return config;
		}

		/**
		 * Sets the parent ApplicationContext for this RunnerConfiguration.
		 * @param parent the parent ApplicationContext to set
		 * @return a new RunnerConfiguration with the specified parent ApplicationContext
		 */
		private RunnerConfiguration<C> withParent(ApplicationContext parent) {
			RunnerConfiguration<C> config = new RunnerConfiguration<>(this);
			config.parent = parent;
			return config;
		}

		/**
		 * Adds a bean registration to the runner configuration.
		 * @param name the name of the bean
		 * @param type the class type of the bean
		 * @param constructorArgs the constructor arguments for the bean
		 * @param <T> the generic type of the bean
		 * @return a new RunnerConfiguration with the added bean registration
		 */
		private <T> RunnerConfiguration<C> withBean(String name, Class<T> type, Object... constructorArgs) {
			RunnerConfiguration<C> config = new RunnerConfiguration<>(this);
			config.beanRegistrations = add(config.beanRegistrations,
					new BeanRegistration<>(name, type, constructorArgs));
			return config;
		}

		/**
		 * Adds a bean to the runner configuration.
		 * @param <T> the type of the bean
		 * @param name the name of the bean
		 * @param type the class of the bean
		 * @param supplier the supplier function to create the bean
		 * @param customizers the customizers to apply to the bean definition
		 * @return the updated runner configuration with the added bean
		 */
		private <T> RunnerConfiguration<C> withBean(String name, Class<T> type, Supplier<T> supplier,
				BeanDefinitionCustomizer... customizers) {
			RunnerConfiguration<C> config = new RunnerConfiguration<>(this);
			config.beanRegistrations = add(config.beanRegistrations,
					new BeanRegistration<>(name, type, supplier, customizers));
			return config;
		}

		/**
		 * Sets the configurations for the runner.
		 * @param configurations the configurations to be set (must not be null)
		 * @return a new RunnerConfiguration object with the updated configurations
		 * @throws IllegalArgumentException if the configurations parameter is null
		 */
		private RunnerConfiguration<C> withConfiguration(Configurations configurations) {
			Assert.notNull(configurations, "Configurations must not be null");
			RunnerConfiguration<C> config = new RunnerConfiguration<>(this);
			config.configurations = add(config.configurations, configurations);
			return config;
		}

		/**
		 * Adds an element to the given list and returns a new list with the added
		 * element.
		 * @param <T> the type of elements in the list
		 * @param list the list to add the element to
		 * @param element the element to be added to the list
		 * @return a new list with the added element
		 */
		private static <T> List<T> add(List<T> list, T element) {
			List<T> result = new ArrayList<>(list);
			result.add(element);
			return result;
		}

	}

}
