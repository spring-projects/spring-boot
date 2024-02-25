/*
 * Copyright 2012-2024 the original author or authors.
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

import java.lang.StackWalker.StackFrame;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.crac.management.CRaCMXBean;

import org.springframework.aot.AotDetector;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.BindableRuntimeHintsRegistrar;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.aot.AotApplicationContextInitializer;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.OrderComparator;
import org.springframework.core.OrderComparator.OrderSourceProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.io.support.SpringFactoriesLoader.ArgumentResolver;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.function.ThrowingConsumer;
import org.springframework.util.function.ThrowingSupplier;

/**
 * Class that can be used to bootstrap and launch a Spring application from a Java main
 * method. By default class will perform the following steps to bootstrap your
 * application:
 *
 * <ul>
 * <li>Create an appropriate {@link ApplicationContext} instance (depending on your
 * classpath)</li>
 * <li>Register a {@link CommandLinePropertySource} to expose command line arguments as
 * Spring properties</li>
 * <li>Refresh the application context, loading all singleton beans</li>
 * <li>Trigger any {@link CommandLineRunner} beans</li>
 * </ul>
 *
 * In most circumstances the static {@link #run(Class, String[])} method can be called
 * directly from your {@literal main} method to bootstrap your application:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableAutoConfiguration
 * public class MyApplication  {
 *
 *   // ... Bean definitions
 *
 *   public static void main(String[] args) {
 *     SpringApplication.run(MyApplication.class, args);
 *   }
 * }
 * </pre>
 *
 * <p>
 * For more advanced configuration a {@link SpringApplication} instance can be created and
 * customized before being run:
 *
 * <pre class="code">
 * public static void main(String[] args) {
 *   SpringApplication application = new SpringApplication(MyApplication.class);
 *   // ... customize application settings here
 *   application.run(args)
 * }
 * </pre>
 *
 * {@link SpringApplication}s can read beans from a variety of different sources. It is
 * generally recommended that a single {@code @Configuration} class is used to bootstrap
 * your application, however, you may also set {@link #getSources() sources} from:
 * <ul>
 * <li>The fully qualified class name to be loaded by
 * {@link AnnotatedBeanDefinitionReader}</li>
 * <li>The location of an XML resource to be loaded by {@link XmlBeanDefinitionReader}, or
 * a groovy script to be loaded by {@link GroovyBeanDefinitionReader}</li>
 * <li>The name of a package to be scanned by {@link ClassPathBeanDefinitionScanner}</li>
 * </ul>
 *
 * Configuration properties are also bound to the {@link SpringApplication}. This makes it
 * possible to set {@link SpringApplication} properties dynamically, like additional
 * sources ("spring.main.sources" - a CSV list) the flag to indicate a web environment
 * ("spring.main.web-application-type=none") or the flag to switch off the banner
 * ("spring.main.banner-mode=off").
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Christian Dupuis
 * @author Stephane Nicoll
 * @author Jeremy Rickard
 * @author Craig Burke
 * @author Michael Simons
 * @author Madhura Bhave
 * @author Brian Clozel
 * @author Ethan Rubinson
 * @author Chris Bono
 * @author Moritz Halbritter
 * @author Tadaya Tsuyukubo
 * @author Lasse Wulff
 * @author Yanming Zhou
 * @since 1.0.0
 * @see #run(Class, String[])
 * @see #run(Class[], String[])
 * @see #SpringApplication(Class...)
 */
public class SpringApplication {

	/**
	 * Default banner location.
	 */
	public static final String BANNER_LOCATION_PROPERTY_VALUE = SpringApplicationBannerPrinter.DEFAULT_BANNER_LOCATION;

	/**
	 * Banner location property key.
	 */
	public static final String BANNER_LOCATION_PROPERTY = SpringApplicationBannerPrinter.BANNER_LOCATION_PROPERTY;

	private static final String SYSTEM_PROPERTY_JAVA_AWT_HEADLESS = "java.awt.headless";

	private static final Log logger = LogFactory.getLog(SpringApplication.class);

	static final SpringApplicationShutdownHook shutdownHook = new SpringApplicationShutdownHook();

	private static final ThreadLocal<SpringApplicationHook> applicationHook = new ThreadLocal<>();

	private final Set<Class<?>> primarySources;

	private Set<String> sources = new LinkedHashSet<>();

	private Class<?> mainApplicationClass;

	private Banner.Mode bannerMode = Banner.Mode.CONSOLE;

	private boolean logStartupInfo = true;

	private boolean addCommandLineProperties = true;

	private boolean addConversionService = true;

	private Banner banner;

	private ResourceLoader resourceLoader;

	private BeanNameGenerator beanNameGenerator;

	private ConfigurableEnvironment environment;

	private WebApplicationType webApplicationType;

	private boolean headless = true;

	private boolean registerShutdownHook = true;

	private List<ApplicationContextInitializer<?>> initializers;

	private List<ApplicationListener<?>> listeners;

	private Map<String, Object> defaultProperties;

	private final List<BootstrapRegistryInitializer> bootstrapRegistryInitializers;

	private Set<String> additionalProfiles = Collections.emptySet();

	private boolean allowBeanDefinitionOverriding;

	private boolean allowCircularReferences;

	private boolean isCustomEnvironment = false;

	private boolean lazyInitialization = false;

	private String environmentPrefix;

	private ApplicationContextFactory applicationContextFactory = ApplicationContextFactory.DEFAULT;

	private ApplicationStartup applicationStartup = ApplicationStartup.DEFAULT;

	private boolean keepAlive;

	/**
	 * Create a new {@link SpringApplication} instance. The application context will load
	 * beans from the specified primary sources (see {@link SpringApplication class-level}
	 * documentation for details). The instance can be customized before calling
	 * {@link #run(String...)}.
	 * @param primarySources the primary bean sources
	 * @see #run(Class, String[])
	 * @see #SpringApplication(ResourceLoader, Class...)
	 * @see #setSources(Set)
	 */
	public SpringApplication(Class<?>... primarySources) {
		this(null, primarySources);
	}

	/**
	 * Create a new {@link SpringApplication} instance. The application context will load
	 * beans from the specified primary sources (see {@link SpringApplication class-level}
	 * documentation for details). The instance can be customized before calling
	 * {@link #run(String...)}.
	 * @param resourceLoader the resource loader to use
	 * @param primarySources the primary bean sources
	 * @see #run(Class, String[])
	 * @see #setSources(Set)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public SpringApplication(ResourceLoader resourceLoader, Class<?>... primarySources) {
		this.resourceLoader = resourceLoader;
		Assert.notNull(primarySources, "PrimarySources must not be null");
		this.primarySources = new LinkedHashSet<>(Arrays.asList(primarySources));
		this.webApplicationType = WebApplicationType.deduceFromClasspath();
		this.bootstrapRegistryInitializers = new ArrayList<>(
				getSpringFactoriesInstances(BootstrapRegistryInitializer.class));
		setInitializers((Collection) getSpringFactoriesInstances(ApplicationContextInitializer.class));
		setListeners((Collection) getSpringFactoriesInstances(ApplicationListener.class));
		this.mainApplicationClass = deduceMainApplicationClass();
	}

	/**
	 * Deduces the main application class.
	 * @return The main application class, or null if not found.
	 */
	private Class<?> deduceMainApplicationClass() {
		return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
			.walk(this::findMainClass)
			.orElse(null);
	}

	/**
	 * Finds the main class from the given stack of stack frames.
	 * @param stack the stream of stack frames to search
	 * @return an Optional containing the main class if found, otherwise empty
	 */
	private Optional<Class<?>> findMainClass(Stream<StackFrame> stack) {
		return stack.filter((frame) -> Objects.equals(frame.getMethodName(), "main"))
			.findFirst()
			.map(StackWalker.StackFrame::getDeclaringClass);
	}

	/**
	 * Run the Spring application, creating and refreshing a new
	 * {@link ApplicationContext}.
	 * @param args the application arguments (usually passed from a Java main method)
	 * @return a running {@link ApplicationContext}
	 */
	public ConfigurableApplicationContext run(String... args) {
		Startup startup = Startup.create();
		if (this.registerShutdownHook) {
			SpringApplication.shutdownHook.enableShutdownHookAddition();
		}
		DefaultBootstrapContext bootstrapContext = createBootstrapContext();
		ConfigurableApplicationContext context = null;
		configureHeadlessProperty();
		SpringApplicationRunListeners listeners = getRunListeners(args);
		listeners.starting(bootstrapContext, this.mainApplicationClass);
		try {
			ApplicationArguments applicationArguments = new DefaultApplicationArguments(args);
			ConfigurableEnvironment environment = prepareEnvironment(listeners, bootstrapContext, applicationArguments);
			Banner printedBanner = printBanner(environment);
			context = createApplicationContext();
			context.setApplicationStartup(this.applicationStartup);
			prepareContext(bootstrapContext, context, environment, listeners, applicationArguments, printedBanner);
			refreshContext(context);
			afterRefresh(context, applicationArguments);
			startup.started();
			if (this.logStartupInfo) {
				new StartupInfoLogger(this.mainApplicationClass).logStarted(getApplicationLog(), startup);
			}
			listeners.started(context, startup.timeTakenToStarted());
			callRunners(context, applicationArguments);
		}
		catch (Throwable ex) {
			throw handleRunFailure(context, ex, listeners);
		}
		try {
			if (context.isRunning()) {
				listeners.ready(context, startup.ready());
			}
		}
		catch (Throwable ex) {
			throw handleRunFailure(context, ex, null);
		}
		return context;
	}

	/**
	 * Creates a new {@link DefaultBootstrapContext} and initializes it with the
	 * registered {@link BootstrapRegistryInitializer}s.
	 * @return the created {@link DefaultBootstrapContext}
	 */
	private DefaultBootstrapContext createBootstrapContext() {
		DefaultBootstrapContext bootstrapContext = new DefaultBootstrapContext();
		this.bootstrapRegistryInitializers.forEach((initializer) -> initializer.initialize(bootstrapContext));
		return bootstrapContext;
	}

	/**
	 * Prepares the environment for the SpringApplication.
	 * @param listeners the SpringApplicationRunListeners to be used
	 * @param bootstrapContext the DefaultBootstrapContext to be used
	 * @param applicationArguments the ApplicationArguments to be used
	 * @return the prepared ConfigurableEnvironment
	 */
	private ConfigurableEnvironment prepareEnvironment(SpringApplicationRunListeners listeners,
			DefaultBootstrapContext bootstrapContext, ApplicationArguments applicationArguments) {
		// Create and configure the environment
		ConfigurableEnvironment environment = getOrCreateEnvironment();
		configureEnvironment(environment, applicationArguments.getSourceArgs());
		ConfigurationPropertySources.attach(environment);
		listeners.environmentPrepared(bootstrapContext, environment);
		DefaultPropertiesPropertySource.moveToEnd(environment);
		Assert.state(!environment.containsProperty("spring.main.environment-prefix"),
				"Environment prefix cannot be set via properties.");
		bindToSpringApplication(environment);
		if (!this.isCustomEnvironment) {
			EnvironmentConverter environmentConverter = new EnvironmentConverter(getClassLoader());
			environment = environmentConverter.convertEnvironmentIfNecessary(environment, deduceEnvironmentClass());
		}
		ConfigurationPropertySources.attach(environment);
		return environment;
	}

	/**
	 * Deduces the class of the environment based on the web application type and the
	 * configured ApplicationContextFactory. If a specific environment class is not found,
	 * the default ApplicationEnvironment class is used.
	 * @return The class of the environment to be used.
	 */
	private Class<? extends ConfigurableEnvironment> deduceEnvironmentClass() {
		Class<? extends ConfigurableEnvironment> environmentType = this.applicationContextFactory
			.getEnvironmentType(this.webApplicationType);
		if (environmentType == null && this.applicationContextFactory != ApplicationContextFactory.DEFAULT) {
			environmentType = ApplicationContextFactory.DEFAULT.getEnvironmentType(this.webApplicationType);
		}
		if (environmentType == null) {
			return ApplicationEnvironment.class;
		}
		return environmentType;
	}

	/**
	 * Prepares the application context before it is fully initialized.
	 * @param bootstrapContext the bootstrap context
	 * @param context the configurable application context
	 * @param environment the configurable environment
	 * @param listeners the application run listeners
	 * @param applicationArguments the application arguments
	 * @param printedBanner the printed banner
	 */
	private void prepareContext(DefaultBootstrapContext bootstrapContext, ConfigurableApplicationContext context,
			ConfigurableEnvironment environment, SpringApplicationRunListeners listeners,
			ApplicationArguments applicationArguments, Banner printedBanner) {
		context.setEnvironment(environment);
		postProcessApplicationContext(context);
		addAotGeneratedInitializerIfNecessary(this.initializers);
		applyInitializers(context);
		listeners.contextPrepared(context);
		bootstrapContext.close(context);
		if (this.logStartupInfo) {
			logStartupInfo(context.getParent() == null);
			logStartupProfileInfo(context);
		}
		// Add boot specific singleton beans
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		beanFactory.registerSingleton("springApplicationArguments", applicationArguments);
		if (printedBanner != null) {
			beanFactory.registerSingleton("springBootBanner", printedBanner);
		}
		if (beanFactory instanceof AbstractAutowireCapableBeanFactory autowireCapableBeanFactory) {
			autowireCapableBeanFactory.setAllowCircularReferences(this.allowCircularReferences);
			if (beanFactory instanceof DefaultListableBeanFactory listableBeanFactory) {
				listableBeanFactory.setAllowBeanDefinitionOverriding(this.allowBeanDefinitionOverriding);
			}
		}
		if (this.lazyInitialization) {
			context.addBeanFactoryPostProcessor(new LazyInitializationBeanFactoryPostProcessor());
		}
		if (this.keepAlive) {
			context.addApplicationListener(new KeepAlive());
		}
		context.addBeanFactoryPostProcessor(new PropertySourceOrderingBeanFactoryPostProcessor(context));
		if (!AotDetector.useGeneratedArtifacts()) {
			// Load the sources
			Set<Object> sources = getAllSources();
			Assert.notEmpty(sources, "Sources must not be empty");
			load(context, sources.toArray(new Object[0]));
		}
		listeners.contextLoaded(context);
	}

	/**
	 * Adds an AOT-generated initializer to the list of application context initializers
	 * if necessary.
	 * @param initializers the list of application context initializers
	 */
	private void addAotGeneratedInitializerIfNecessary(List<ApplicationContextInitializer<?>> initializers) {
		if (AotDetector.useGeneratedArtifacts()) {
			List<ApplicationContextInitializer<?>> aotInitializers = new ArrayList<>(
					initializers.stream().filter(AotApplicationContextInitializer.class::isInstance).toList());
			if (aotInitializers.isEmpty()) {
				String initializerClassName = this.mainApplicationClass.getName() + "__ApplicationContextInitializer";
				Assert.state(ClassUtils.isPresent(initializerClassName, getClassLoader()),
						"You are starting the application with AOT mode enabled but AOT processing hasn't happened. "
								+ "Please build your application with enabled AOT processing first, "
								+ "or remove the system property 'spring.aot.enabled' to run the application in regular mode");
				aotInitializers.add(AotApplicationContextInitializer.forInitializerClasses(initializerClassName));
			}
			initializers.removeAll(aotInitializers);
			initializers.addAll(0, aotInitializers);
		}
	}

	/**
	 * Refreshes the given application context.
	 * @param context the configurable application context to refresh
	 */
	private void refreshContext(ConfigurableApplicationContext context) {
		if (this.registerShutdownHook) {
			shutdownHook.registerApplicationContext(context);
		}
		refresh(context);
	}

	/**
	 * Configures the headless property for the application.
	 *
	 * This method sets the system property "java.awt.headless" to the value specified by
	 * the "headless" property of the SpringApplication object. If the "java.awt.headless"
	 * system property is already set, it will not be overridden.
	 *
	 * @see System#setProperty(String, String)
	 * @see System#getProperty(String, String)
	 *
	 * @since 1.0.0
	 */
	private void configureHeadlessProperty() {
		System.setProperty(SYSTEM_PROPERTY_JAVA_AWT_HEADLESS,
				System.getProperty(SYSTEM_PROPERTY_JAVA_AWT_HEADLESS, Boolean.toString(this.headless)));
	}

	/**
	 * Retrieves the run listeners for the SpringApplication.
	 * @param args the command line arguments passed to the SpringApplication
	 * @return the SpringApplicationRunListeners containing the run listeners
	 */
	private SpringApplicationRunListeners getRunListeners(String[] args) {
		ArgumentResolver argumentResolver = ArgumentResolver.of(SpringApplication.class, this);
		argumentResolver = argumentResolver.and(String[].class, args);
		List<SpringApplicationRunListener> listeners = getSpringFactoriesInstances(SpringApplicationRunListener.class,
				argumentResolver);
		SpringApplicationHook hook = applicationHook.get();
		SpringApplicationRunListener hookListener = (hook != null) ? hook.getRunListener(this) : null;
		if (hookListener != null) {
			listeners = new ArrayList<>(listeners);
			listeners.add(hookListener);
		}
		return new SpringApplicationRunListeners(logger, listeners, this.applicationStartup);
	}

	/**
	 * Retrieve all instances of the specified type from the Spring Factories.
	 * @param <T> the type of instances to retrieve
	 * @param type the class object representing the type of instances to retrieve
	 * @return a list of instances of the specified type
	 */
	private <T> List<T> getSpringFactoriesInstances(Class<T> type) {
		return getSpringFactoriesInstances(type, null);
	}

	/**
	 * Retrieves instances of the specified type from the Spring factories.
	 * @param <T> the type of instances to retrieve
	 * @param type the class object representing the type of instances to retrieve
	 * @param argumentResolver the argument resolver to use for resolving constructor
	 * arguments
	 * @return a list of instances of the specified type retrieved from the Spring
	 * factories
	 */
	private <T> List<T> getSpringFactoriesInstances(Class<T> type, ArgumentResolver argumentResolver) {
		return SpringFactoriesLoader.forDefaultResourceLocation(getClassLoader()).load(type, argumentResolver);
	}

	/**
	 * Returns the existing environment if it is already created, otherwise creates a new
	 * environment.
	 * @return the existing or newly created ConfigurableEnvironment
	 */
	private ConfigurableEnvironment getOrCreateEnvironment() {
		if (this.environment != null) {
			return this.environment;
		}
		ConfigurableEnvironment environment = this.applicationContextFactory.createEnvironment(this.webApplicationType);
		if (environment == null && this.applicationContextFactory != ApplicationContextFactory.DEFAULT) {
			environment = ApplicationContextFactory.DEFAULT.createEnvironment(this.webApplicationType);
		}
		return (environment != null) ? environment : new ApplicationEnvironment();
	}

	/**
	 * Template method delegating to
	 * {@link #configurePropertySources(ConfigurableEnvironment, String[])} and
	 * {@link #configureProfiles(ConfigurableEnvironment, String[])} in that order.
	 * Override this method for complete control over Environment customization, or one of
	 * the above for fine-grained control over property sources or profiles, respectively.
	 * @param environment this application's environment
	 * @param args arguments passed to the {@code run} method
	 * @see #configureProfiles(ConfigurableEnvironment, String[])
	 * @see #configurePropertySources(ConfigurableEnvironment, String[])
	 */
	protected void configureEnvironment(ConfigurableEnvironment environment, String[] args) {
		if (this.addConversionService) {
			environment.setConversionService(new ApplicationConversionService());
		}
		configurePropertySources(environment, args);
		configureProfiles(environment, args);
	}

	/**
	 * Add, remove or re-order any {@link PropertySource}s in this application's
	 * environment.
	 * @param environment this application's environment
	 * @param args arguments passed to the {@code run} method
	 * @see #configureEnvironment(ConfigurableEnvironment, String[])
	 */
	protected void configurePropertySources(ConfigurableEnvironment environment, String[] args) {
		MutablePropertySources sources = environment.getPropertySources();
		if (!CollectionUtils.isEmpty(this.defaultProperties)) {
			DefaultPropertiesPropertySource.addOrMerge(this.defaultProperties, sources);
		}
		if (this.addCommandLineProperties && args.length > 0) {
			String name = CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME;
			if (sources.contains(name)) {
				PropertySource<?> source = sources.get(name);
				CompositePropertySource composite = new CompositePropertySource(name);
				composite
					.addPropertySource(new SimpleCommandLinePropertySource("springApplicationCommandLineArgs", args));
				composite.addPropertySource(source);
				sources.replace(name, composite);
			}
			else {
				sources.addFirst(new SimpleCommandLinePropertySource(args));
			}
		}
	}

	/**
	 * Configure which profiles are active (or active by default) for this application
	 * environment. Additional profiles may be activated during configuration file
	 * processing through the {@code spring.profiles.active} property.
	 * @param environment this application's environment
	 * @param args arguments passed to the {@code run} method
	 * @see #configureEnvironment(ConfigurableEnvironment, String[])
	 */
	protected void configureProfiles(ConfigurableEnvironment environment, String[] args) {
	}

	/**
	 * Bind the environment to the {@link SpringApplication}.
	 * @param environment the environment to bind
	 */
	protected void bindToSpringApplication(ConfigurableEnvironment environment) {
		try {
			Binder.get(environment).bind("spring.main", Bindable.ofInstance(this));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Cannot bind to SpringApplication", ex);
		}
	}

	/**
	 * Prints the banner based on the specified environment.
	 * @param environment the configurable environment
	 * @return the printed banner
	 */
	private Banner printBanner(ConfigurableEnvironment environment) {
		if (this.bannerMode == Banner.Mode.OFF) {
			return null;
		}
		ResourceLoader resourceLoader = (this.resourceLoader != null) ? this.resourceLoader
				: new DefaultResourceLoader(null);
		SpringApplicationBannerPrinter bannerPrinter = new SpringApplicationBannerPrinter(resourceLoader, this.banner);
		if (this.bannerMode == Mode.LOG) {
			return bannerPrinter.print(environment, this.mainApplicationClass, logger);
		}
		return bannerPrinter.print(environment, this.mainApplicationClass, System.out);
	}

	/**
	 * Strategy method used to create the {@link ApplicationContext}. By default this
	 * method will respect any explicitly set application context class or factory before
	 * falling back to a suitable default.
	 * @return the application context (not yet refreshed)
	 * @see #setApplicationContextFactory(ApplicationContextFactory)
	 */
	protected ConfigurableApplicationContext createApplicationContext() {
		return this.applicationContextFactory.create(this.webApplicationType);
	}

	/**
	 * Apply any relevant post-processing to the {@link ApplicationContext}. Subclasses
	 * can apply additional processing as required.
	 * @param context the application context
	 */
	protected void postProcessApplicationContext(ConfigurableApplicationContext context) {
		if (this.beanNameGenerator != null) {
			context.getBeanFactory()
				.registerSingleton(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR, this.beanNameGenerator);
		}
		if (this.resourceLoader != null) {
			if (context instanceof GenericApplicationContext genericApplicationContext) {
				genericApplicationContext.setResourceLoader(this.resourceLoader);
			}
			if (context instanceof DefaultResourceLoader defaultResourceLoader) {
				defaultResourceLoader.setClassLoader(this.resourceLoader.getClassLoader());
			}
		}
		if (this.addConversionService) {
			context.getBeanFactory().setConversionService(context.getEnvironment().getConversionService());
		}
	}

	/**
	 * Apply any {@link ApplicationContextInitializer}s to the context before it is
	 * refreshed.
	 * @param context the configured ApplicationContext (not refreshed yet)
	 * @see ConfigurableApplicationContext#refresh()
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void applyInitializers(ConfigurableApplicationContext context) {
		for (ApplicationContextInitializer initializer : getInitializers()) {
			Class<?> requiredType = GenericTypeResolver.resolveTypeArgument(initializer.getClass(),
					ApplicationContextInitializer.class);
			Assert.isInstanceOf(requiredType, context, "Unable to call initializer.");
			initializer.initialize(context);
		}
	}

	/**
	 * Called to log startup information, subclasses may override to add additional
	 * logging.
	 * @param isRoot true if this application is the root of a context hierarchy
	 */
	protected void logStartupInfo(boolean isRoot) {
		if (isRoot) {
			new StartupInfoLogger(this.mainApplicationClass).logStarting(getApplicationLog());
		}
	}

	/**
	 * Called to log active profile information.
	 * @param context the application context
	 */
	protected void logStartupProfileInfo(ConfigurableApplicationContext context) {
		Log log = getApplicationLog();
		if (log.isInfoEnabled()) {
			List<String> activeProfiles = quoteProfiles(context.getEnvironment().getActiveProfiles());
			if (ObjectUtils.isEmpty(activeProfiles)) {
				List<String> defaultProfiles = quoteProfiles(context.getEnvironment().getDefaultProfiles());
				String message = String.format("%s default %s: ", defaultProfiles.size(),
						(defaultProfiles.size() <= 1) ? "profile" : "profiles");
				log.info("No active profile set, falling back to " + message
						+ StringUtils.collectionToDelimitedString(defaultProfiles, ", "));
			}
			else {
				String message = (activeProfiles.size() == 1) ? "1 profile is active: "
						: activeProfiles.size() + " profiles are active: ";
				log.info("The following " + message + StringUtils.collectionToDelimitedString(activeProfiles, ", "));
			}
		}
	}

	/**
	 * Quotes the given array of profiles by adding double quotes around each profile.
	 * @param profiles the array of profiles to be quoted
	 * @return a list of quoted profiles
	 */
	private List<String> quoteProfiles(String[] profiles) {
		return Arrays.stream(profiles).map((profile) -> "\"" + profile + "\"").toList();
	}

	/**
	 * Returns the {@link Log} for the application. By default will be deduced.
	 * @return the application log
	 */
	protected Log getApplicationLog() {
		if (this.mainApplicationClass == null) {
			return logger;
		}
		return LogFactory.getLog(this.mainApplicationClass);
	}

	/**
	 * Load beans into the application context.
	 * @param context the context to load beans into
	 * @param sources the sources to load
	 */
	protected void load(ApplicationContext context, Object[] sources) {
		if (logger.isDebugEnabled()) {
			logger.debug("Loading source " + StringUtils.arrayToCommaDelimitedString(sources));
		}
		BeanDefinitionLoader loader = createBeanDefinitionLoader(getBeanDefinitionRegistry(context), sources);
		if (this.beanNameGenerator != null) {
			loader.setBeanNameGenerator(this.beanNameGenerator);
		}
		if (this.resourceLoader != null) {
			loader.setResourceLoader(this.resourceLoader);
		}
		if (this.environment != null) {
			loader.setEnvironment(this.environment);
		}
		loader.load();
	}

	/**
	 * The ResourceLoader that will be used in the ApplicationContext.
	 * @return the resourceLoader the resource loader that will be used in the
	 * ApplicationContext (or null if the default)
	 */
	public ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}

	/**
	 * Either the ClassLoader that will be used in the ApplicationContext (if
	 * {@link #setResourceLoader(ResourceLoader) resourceLoader} is set), or the context
	 * class loader (if not null), or the loader of the Spring {@link ClassUtils} class.
	 * @return a ClassLoader (never null)
	 */
	public ClassLoader getClassLoader() {
		if (this.resourceLoader != null) {
			return this.resourceLoader.getClassLoader();
		}
		return ClassUtils.getDefaultClassLoader();
	}

	/**
	 * Get the bean definition registry.
	 * @param context the application context
	 * @return the BeanDefinitionRegistry if it can be determined
	 */
	private BeanDefinitionRegistry getBeanDefinitionRegistry(ApplicationContext context) {
		if (context instanceof BeanDefinitionRegistry registry) {
			return registry;
		}
		if (context instanceof AbstractApplicationContext abstractApplicationContext) {
			return (BeanDefinitionRegistry) abstractApplicationContext.getBeanFactory();
		}
		throw new IllegalStateException("Could not locate BeanDefinitionRegistry");
	}

	/**
	 * Factory method used to create the {@link BeanDefinitionLoader}.
	 * @param registry the bean definition registry
	 * @param sources the sources to load
	 * @return the {@link BeanDefinitionLoader} that will be used to load beans
	 */
	protected BeanDefinitionLoader createBeanDefinitionLoader(BeanDefinitionRegistry registry, Object[] sources) {
		return new BeanDefinitionLoader(registry, sources);
	}

	/**
	 * Refresh the underlying {@link ApplicationContext}.
	 * @param applicationContext the application context to refresh
	 */
	protected void refresh(ConfigurableApplicationContext applicationContext) {
		applicationContext.refresh();
	}

	/**
	 * Called after the context has been refreshed.
	 * @param context the application context
	 * @param args the application arguments
	 */
	protected void afterRefresh(ConfigurableApplicationContext context, ApplicationArguments args) {
	}

	/**
	 * Calls the runners in the specified application context in the order defined by
	 * their order values.
	 * @param context the application context
	 * @param args the application arguments
	 */
	private void callRunners(ConfigurableApplicationContext context, ApplicationArguments args) {
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		String[] beanNames = beanFactory.getBeanNamesForType(Runner.class);
		Map<Runner, String> instancesToBeanNames = new IdentityHashMap<>();
		for (String beanName : beanNames) {
			instancesToBeanNames.put(beanFactory.getBean(beanName, Runner.class), beanName);
		}
		Comparator<Object> comparator = getOrderComparator(beanFactory)
			.withSourceProvider(new FactoryAwareOrderSourceProvider(beanFactory, instancesToBeanNames));
		instancesToBeanNames.keySet().stream().sorted(comparator).forEach((runner) -> callRunner(runner, args));
	}

	/**
	 * Returns the OrderComparator for the given ConfigurableListableBeanFactory.
	 * @param beanFactory the ConfigurableListableBeanFactory to get the OrderComparator
	 * for
	 * @return the OrderComparator for the given beanFactory
	 */
	private OrderComparator getOrderComparator(ConfigurableListableBeanFactory beanFactory) {
		Comparator<?> dependencyComparator = (beanFactory instanceof DefaultListableBeanFactory defaultListableBeanFactory)
				? defaultListableBeanFactory.getDependencyComparator() : null;
		return (dependencyComparator instanceof OrderComparator orderComparator) ? orderComparator
				: AnnotationAwareOrderComparator.INSTANCE;
	}

	/**
	 * Calls the specified runner with the given application arguments.
	 * @param runner the runner to be called
	 * @param args the application arguments
	 */
	private void callRunner(Runner runner, ApplicationArguments args) {
		if (runner instanceof ApplicationRunner) {
			callRunner(ApplicationRunner.class, runner, (applicationRunner) -> applicationRunner.run(args));
		}
		if (runner instanceof CommandLineRunner) {
			callRunner(CommandLineRunner.class, runner,
					(commandLineRunner) -> commandLineRunner.run(args.getSourceArgs()));
		}
	}

	/**
	 * Calls the specified runner with the given type and runner object.
	 * @param <R> the type of the runner
	 * @param type the class of the runner
	 * @param runner the runner object
	 * @param call the consumer function to be called with the runner object
	 * @throws IllegalStateException if the execution of the runner fails
	 */
	@SuppressWarnings("unchecked")
	private <R extends Runner> void callRunner(Class<R> type, Runner runner, ThrowingConsumer<R> call) {
		call.throwing(
				(message, ex) -> new IllegalStateException("Failed to execute " + ClassUtils.getShortName(type), ex))
			.accept((R) runner);
	}

	/**
	 * Handles the failure of running the application.
	 * @param context the application context
	 * @param exception the exception that caused the failure
	 * @param listeners the run listeners
	 * @return a RuntimeException if the exception is an AbandonedRunException, otherwise
	 * an IllegalStateException
	 */
	private RuntimeException handleRunFailure(ConfigurableApplicationContext context, Throwable exception,
			SpringApplicationRunListeners listeners) {
		if (exception instanceof AbandonedRunException abandonedRunException) {
			return abandonedRunException;
		}
		try {
			try {
				handleExitCode(context, exception);
				if (listeners != null) {
					listeners.failed(context, exception);
				}
			}
			finally {
				reportFailure(getExceptionReporters(context), exception);
				if (context != null) {
					context.close();
					shutdownHook.deregisterFailedApplicationContext(context);
				}
			}
		}
		catch (Exception ex) {
			logger.warn("Unable to close ApplicationContext", ex);
		}
		return (exception instanceof RuntimeException runtimeException) ? runtimeException
				: new IllegalStateException(exception);
	}

	/**
	 * Retrieves a collection of SpringBootExceptionReporter instances.
	 * @param context the configurable application context
	 * @return a collection of SpringBootExceptionReporter instances
	 */
	private Collection<SpringBootExceptionReporter> getExceptionReporters(ConfigurableApplicationContext context) {
		try {
			ArgumentResolver argumentResolver = ArgumentResolver.of(ConfigurableApplicationContext.class, context);
			return getSpringFactoriesInstances(SpringBootExceptionReporter.class, argumentResolver);
		}
		catch (Throwable ex) {
			return Collections.emptyList();
		}
	}

	/**
	 * Reports a failure by iterating through a collection of SpringBootExceptionReporters
	 * and calling their reportException method. If a reporter successfully reports the
	 * exception, it is registered as a logged exception and the method returns. If no
	 * reporter is able to report the exception, the failure is logged using the logger
	 * and registered as a logged exception.
	 * @param exceptionReporters the collection of SpringBootExceptionReporters to iterate
	 * through
	 * @param failure the Throwable object representing the failure
	 */
	private void reportFailure(Collection<SpringBootExceptionReporter> exceptionReporters, Throwable failure) {
		try {
			for (SpringBootExceptionReporter reporter : exceptionReporters) {
				if (reporter.reportException(failure)) {
					registerLoggedException(failure);
					return;
				}
			}
		}
		catch (Throwable ex) {
			// Continue with normal handling of the original failure
		}
		if (logger.isErrorEnabled()) {
			logger.error("Application run failed", failure);
			registerLoggedException(failure);
		}
	}

	/**
	 * Register that the given exception has been logged. By default, if the running in
	 * the main thread, this method will suppress additional printing of the stacktrace.
	 * @param exception the exception that was logged
	 */
	protected void registerLoggedException(Throwable exception) {
		SpringBootExceptionHandler handler = getSpringBootExceptionHandler();
		if (handler != null) {
			handler.registerLoggedException(exception);
		}
	}

	/**
	 * Handles the exit code for the application.
	 * @param context the configurable application context
	 * @param exception the throwable exception
	 */
	private void handleExitCode(ConfigurableApplicationContext context, Throwable exception) {
		int exitCode = getExitCodeFromException(context, exception);
		if (exitCode != 0) {
			if (context != null) {
				context.publishEvent(new ExitCodeEvent(context, exitCode));
			}
			SpringBootExceptionHandler handler = getSpringBootExceptionHandler();
			if (handler != null) {
				handler.registerExitCode(exitCode);
			}
		}
	}

	/**
	 * Retrieves the exit code from the given exception.
	 * @param context the configurable application context
	 * @param exception the throwable exception
	 * @return the exit code
	 */
	private int getExitCodeFromException(ConfigurableApplicationContext context, Throwable exception) {
		int exitCode = getExitCodeFromMappedException(context, exception);
		if (exitCode == 0) {
			exitCode = getExitCodeFromExitCodeGeneratorException(exception);
		}
		return exitCode;
	}

	/**
	 * Retrieves the exit code from a mapped exception.
	 * @param context the application context
	 * @param exception the exception to map
	 * @return the exit code
	 */
	private int getExitCodeFromMappedException(ConfigurableApplicationContext context, Throwable exception) {
		if (context == null || !context.isActive()) {
			return 0;
		}
		ExitCodeGenerators generators = new ExitCodeGenerators();
		Collection<ExitCodeExceptionMapper> beans = context.getBeansOfType(ExitCodeExceptionMapper.class).values();
		generators.addAll(exception, beans);
		return generators.getExitCode();
	}

	/**
	 * Returns the exit code from the given ExitCodeGeneratorException.
	 * @param exception the Throwable object representing the ExitCodeGeneratorException
	 * @return the exit code obtained from the ExitCodeGenerator, or 0 if the exception is
	 * null
	 * @throws NullPointerException if the exception is null and does not have a cause
	 */
	private int getExitCodeFromExitCodeGeneratorException(Throwable exception) {
		if (exception == null) {
			return 0;
		}
		if (exception instanceof ExitCodeGenerator generator) {
			return generator.getExitCode();
		}
		return getExitCodeFromExitCodeGeneratorException(exception.getCause());
	}

	/**
	 * Returns the SpringBootExceptionHandler for the current thread if it is the main
	 * thread.
	 * @return the SpringBootExceptionHandler for the current thread if it is the main
	 * thread, otherwise null.
	 */
	SpringBootExceptionHandler getSpringBootExceptionHandler() {
		if (isMainThread(Thread.currentThread())) {
			return SpringBootExceptionHandler.forCurrentThread();
		}
		return null;
	}

	/**
	 * Checks if the given thread is the main thread.
	 * @param currentThread the thread to check
	 * @return true if the given thread is the main thread, false otherwise
	 */
	private boolean isMainThread(Thread currentThread) {
		return ("main".equals(currentThread.getName()) || "restartedMain".equals(currentThread.getName()))
				&& "main".equals(currentThread.getThreadGroup().getName());
	}

	/**
	 * Returns the main application class that has been deduced or explicitly configured.
	 * @return the main application class or {@code null}
	 */
	public Class<?> getMainApplicationClass() {
		return this.mainApplicationClass;
	}

	/**
	 * Set a specific main application class that will be used as a log source and to
	 * obtain version information. By default the main application class will be deduced.
	 * Can be set to {@code null} if there is no explicit application class.
	 * @param mainApplicationClass the mainApplicationClass to set or {@code null}
	 */
	public void setMainApplicationClass(Class<?> mainApplicationClass) {
		this.mainApplicationClass = mainApplicationClass;
	}

	/**
	 * Returns the type of web application that is being run.
	 * @return the type of web application
	 * @since 2.0.0
	 */
	public WebApplicationType getWebApplicationType() {
		return this.webApplicationType;
	}

	/**
	 * Sets the type of web application to be run. If not explicitly set the type of web
	 * application will be deduced based on the classpath.
	 * @param webApplicationType the web application type
	 * @since 2.0.0
	 */
	public void setWebApplicationType(WebApplicationType webApplicationType) {
		Assert.notNull(webApplicationType, "WebApplicationType must not be null");
		this.webApplicationType = webApplicationType;
	}

	/**
	 * Sets if bean definition overriding, by registering a definition with the same name
	 * as an existing definition, should be allowed. Defaults to {@code false}.
	 * @param allowBeanDefinitionOverriding if overriding is allowed
	 * @since 2.1.0
	 * @see DefaultListableBeanFactory#setAllowBeanDefinitionOverriding(boolean)
	 */
	public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
		this.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
	}

	/**
	 * Sets whether to allow circular references between beans and automatically try to
	 * resolve them. Defaults to {@code false}.
	 * @param allowCircularReferences if circular references are allowed
	 * @since 2.6.0
	 * @see AbstractAutowireCapableBeanFactory#setAllowCircularReferences(boolean)
	 */
	public void setAllowCircularReferences(boolean allowCircularReferences) {
		this.allowCircularReferences = allowCircularReferences;
	}

	/**
	 * Sets if beans should be initialized lazily. Defaults to {@code false}.
	 * @param lazyInitialization if initialization should be lazy
	 * @since 2.2
	 * @see BeanDefinition#setLazyInit(boolean)
	 */
	public void setLazyInitialization(boolean lazyInitialization) {
		this.lazyInitialization = lazyInitialization;
	}

	/**
	 * Sets if the application is headless and should not instantiate AWT. Defaults to
	 * {@code true} to prevent java icons appearing.
	 * @param headless if the application is headless
	 */
	public void setHeadless(boolean headless) {
		this.headless = headless;
	}

	/**
	 * Sets if the created {@link ApplicationContext} should have a shutdown hook
	 * registered. Defaults to {@code true} to ensure that JVM shutdowns are handled
	 * gracefully.
	 * @param registerShutdownHook if the shutdown hook should be registered
	 * @see #getShutdownHandlers()
	 */
	public void setRegisterShutdownHook(boolean registerShutdownHook) {
		this.registerShutdownHook = registerShutdownHook;
	}

	/**
	 * Sets the {@link Banner} instance which will be used to print the banner when no
	 * static banner file is provided.
	 * @param banner the Banner instance to use
	 */
	public void setBanner(Banner banner) {
		this.banner = banner;
	}

	/**
	 * Sets the mode used to display the banner when the application runs. Defaults to
	 * {@code Banner.Mode.CONSOLE}.
	 * @param bannerMode the mode used to display the banner
	 */
	public void setBannerMode(Banner.Mode bannerMode) {
		this.bannerMode = bannerMode;
	}

	/**
	 * Sets if the application information should be logged when the application starts.
	 * Defaults to {@code true}.
	 * @param logStartupInfo if startup info should be logged.
	 */
	public void setLogStartupInfo(boolean logStartupInfo) {
		this.logStartupInfo = logStartupInfo;
	}

	/**
	 * Sets if a {@link CommandLinePropertySource} should be added to the application
	 * context in order to expose arguments. Defaults to {@code true}.
	 * @param addCommandLineProperties if command line arguments should be exposed
	 */
	public void setAddCommandLineProperties(boolean addCommandLineProperties) {
		this.addCommandLineProperties = addCommandLineProperties;
	}

	/**
	 * Sets if the {@link ApplicationConversionService} should be added to the application
	 * context's {@link Environment}.
	 * @param addConversionService if the application conversion service should be added
	 * @since 2.1.0
	 */
	public void setAddConversionService(boolean addConversionService) {
		this.addConversionService = addConversionService;
	}

	/**
	 * Adds {@link BootstrapRegistryInitializer} instances that can be used to initialize
	 * the {@link BootstrapRegistry}.
	 * @param bootstrapRegistryInitializer the bootstrap registry initializer to add
	 * @since 2.4.5
	 */
	public void addBootstrapRegistryInitializer(BootstrapRegistryInitializer bootstrapRegistryInitializer) {
		Assert.notNull(bootstrapRegistryInitializer, "BootstrapRegistryInitializer must not be null");
		this.bootstrapRegistryInitializers.addAll(Arrays.asList(bootstrapRegistryInitializer));
	}

	/**
	 * Set default environment properties which will be used in addition to those in the
	 * existing {@link Environment}.
	 * @param defaultProperties the additional properties to set
	 */
	public void setDefaultProperties(Map<String, Object> defaultProperties) {
		this.defaultProperties = defaultProperties;
	}

	/**
	 * Convenient alternative to {@link #setDefaultProperties(Map)}.
	 * @param defaultProperties some {@link Properties}
	 */
	public void setDefaultProperties(Properties defaultProperties) {
		this.defaultProperties = new HashMap<>();
		for (Object key : Collections.list(defaultProperties.propertyNames())) {
			this.defaultProperties.put((String) key, defaultProperties.get(key));
		}
	}

	/**
	 * Set additional profile values to use (on top of those set in system or command line
	 * properties).
	 * @param profiles the additional profiles to set
	 */
	public void setAdditionalProfiles(String... profiles) {
		this.additionalProfiles = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(profiles)));
	}

	/**
	 * Return an immutable set of any additional profiles in use.
	 * @return the additional profiles
	 */
	public Set<String> getAdditionalProfiles() {
		return this.additionalProfiles;
	}

	/**
	 * Sets the bean name generator that should be used when generating bean names.
	 * @param beanNameGenerator the bean name generator
	 */
	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		this.beanNameGenerator = beanNameGenerator;
	}

	/**
	 * Sets the underlying environment that should be used with the created application
	 * context.
	 * @param environment the environment
	 */
	public void setEnvironment(ConfigurableEnvironment environment) {
		this.isCustomEnvironment = true;
		this.environment = environment;
	}

	/**
	 * Add additional items to the primary sources that will be added to an
	 * ApplicationContext when {@link #run(String...)} is called.
	 * <p>
	 * The sources here are added to those that were set in the constructor. Most users
	 * should consider using {@link #getSources()}/{@link #setSources(Set)} rather than
	 * calling this method.
	 * @param additionalPrimarySources the additional primary sources to add
	 * @see #SpringApplication(Class...)
	 * @see #getSources()
	 * @see #setSources(Set)
	 * @see #getAllSources()
	 */
	public void addPrimarySources(Collection<Class<?>> additionalPrimarySources) {
		this.primarySources.addAll(additionalPrimarySources);
	}

	/**
	 * Returns a mutable set of the sources that will be added to an ApplicationContext
	 * when {@link #run(String...)} is called.
	 * <p>
	 * Sources set here will be used in addition to any primary sources set in the
	 * constructor.
	 * @return the application sources.
	 * @see #SpringApplication(Class...)
	 * @see #getAllSources()
	 */
	public Set<String> getSources() {
		return this.sources;
	}

	/**
	 * Set additional sources that will be used to create an ApplicationContext. A source
	 * can be: a class name, package name, or an XML resource location.
	 * <p>
	 * Sources set here will be used in addition to any primary sources set in the
	 * constructor.
	 * @param sources the application sources to set
	 * @see #SpringApplication(Class...)
	 * @see #getAllSources()
	 */
	public void setSources(Set<String> sources) {
		Assert.notNull(sources, "Sources must not be null");
		this.sources = new LinkedHashSet<>(sources);
	}

	/**
	 * Return an immutable set of all the sources that will be added to an
	 * ApplicationContext when {@link #run(String...)} is called. This method combines any
	 * primary sources specified in the constructor with any additional ones that have
	 * been {@link #setSources(Set) explicitly set}.
	 * @return an immutable set of all sources
	 */
	public Set<Object> getAllSources() {
		Set<Object> allSources = new LinkedHashSet<>();
		if (!CollectionUtils.isEmpty(this.primarySources)) {
			allSources.addAll(this.primarySources);
		}
		if (!CollectionUtils.isEmpty(this.sources)) {
			allSources.addAll(this.sources);
		}
		return Collections.unmodifiableSet(allSources);
	}

	/**
	 * Sets the {@link ResourceLoader} that should be used when loading resources.
	 * @param resourceLoader the resource loader
	 */
	public void setResourceLoader(ResourceLoader resourceLoader) {
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		this.resourceLoader = resourceLoader;
	}

	/**
	 * Return a prefix that should be applied when obtaining configuration properties from
	 * the system environment.
	 * @return the environment property prefix
	 * @since 2.5.0
	 */
	public String getEnvironmentPrefix() {
		return this.environmentPrefix;
	}

	/**
	 * Set the prefix that should be applied when obtaining configuration properties from
	 * the system environment.
	 * @param environmentPrefix the environment property prefix to set
	 * @since 2.5.0
	 */
	public void setEnvironmentPrefix(String environmentPrefix) {
		this.environmentPrefix = environmentPrefix;
	}

	/**
	 * Sets the factory that will be called to create the application context. If not set,
	 * defaults to a factory that will create
	 * {@link AnnotationConfigServletWebServerApplicationContext} for servlet web
	 * applications, {@link AnnotationConfigReactiveWebServerApplicationContext} for
	 * reactive web applications, and {@link AnnotationConfigApplicationContext} for
	 * non-web applications.
	 * @param applicationContextFactory the factory for the context
	 * @since 2.4.0
	 */
	public void setApplicationContextFactory(ApplicationContextFactory applicationContextFactory) {
		this.applicationContextFactory = (applicationContextFactory != null) ? applicationContextFactory
				: ApplicationContextFactory.DEFAULT;
	}

	/**
	 * Sets the {@link ApplicationContextInitializer} that will be applied to the Spring
	 * {@link ApplicationContext}.
	 * @param initializers the initializers to set
	 */
	public void setInitializers(Collection<? extends ApplicationContextInitializer<?>> initializers) {
		this.initializers = new ArrayList<>(initializers);
	}

	/**
	 * Add {@link ApplicationContextInitializer}s to be applied to the Spring
	 * {@link ApplicationContext}.
	 * @param initializers the initializers to add
	 */
	public void addInitializers(ApplicationContextInitializer<?>... initializers) {
		this.initializers.addAll(Arrays.asList(initializers));
	}

	/**
	 * Returns read-only ordered Set of the {@link ApplicationContextInitializer}s that
	 * will be applied to the Spring {@link ApplicationContext}.
	 * @return the initializers
	 */
	public Set<ApplicationContextInitializer<?>> getInitializers() {
		return asUnmodifiableOrderedSet(this.initializers);
	}

	/**
	 * Sets the {@link ApplicationListener}s that will be applied to the SpringApplication
	 * and registered with the {@link ApplicationContext}.
	 * @param listeners the listeners to set
	 */
	public void setListeners(Collection<? extends ApplicationListener<?>> listeners) {
		this.listeners = new ArrayList<>(listeners);
	}

	/**
	 * Add {@link ApplicationListener}s to be applied to the SpringApplication and
	 * registered with the {@link ApplicationContext}.
	 * @param listeners the listeners to add
	 */
	public void addListeners(ApplicationListener<?>... listeners) {
		this.listeners.addAll(Arrays.asList(listeners));
	}

	/**
	 * Returns read-only ordered Set of the {@link ApplicationListener}s that will be
	 * applied to the SpringApplication and registered with the {@link ApplicationContext}
	 * .
	 * @return the listeners
	 */
	public Set<ApplicationListener<?>> getListeners() {
		return asUnmodifiableOrderedSet(this.listeners);
	}

	/**
	 * Set the {@link ApplicationStartup} to use for collecting startup metrics.
	 * @param applicationStartup the application startup to use
	 * @since 2.4.0
	 */
	public void setApplicationStartup(ApplicationStartup applicationStartup) {
		this.applicationStartup = (applicationStartup != null) ? applicationStartup : ApplicationStartup.DEFAULT;
	}

	/**
	 * Returns the {@link ApplicationStartup} used for collecting startup metrics.
	 * @return the application startup
	 * @since 2.4.0
	 */
	public ApplicationStartup getApplicationStartup() {
		return this.applicationStartup;
	}

	/**
	 * Whether to keep the application alive even if there are no more non-daemon threads.
	 * @return whether to keep the application alive even if there are no more non-daemon
	 * threads
	 * @since 3.2.0
	 */
	public boolean isKeepAlive() {
		return this.keepAlive;
	}

	/**
	 * Set whether to keep the application alive even if there are no more non-daemon
	 * threads.
	 * @param keepAlive whether to keep the application alive even if there are no more
	 * non-daemon threads
	 * @since 3.2.0
	 */
	public void setKeepAlive(boolean keepAlive) {
		this.keepAlive = keepAlive;
	}

	/**
	 * Return a {@link SpringApplicationShutdownHandlers} instance that can be used to add
	 * or remove handlers that perform actions before the JVM is shutdown.
	 * @return a {@link SpringApplicationShutdownHandlers} instance
	 * @since 2.5.1
	 */
	public static SpringApplicationShutdownHandlers getShutdownHandlers() {
		return shutdownHook.getHandlers();
	}

	/**
	 * Static helper that can be used to run a {@link SpringApplication} from the
	 * specified source using default settings.
	 * @param primarySource the primary source to load
	 * @param args the application arguments (usually passed from a Java main method)
	 * @return the running {@link ApplicationContext}
	 */
	public static ConfigurableApplicationContext run(Class<?> primarySource, String... args) {
		return run(new Class<?>[] { primarySource }, args);
	}

	/**
	 * Static helper that can be used to run a {@link SpringApplication} from the
	 * specified sources using default settings and user supplied arguments.
	 * @param primarySources the primary sources to load
	 * @param args the application arguments (usually passed from a Java main method)
	 * @return the running {@link ApplicationContext}
	 */
	public static ConfigurableApplicationContext run(Class<?>[] primarySources, String[] args) {
		return new SpringApplication(primarySources).run(args);
	}

	/**
	 * A basic main that can be used to launch an application. This method is useful when
	 * application sources are defined through a {@literal --spring.main.sources} command
	 * line argument.
	 * <p>
	 * Most developers will want to define their own main method and call the
	 * {@link #run(Class, String...) run} method instead.
	 * @param args command line arguments
	 * @throws Exception if the application cannot be started
	 * @see SpringApplication#run(Class[], String[])
	 * @see SpringApplication#run(Class, String...)
	 */
	public static void main(String[] args) throws Exception {
		SpringApplication.run(new Class<?>[0], args);
	}

	/**
	 * Static helper that can be used to exit a {@link SpringApplication} and obtain a
	 * code indicating success (0) or otherwise. Does not throw exceptions but should
	 * print stack traces of any encountered. Applies the specified
	 * {@link ExitCodeGenerator ExitCodeGenerators} in addition to any Spring beans that
	 * implement {@link ExitCodeGenerator}. When multiple generators are available, the
	 * first non-zero exit code is used. Generators are ordered based on their
	 * {@link Ordered} implementation and {@link Order @Order} annotation.
	 * @param context the context to close if possible
	 * @param exitCodeGenerators exit code generators
	 * @return the outcome (0 if successful)
	 */
	public static int exit(ApplicationContext context, ExitCodeGenerator... exitCodeGenerators) {
		Assert.notNull(context, "Context must not be null");
		int exitCode = 0;
		try {
			try {
				ExitCodeGenerators generators = new ExitCodeGenerators();
				Collection<ExitCodeGenerator> beans = context.getBeansOfType(ExitCodeGenerator.class).values();
				generators.addAll(exitCodeGenerators);
				generators.addAll(beans);
				exitCode = generators.getExitCode();
				if (exitCode != 0) {
					context.publishEvent(new ExitCodeEvent(context, exitCode));
				}
			}
			finally {
				close(context);
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
			exitCode = (exitCode != 0) ? exitCode : 1;
		}
		return exitCode;
	}

	/**
	 * Create an application from an existing {@code main} method that can run with
	 * additional {@code @Configuration} or bean classes. This method can be helpful when
	 * writing a test harness that needs to start an application with additional
	 * configuration.
	 * @param main the main method entry point that runs the {@link SpringApplication}
	 * @return a {@link SpringApplication.Augmented} instance that can be used to add
	 * configuration and run the application
	 * @since 3.1.0
	 * @see #withHook(SpringApplicationHook, Runnable)
	 */
	public static SpringApplication.Augmented from(ThrowingConsumer<String[]> main) {
		Assert.notNull(main, "Main must not be null");
		return new Augmented(main, Collections.emptySet());
	}

	/**
	 * Perform the given action with the given {@link SpringApplicationHook} attached if
	 * the action triggers an {@link SpringApplication#run(String...) application run}.
	 * @param hook the hook to apply
	 * @param action the action to run
	 * @since 3.0.0
	 * @see #withHook(SpringApplicationHook, ThrowingSupplier)
	 */
	public static void withHook(SpringApplicationHook hook, Runnable action) {
		withHook(hook, () -> {
			action.run();
			return null;
		});
	}

	/**
	 * Perform the given action with the given {@link SpringApplicationHook} attached if
	 * the action triggers an {@link SpringApplication#run(String...) application run}.
	 * @param <T> the result type
	 * @param hook the hook to apply
	 * @param action the action to run
	 * @return the result of the action
	 * @since 3.0.0
	 * @see #withHook(SpringApplicationHook, Runnable)
	 */
	public static <T> T withHook(SpringApplicationHook hook, ThrowingSupplier<T> action) {
		applicationHook.set(hook);
		try {
			return action.get();
		}
		finally {
			applicationHook.remove();
		}
	}

	/**
	 * Closes the given ApplicationContext.
	 * @param context the ApplicationContext to be closed
	 */
	private static void close(ApplicationContext context) {
		if (context instanceof ConfigurableApplicationContext closable) {
			closable.close();
		}
	}

	/**
	 * Returns an unmodifiable ordered set containing the elements of the specified
	 * collection. The elements are sorted based on their natural ordering or the ordering
	 * specified by the elements' annotations.
	 * @param elements the collection of elements to be added to the set
	 * @param <E> the type of elements in the set
	 * @return an unmodifiable ordered set containing the elements of the specified
	 * collection
	 */
	private static <E> Set<E> asUnmodifiableOrderedSet(Collection<E> elements) {
		List<E> list = new ArrayList<>(elements);
		list.sort(AnnotationAwareOrderComparator.INSTANCE);
		return new LinkedHashSet<>(list);
	}

	/**
	 * Used to configure and run an augmented {@link SpringApplication} where additional
	 * configuration should be applied.
	 *
	 * @since 3.1.0
	 */
	public static class Augmented {

		private final ThrowingConsumer<String[]> main;

		private final Set<Class<?>> sources;

		/**
		 * Executes the main method with the given throwing consumer and set of sources.
		 * @param main the throwing consumer representing the main method
		 * @param sources the set of classes representing the sources
		 */
		Augmented(ThrowingConsumer<String[]> main, Set<Class<?>> sources) {
			this.main = main;
			this.sources = Set.copyOf(sources);
		}

		/**
		 * Return a new {@link SpringApplication.Augmented} instance with additional
		 * sources that should be applied when the application runs.
		 * @param sources the sources that should be applied
		 * @return a new {@link SpringApplication.Augmented} instance
		 */
		public Augmented with(Class<?>... sources) {
			LinkedHashSet<Class<?>> merged = new LinkedHashSet<>(this.sources);
			merged.addAll(Arrays.asList(sources));
			return new Augmented(this.main, merged);
		}

		/**
		 * Run the application using the given args.
		 * @param args the main method args
		 * @return the running {@link ApplicationContext}
		 */
		public SpringApplication.Running run(String... args) {
			RunListener runListener = new RunListener();
			SpringApplicationHook hook = new SingleUseSpringApplicationHook((springApplication) -> {
				springApplication.addPrimarySources(this.sources);
				return runListener;
			});
			withHook(hook, () -> this.main.accept(args));
			return runListener;
		}

		/**
		 * {@link SpringApplicationRunListener} to capture {@link Running} application
		 * details.
		 */
		private static final class RunListener implements SpringApplicationRunListener, Running {

			private final List<ConfigurableApplicationContext> contexts = Collections
				.synchronizedList(new ArrayList<>());

			/**
			 * Called when the context is loaded. Adds the given context to the list of
			 * contexts.
			 * @param context the configurable application context
			 */
			@Override
			public void contextLoaded(ConfigurableApplicationContext context) {
				this.contexts.add(context);
			}

			/**
			 * Returns the root application context from the list of contexts.
			 * @return The root application context.
			 * @throws IllegalStateException if no root application context is located or
			 * if multiple root application contexts are located.
			 */
			@Override
			public ConfigurableApplicationContext getApplicationContext() {
				List<ConfigurableApplicationContext> rootContexts = this.contexts.stream()
					.filter((context) -> context.getParent() == null)
					.toList();
				Assert.state(!rootContexts.isEmpty(), "No root application context located");
				Assert.state(rootContexts.size() == 1, "No unique root application context located");
				return rootContexts.get(0);
			}

		}

	}

	/**
	 * Provides access to details of a {@link SpringApplication} run using
	 * {@link Augmented#run(String...)}.
	 *
	 * @since 3.1.0
	 */
	public interface Running {

		/**
		 * Return the root {@link ConfigurableApplicationContext} of the running
		 * application.
		 * @return the root application context
		 */
		ConfigurableApplicationContext getApplicationContext();

	}

	/**
	 * {@link BeanFactoryPostProcessor} to re-order our property sources below any
	 * {@code @PropertySource} items added by the {@link ConfigurationClassPostProcessor}.
	 */
	private static class PropertySourceOrderingBeanFactoryPostProcessor implements BeanFactoryPostProcessor, Ordered {

		private final ConfigurableApplicationContext context;

		/**
		 * Constructs a new PropertySourceOrderingBeanFactoryPostProcessor with the
		 * specified ConfigurableApplicationContext.
		 * @param context the ConfigurableApplicationContext to be used by the
		 * PropertySourceOrderingBeanFactoryPostProcessor
		 */
		PropertySourceOrderingBeanFactoryPostProcessor(ConfigurableApplicationContext context) {
			this.context = context;
		}

		/**
		 * Returns the order of this bean in the bean factory post-processor chain. The
		 * order is set to the highest precedence.
		 * @return the order of this bean
		 */
		@Override
		public int getOrder() {
			return Ordered.HIGHEST_PRECEDENCE;
		}

		/**
		 * Moves the DefaultPropertiesPropertySource to the end of the property source
		 * list in the given bean factory. This method is called during the
		 * post-processing phase of the bean factory initialization.
		 * @param beanFactory the ConfigurableListableBeanFactory to process
		 * @throws BeansException if an error occurs during the bean factory
		 * post-processing
		 */
		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
			DefaultPropertiesPropertySource.moveToEnd(this.context.getEnvironment());
		}

	}

	/**
	 * SpringApplicationRuntimeHints class.
	 */
	static class SpringApplicationRuntimeHints extends BindableRuntimeHintsRegistrar {

		/**
		 * Constructs a new SpringApplicationRuntimeHints object.
		 *
		 * This constructor calls the constructor of the super class SpringApplication,
		 * passing the class SpringApplication.class as an argument.
		 *
		 * @see SpringApplication
		 */
		SpringApplicationRuntimeHints() {
			super(SpringApplication.class);
		}

	}

	/**
	 * Exception that can be thrown to silently exit a running {@link SpringApplication}
	 * without handling run failures.
	 *
	 * @since 3.0.0
	 */
	public static class AbandonedRunException extends RuntimeException {

		private final ConfigurableApplicationContext applicationContext;

		/**
		 * Create a new {@link AbandonedRunException} instance.
		 */
		public AbandonedRunException() {
			this(null);
		}

		/**
		 * Create a new {@link AbandonedRunException} instance with the given application
		 * context.
		 * @param applicationContext the application context that was available when the
		 * run was abandoned
		 */
		public AbandonedRunException(ConfigurableApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}

		/**
		 * Return the application context that was available when the run was abandoned or
		 * {@code null} if no context was available.
		 * @return the application context
		 */
		public ConfigurableApplicationContext getApplicationContext() {
			return this.applicationContext;
		}

	}

	/**
	 * {@link SpringApplicationHook} decorator that ensures the hook is only used once.
	 */
	private static final class SingleUseSpringApplicationHook implements SpringApplicationHook {

		private final AtomicBoolean used = new AtomicBoolean();

		private final SpringApplicationHook delegate;

		/**
		 * Constructs a new SingleUseSpringApplicationHook with the specified delegate.
		 * @param delegate the delegate SpringApplicationHook to be used
		 */
		private SingleUseSpringApplicationHook(SpringApplicationHook delegate) {
			this.delegate = delegate;
		}

		/**
		 * Returns the run listener for the specified SpringApplication.
		 * @param springApplication the SpringApplication instance
		 * @return the run listener if it has not been used before, null otherwise
		 */
		@Override
		public SpringApplicationRunListener getRunListener(SpringApplication springApplication) {
			return this.used.compareAndSet(false, true) ? this.delegate.getRunListener(springApplication) : null;
		}

	}

	/**
	 * Starts a non-daemon thread to keep the JVM alive on {@link ContextRefreshedEvent}.
	 * Stops the thread on {@link ContextClosedEvent}.
	 */
	private static final class KeepAlive implements ApplicationListener<ApplicationContextEvent> {

		private final AtomicReference<Thread> thread = new AtomicReference<>();

		/**
		 * This method is an event listener that is triggered when an application context
		 * event occurs. It checks the type of the event and performs specific actions
		 * accordingly. If the event is a ContextRefreshedEvent, it starts a keep-alive
		 * thread. If the event is a ContextClosedEvent, it stops the keep-alive thread.
		 * @param event The application context event that triggered this method.
		 */
		@Override
		public void onApplicationEvent(ApplicationContextEvent event) {
			if (event instanceof ContextRefreshedEvent) {
				startKeepAliveThread();
			}
			else if (event instanceof ContextClosedEvent) {
				stopKeepAliveThread();
			}
		}

		/**
		 * Starts a keep-alive thread.
		 *
		 * This method creates a new thread that runs indefinitely, keeping the
		 * application alive. The thread is set as a non-daemon thread and named
		 * "keep-alive".
		 * @throws None
		 * @return None
		 */
		private void startKeepAliveThread() {
			Thread thread = new Thread(() -> {
				while (true) {
					try {
						Thread.sleep(Long.MAX_VALUE);
					}
					catch (InterruptedException ex) {
						break;
					}
				}
			});
			if (this.thread.compareAndSet(null, thread)) {
				thread.setDaemon(false);
				thread.setName("keep-alive");
				thread.start();
			}
		}

		/**
		 * Stops the keep alive thread.
		 *
		 * This method interrupts the keep alive thread, if it is running.
		 * @throws SecurityException if a security manager exists and its checkAccess
		 * method doesn't allow the current thread to interrupt the keep alive thread.
		 * @see Thread#interrupt()
		 */
		private void stopKeepAliveThread() {
			Thread thread = this.thread.getAndSet(null);
			if (thread == null) {
				return;
			}
			thread.interrupt();
		}

	}

	/**
	 * Strategy used to handle startup concerns.
	 */
	abstract static class Startup {

		private Duration timeTakenToStarted;

		/**
		 * Returns the start time of the startup process.
		 * @return the start time of the startup process
		 */
		protected abstract long startTime();

		/**
		 * Returns the uptime of the system in milliseconds.
		 * @return the uptime of the system in milliseconds
		 */
		protected abstract Long processUptime();

		/**
		 * This method is declared as abstract and is meant to be overridden by
		 * subclasses. It performs a specific action and returns a String result.
		 * @return the result of the action as a String
		 */
		protected abstract String action();

		/**
		 * Returns the duration it took for the startup process to begin.
		 * @return the duration it took for the startup process to begin
		 */
		final Duration started() {
			long now = System.currentTimeMillis();
			this.timeTakenToStarted = Duration.ofMillis(now - startTime());
			return this.timeTakenToStarted;
		}

		/**
		 * Returns the duration of time taken for the startup process to begin.
		 * @return the duration of time taken for the startup process to begin
		 */
		Duration timeTakenToStarted() {
			return this.timeTakenToStarted;
		}

		/**
		 * Calculates the duration between the current time and the start time of the
		 * application.
		 * @return the duration between the current time and the start time
		 */
		private Duration ready() {
			long now = System.currentTimeMillis();
			return Duration.ofMillis(now - startTime());
		}

		/**
		 * Creates a new instance of the Startup class.
		 *
		 * This method checks if the classes "jdk.crac.management.CRaCMXBean" and
		 * "org.crac.management.CRaCMXBean" are present in the class loader. If both
		 * classes are present, it returns a new instance of the
		 * CoordinatedRestoreAtCheckpointStartup class. Otherwise, it returns a new
		 * instance of the StandardStartup class.
		 * @return a new instance of the Startup class
		 */
		static Startup create() {
			ClassLoader classLoader = Startup.class.getClassLoader();
			return (ClassUtils.isPresent("jdk.crac.management.CRaCMXBean", classLoader)
					&& ClassUtils.isPresent("org.crac.management.CRaCMXBean", classLoader))
							? new CoordinatedRestoreAtCheckpointStartup() : new StandardStartup();
		}

	}

	/**
	 * Standard {@link Startup} implementation.
	 */
	private static final class StandardStartup extends Startup {

		private final Long startTime = System.currentTimeMillis();

		/**
		 * Returns the start time of the StandardStartup object.
		 * @return the start time of the StandardStartup object
		 */
		@Override
		protected long startTime() {
			return this.startTime;
		}

		/**
		 * Returns the uptime of the current Java virtual machine in milliseconds.
		 * @return the uptime of the Java virtual machine in milliseconds, or null if an
		 * error occurs
		 */
		@Override
		protected Long processUptime() {
			try {
				return ManagementFactory.getRuntimeMXBean().getUptime();
			}
			catch (Throwable ex) {
				return null;
			}
		}

		/**
		 * Returns the action performed by the StandardStartup class.
		 * @return the action performed, which is "Started"
		 */
		@Override
		protected String action() {
			return "Started";
		}

	}

	/**
	 * Coordinated-Restore-At-Checkpoint {@link Startup} implementation.
	 */
	private static final class CoordinatedRestoreAtCheckpointStartup extends Startup {

		private final StandardStartup fallback = new StandardStartup();

		/**
		 * Returns the uptime since the last restore operation.
		 * @return the uptime in milliseconds, or the fallback uptime if the uptime is
		 * negative
		 */
		@Override
		protected Long processUptime() {
			long uptime = CRaCMXBean.getCRaCMXBean().getUptimeSinceRestore();
			return (uptime >= 0) ? uptime : this.fallback.processUptime();
		}

		/**
		 * Performs an action based on the restore time. If the restore time is greater
		 * than or equal to 0, it returns "Restored". Otherwise, it delegates the action
		 * to the fallback object.
		 * @return the result of the action
		 */
		@Override
		protected String action() {
			return (restoreTime() >= 0) ? "Restored" : this.fallback.action();
		}

		/**
		 * Returns the restore time in milliseconds.
		 * @return the restore time in milliseconds
		 */
		private long restoreTime() {
			return CRaCMXBean.getCRaCMXBean().getRestoreTime();
		}

		/**
		 * Returns the start time for the coordinated restore at checkpoint startup. If
		 * the restore time is greater than or equal to 0, it returns the restore time.
		 * Otherwise, it returns the start time from the fallback.
		 * @return the start time for the coordinated restore at checkpoint startup
		 */
		@Override
		protected long startTime() {
			long restoreTime = restoreTime();
			return (restoreTime >= 0) ? restoreTime : this.fallback.startTime();
		}

	}

	/**
	 * {@link OrderSourceProvider} used to obtain factory method and target type order
	 * sources. Based on internal {@link DefaultListableBeanFactory} code.
	 */
	private class FactoryAwareOrderSourceProvider implements OrderSourceProvider {

		private final ConfigurableBeanFactory beanFactory;

		private final Map<?, String> instancesToBeanNames;

		/**
		 * Constructs a new FactoryAwareOrderSourceProvider with the specified bean
		 * factory and instances to bean names mapping.
		 * @param beanFactory the configurable bean factory to be used by the order source
		 * provider
		 * @param instancesToBeanNames a mapping of instances to their corresponding bean
		 * names
		 */
		FactoryAwareOrderSourceProvider(ConfigurableBeanFactory beanFactory, Map<?, String> instancesToBeanNames) {
			this.beanFactory = beanFactory;
			this.instancesToBeanNames = instancesToBeanNames;
		}

		/**
		 * Retrieves the order source for the given object.
		 * @param obj the object for which to retrieve the order source
		 * @return the order source for the given object, or null if not found
		 */
		@Override
		public Object getOrderSource(Object obj) {
			String beanName = this.instancesToBeanNames.get(obj);
			return (beanName != null) ? getOrderSource(beanName, obj.getClass()) : null;
		}

		/**
		 * Retrieves the order source for a given bean name and instance type.
		 * @param beanName the name of the bean
		 * @param instanceType the type of the instance
		 * @return an Object array containing the factory method and target type, or null
		 * if the bean definition does not exist
		 */
		private Object getOrderSource(String beanName, Class<?> instanceType) {
			try {
				RootBeanDefinition beanDefinition = (RootBeanDefinition) this.beanFactory
					.getMergedBeanDefinition(beanName);
				Method factoryMethod = beanDefinition.getResolvedFactoryMethod();
				Class<?> targetType = beanDefinition.getTargetType();
				targetType = (targetType != instanceType) ? targetType : null;
				return Stream.of(factoryMethod, targetType).filter(Objects::nonNull).toArray();
			}
			catch (NoSuchBeanDefinitionException ex) {
				return null;
			}
		}

	}

}
