/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StandardServletEnvironment;

/**
 * Classes that can be used to bootstrap and launch a Spring application from a Java main
 * method. By default class will perform the following steps to bootstrap your
 * application:
 * 
 * <ul>
 * <li>Create an appropriate {@link ApplicationContext} instance (depending on your
 * classpath)</li>
 * 
 * <li>Register a {@link CommandLinePropertySource} to expose command line arguments as
 * Spring properties</li>
 * 
 * <li>Refresh the application context, loading all singleton beans</li>
 * 
 * <li>Trigger any {@link CommandLineRunner} beans</li>
 * </ul>
 * 
 * In most circumstances the static {@link #run(Object, String[])} method can be called
 * directly from your {@literal main} method to bootstrap your application:
 * 
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableAutoConfiguration
 * public class MyApplication  {
 * 
 * // ... Bean definitions
 * 
 * public static void main(String[] args) throws Exception {
 *   SpringApplication.run(MyApplication.class, args);
 * }
 * </pre>
 * 
 * <p>
 * For more advanced configuration a {@link SpringApplication} instance can be created and
 * customized before being run:
 * 
 * <pre class="code">
 * public static void main(String[] args) throws Exception {
 *   SpringApplication app = new SpringApplication(MyApplication.class);
 *   // ... customize app settings here
 *   app.run(args)
 * }
 * </pre>
 * 
 * {@link SpringApplication}s can read beans from a variety of different sources. It is
 * generally recommended that a single {@code @Configuration} class is used to bootstrap
 * your application, however, any of the following sources can also be used:
 * 
 * <p>
 * <ul>
 * <li>{@link Class} - A Java class to be loaded by {@link AnnotatedBeanDefinitionReader}</li>
 * 
 * <li>{@link Resource} - An XML resource to be loaded by {@link XmlBeanDefinitionReader},
 * or a groovy script to be loaded by {@link GroovyBeanDefinitionReader}</li>
 * 
 * <li>{@link Package} - A Java package to be scanned by
 * {@link ClassPathBeanDefinitionScanner}</li>
 * 
 * <li>{@link CharSequence} - A class name, resource handle or package name to loaded as
 * appropriate. If the {@link CharSequence} cannot be resolved to class and does not
 * resolve to a {@link Resource} that exists it will be considered a {@link Package}.</li>
 * 
 * </ul>
 * 
 * @author Phillip Webb
 * @author Dave Syer
 * @see #run(Object, String[])
 * @see #run(Object[], String[])
 * @see #SpringApplication(Object...)
 */
public class SpringApplication {

	private static final String DEFAULT_CONTEXT_CLASS = "org.springframework.context."
			+ "annotation.AnnotationConfigApplicationContext";

	public static final String DEFAULT_WEB_CONTEXT_CLASS = "org.springframework."
			+ "boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext";

	private static final String[] WEB_ENVIRONMENT_CLASSES = { "javax.servlet.Servlet",
			"org.springframework.web.context.ConfigurableWebApplicationContext" };

	private final Log log = LogFactory.getLog(getClass());

	private Set<Object> sources = new LinkedHashSet<Object>();

	private Class<?> mainApplicationClass;

	private boolean showBanner = true;

	private boolean logStartupInfo = true;

	private boolean addCommandLineProperties = true;

	private ResourceLoader resourceLoader;

	private BeanNameGenerator beanNameGenerator;

	private ConfigurableEnvironment environment;

	private Class<? extends ConfigurableApplicationContext> applicationContextClass;

	private boolean webEnvironment;

	private boolean headless = true;

	private Set<ApplicationContextInitializer<?>> initializers;

	private Set<ApplicationListener<?>> listeners;

	private Map<String, Object> defaultProperties;

	private Set<String> profiles = new HashSet<String>();

	/**
	 * Crate a new {@link SpringApplication} instance. The application context will load
	 * beans from the specified sources (see {@link SpringApplication class-level}
	 * documentation for details. The instance can be customized before calling
	 * {@link #run(String...)}.
	 * @param sources the bean sources
	 * @see #run(Object, String[])
	 * @see #SpringApplication(ResourceLoader, Object...)
	 */
	public SpringApplication(Object... sources) {
		initialize(sources);
	}

	/**
	 * Crate a new {@link SpringApplication} instance. The application context will load
	 * beans from the specified sources (see {@link SpringApplication class-level}
	 * documentation for details. The instance can be customized before calling
	 * {@link #run(String...)}.
	 * @param resourceLoader the resource loader to use
	 * @param sources the bean sources
	 * @see #run(Object, String[])
	 * @see #SpringApplication(ResourceLoader, Object...)
	 */
	public SpringApplication(ResourceLoader resourceLoader, Object... sources) {
		this.resourceLoader = resourceLoader;
		initialize(sources);
	}

	private void initialize(Object[] sources) {
		if (sources != null && sources.length > 0) {
			this.sources.addAll(Arrays.asList(sources));
		}
		this.webEnvironment = deduceWebEnvironment();
		this.initializers = new LinkedHashSet<ApplicationContextInitializer<?>>();
		this.listeners = new LinkedHashSet<ApplicationListener<?>>();
		@SuppressWarnings("unchecked")
		Collection<? extends ApplicationContextInitializer<?>> initializers = (Collection<? extends ApplicationContextInitializer<?>>) getSpringFactoriesInstances(ApplicationContextInitializer.class);
		this.initializers.addAll(initializers);
		for (ApplicationContextInitializer<?> initializer : initializers) {
			if (initializer instanceof ApplicationListener) {
				addListeners((ApplicationListener<?>) initializer);
			}
		}
		@SuppressWarnings("unchecked")
		Collection<? extends ApplicationListener<?>> listeners = (Collection<? extends ApplicationListener<?>>) getSpringFactoriesInstances(ApplicationListener.class);
		this.listeners.addAll(listeners);
		for (ApplicationListener<?> listener : listeners) {
			if (listener instanceof ApplicationContextInitializer) {
				addInitializers((ApplicationContextInitializer<?>) listener);
			}
		}
		this.mainApplicationClass = deduceMainApplicationClass();
	}

	private boolean deduceWebEnvironment() {
		for (String className : WEB_ENVIRONMENT_CLASSES) {
			if (!ClassUtils.isPresent(className, null)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns objects loaded via the {@link SpringFactoriesLoader}.
	 */
	private <T> Collection<? extends T> getSpringFactoriesInstances(Class<T> type) {
		ClassLoader classLoader = SpringApplication.class.getClassLoader();

		// Use names and ensure unique to protect against duplicates
		Set<String> names = new LinkedHashSet<String>(
				SpringFactoriesLoader.loadFactoryNames(type, classLoader));
		List<T> instances = new ArrayList<T>(names.size());

		// Create instances from the names
		for (String name : names) {
			try {
				Class<?> instanceClass = ClassUtils.forName(name, classLoader);
				Assert.isAssignable(type, instanceClass);
				@SuppressWarnings("unchecked")
				T instance = (T) instanceClass.newInstance();
				instances.add(instance);
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException("Cannot instantiate " + type + " : "
						+ name, ex);
			}
		}

		AnnotationAwareOrderComparator.sort(instances);
		return instances;
	}

	private Class<?> deduceMainApplicationClass() {
		try {
			StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();
			for (StackTraceElement stackTraceElement : stackTrace) {
				if ("main".equals(stackTraceElement.getMethodName())) {
					return Class.forName(stackTraceElement.getClassName());
				}
			}
		}
		catch (ClassNotFoundException ex) {
			// Swallow and continue
		}
		return null;
	}

	/**
	 * Run the Spring application, creating and refreshing a new
	 * {@link ApplicationContext}.
	 * @param args the application arguments (usually passed from a Java main method)
	 * @return a running {@link ApplicationContext}
	 */
	public ConfigurableApplicationContext run(String... args) {

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		ConfigurableApplicationContext context = null;

		System.setProperty("java.awt.headless", Boolean.toString(this.headless));

		ApplicationEventMulticaster multicaster = createApplicationEventMulticaster();
		try {
			Set<Object> sources = getSources();
			registerListeners(multicaster, sources);
			// Allow logging and stuff to initialize very early
			multicaster.multicastEvent(new SpringApplicationStartEvent(this, args));

			// Create and configure the environment
			ConfigurableEnvironment environment = getOrCreateEnvironment();
			addPropertySources(environment, args);
			for (String profile : this.profiles) {
				environment.addActiveProfile(profile);
			}

			// Notify listeners of the environment creation
			multicaster.multicastEvent(new SpringApplicationEnvironmentAvailableEvent(
					this, environment, args));

			// Sources might have changed when environment was applied
			sources = getSources();
			Assert.notEmpty(sources, "Sources must not be empty");
			if (this.showBanner) {
				printBanner();
			}

			// Some sources might be listeners
			registerListeners(multicaster, sources);

			// Create, load, refresh and run the ApplicationContext
			context = createApplicationContext();
			registerApplicationEventMulticaster(context, multicaster);
			context.registerShutdownHook();
			context.setEnvironment(environment);
			postProcessApplicationContext(context);
			applyInitializers(context);
			if (this.logStartupInfo) {
				logStartupInfo(context.getParent() == null);
			}

			load(context, sources.toArray(new Object[sources.size()]));
			// Notify listeners of intention to refresh
			multicaster.multicastEvent(new SpringApplicationBeforeRefreshEvent(this,
					context, args));
			refresh(context);

			stopWatch.stop();
			if (this.logStartupInfo) {
				new StartupInfoLogger(this.mainApplicationClass).logStarted(
						getApplicationLog(), stopWatch);
			}

			afterRefresh(context, args);
			return context;
		}
		catch (RuntimeException ex) {
			handleError(context, multicaster, ex, args);
			throw ex;
		}
		catch (Error ex) {
			handleError(context, multicaster, ex, args);
			throw ex;
		}

	}

	protected void handleError(ConfigurableApplicationContext context,
			ApplicationEventMulticaster multicaster, Throwable ex, String... args) {
		multicaster.multicastEvent(new SpringApplicationErrorEvent(this, context, args,
				ex));
		if (context != null) {
			context.close();
		}
	}

	private void registerListeners(ApplicationEventMulticaster multicaster,
			Set<Object> sources) {
		for (Object object : sources) {
			if (object instanceof ApplicationListener) {
				multicaster.addApplicationListener((ApplicationListener<?>) object);
			}
			if (object instanceof ApplicationContextInitializer) {
				addInitializers((ApplicationContextInitializer<?>) object);
			}
		}
	}

	private void registerApplicationEventMulticaster(
			ConfigurableApplicationContext context,
			ApplicationEventMulticaster multicaster) {
		context.getBeanFactory().registerSingleton(
				AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME,
				multicaster);
		if (multicaster instanceof BeanFactoryAware) {
			((BeanFactoryAware) multicaster).setBeanFactory(context.getBeanFactory());
		}
	}

	private ApplicationEventMulticaster createApplicationEventMulticaster() {
		final ApplicationEventMulticaster multicaster = new SpringApplicationEventMulticaster();
		for (ApplicationListener<?> listener : getListeners()) {
			multicaster.addApplicationListener(listener);
		}
		return multicaster;
	}

	private void afterRefresh(ConfigurableApplicationContext context, String[] args) {
		runCommandLineRunners(context, args);
	}

	private ConfigurableEnvironment getOrCreateEnvironment() {
		if (this.environment != null) {
			return this.environment;
		}
		if (this.webEnvironment) {
			return new StandardServletEnvironment();
		}
		return new StandardEnvironment();

	}

	/**
	 * Add any {@link PropertySource}s to the environment.
	 * @param environment the environment
	 * @param args run arguments
	 */
	protected void addPropertySources(ConfigurableEnvironment environment, String[] args) {
		MutablePropertySources sources = environment.getPropertySources();
		if (this.defaultProperties != null && !this.defaultProperties.isEmpty()) {
			sources.addLast(new MapPropertySource("defaultProperties",
					this.defaultProperties));
		}
		if (this.addCommandLineProperties && args.length > 0) {
			String name = CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME;
			if (sources.contains(name)) {
				PropertySource<?> source = sources.get(name);
				CompositePropertySource composite = new CompositePropertySource(name);
				composite.addPropertySource(new SimpleCommandLinePropertySource(name
						+ "-" + args.hashCode(), args));
				composite.addPropertySource(source);
				sources.replace(name, composite);
			}
			else {
				sources.addFirst(new SimpleCommandLinePropertySource(args));
			}
		}
	}

	/**
	 * Print a simple banner message to the console. Subclasses can override this method
	 * to provide additional or alternative banners.
	 * @see #setShowBanner(boolean)
	 */
	protected void printBanner() {
		Banner.write(System.out);
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
			Class<?> requiredType = GenericTypeResolver.resolveTypeArgument(
					initializer.getClass(), ApplicationContextInitializer.class);
			Assert.isInstanceOf(requiredType, context, "Unable to call initializer.");
			initializer.initialize(context);
		}
	}

	/**
	 * Called to log startup information, subclasses may override to add additional
	 * logging.
	 * 
	 * @param isRoot true if this application is the root of a context hierarchy
	 */
	protected void logStartupInfo(boolean isRoot) {
		if (isRoot) {
			new StartupInfoLogger(this.mainApplicationClass)
					.logStarting(getApplicationLog());
		}
	}

	/**
	 * Returns the {@link Log} for the application. By default will be deduced.
	 * @return the application log
	 */
	protected Log getApplicationLog() {
		if (this.mainApplicationClass == null) {
			return this.log;
		}
		return LogFactory.getLog(this.mainApplicationClass);
	}

	/**
	 * Strategy method used to create the {@link ApplicationContext}. By default this
	 * method will respect any explicitly set application context or application context
	 * class before falling back to a suitable default.
	 * @return the application context (not yet refreshed)
	 * @see #setApplicationContextClass(Class)
	 */
	protected ConfigurableApplicationContext createApplicationContext() {

		Class<?> contextClass = this.applicationContextClass;
		if (contextClass == null) {
			try {
				contextClass = Class
						.forName(this.webEnvironment ? DEFAULT_WEB_CONTEXT_CLASS
								: DEFAULT_CONTEXT_CLASS);
			}
			catch (ClassNotFoundException ex) {
				throw new IllegalStateException(
						"Unable create a default ApplicationContext, "
								+ "please specify an ApplicationContextClass", ex);
			}
		}

		return (ConfigurableApplicationContext) BeanUtils.instantiate(contextClass);

	}

	/**
	 * Apply any relevant post processing the {@link ApplicationContext}. Subclasses can
	 * apply additional processing as required.
	 * @param context the application context
	 */
	protected void postProcessApplicationContext(ConfigurableApplicationContext context) {
		if (this.webEnvironment) {
			if (context instanceof ConfigurableWebApplicationContext) {
				ConfigurableWebApplicationContext configurableContext = (ConfigurableWebApplicationContext) context;
				if (this.beanNameGenerator != null) {
					configurableContext.getBeanFactory().registerSingleton(
							AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR,
							this.beanNameGenerator);
				}
			}
		}

		if (this.resourceLoader != null) {
			if (context instanceof GenericApplicationContext) {
				((GenericApplicationContext) context)
						.setResourceLoader(this.resourceLoader);
			}
			if (context instanceof DefaultResourceLoader) {
				((DefaultResourceLoader) context).setClassLoader(this.resourceLoader
						.getClassLoader());
			}
		}
	}

	/**
	 * Load beans into the application context.
	 * @param context the context to load beans into
	 * @param sources the sources to load
	 */
	protected void load(ApplicationContext context, Object[] sources) {
		if (this.log.isDebugEnabled()) {
			this.log.debug("Loading source "
					+ StringUtils.arrayToCommaDelimitedString(sources));
		}
		BeanDefinitionLoader loader = createBeanDefinitionLoader(
				getBeanDefinitionRegistry(context), sources);
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
	 * 
	 * @return the resourceLoader the resource loader that will be used in the
	 * ApplicationContext (or null if the default)
	 */
	public ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}

	/**
	 * Either the ClassLoader that will be used in the ApplicationContext (if
	 * {@link #setResourceLoader(ResourceLoader) resourceLoader} is set, or the context
	 * class loader (if not null), or the loader of the Spring {@link ClassUtils} class.
	 * 
	 * @return a ClassLoader (never null)
	 */
	public ClassLoader getClassLoader() {
		if (this.resourceLoader != null) {
			return this.resourceLoader.getClassLoader();
		}
		return ClassUtils.getDefaultClassLoader();
	}

	/**
	 * @param context the application context
	 * @return the BeanDefinitionRegistry if it can be determined
	 */
	private BeanDefinitionRegistry getBeanDefinitionRegistry(ApplicationContext context) {
		if (context instanceof BeanDefinitionRegistry) {
			return (BeanDefinitionRegistry) context;
		}
		if (context instanceof AbstractApplicationContext) {
			return (BeanDefinitionRegistry) ((AbstractApplicationContext) context)
					.getBeanFactory();
		}
		throw new IllegalStateException("Could not locate BeanDefinitionRegistry");
	}

	/**
	 * Factory method used to create the {@link BeanDefinitionLoader}.
	 * @param registry the bean definition registry
	 * @param sources the sources to load
	 * @return the {@link BeanDefinitionLoader} that will be used to load beans
	 */
	protected BeanDefinitionLoader createBeanDefinitionLoader(
			BeanDefinitionRegistry registry, Object[] sources) {
		return new BeanDefinitionLoader(registry, sources);
	}

	private void runCommandLineRunners(ApplicationContext context, String... args) {
		List<CommandLineRunner> runners = new ArrayList<CommandLineRunner>(context
				.getBeansOfType(CommandLineRunner.class).values());
		AnnotationAwareOrderComparator.sort(runners);
		for (CommandLineRunner runner : runners) {
			try {
				runner.run(args);
			}
			catch (Exception ex) {
				throw new IllegalStateException("Failed to execute CommandLineRunner", ex);
			}
		}
	}

	/**
	 * Refresh the underlying {@link ApplicationContext}.
	 * @param applicationContext the application context to refresh
	 */
	protected void refresh(ApplicationContext applicationContext) {
		Assert.isInstanceOf(AbstractApplicationContext.class, applicationContext);
		((AbstractApplicationContext) applicationContext).refresh();
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
	 * Sets if this application is running within a web environment. If not specified will
	 * attempt to deduce the environment based on the classpath.
	 * @param webEnvironment if the application is running in a web environment
	 */
	public void setWebEnvironment(boolean webEnvironment) {
		this.webEnvironment = webEnvironment;
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
	 * Sets if the Spring banner should be displayed when the application runs. Defaults
	 * to {@code true}.
	 * @param showBanner if the banner should be shown
	 * @see #printBanner()
	 */
	public void setShowBanner(boolean showBanner) {
		this.showBanner = showBanner;
	}

	/**
	 * Sets if the application information should be logged when the application starts.
	 * Defaults to {@code true}
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
	 * Set default environment properties which will be used in addition to those in the
	 * existing {@link Environment}.
	 * @param defaultProperties the additional properties to set
	 */
	public void setDefaultProperties(Map<String, Object> defaultProperties) {
		this.defaultProperties = defaultProperties;
	}

	/**
	 * Convenient alternative to {@link #setDefaultProperties(Map)}.
	 * 
	 * @param defaultProperties some {@link Properties}
	 */
	public void setDefaultProperties(Properties defaultProperties) {
		this.defaultProperties = new HashMap<String, Object>();
		for (Object key : Collections.list(defaultProperties.propertyNames())) {
			this.defaultProperties.put((String) key, defaultProperties.get(key));
		}
	}

	/**
	 * Set additional profile values to use (on top of those set in system or command line
	 * properties).
	 * 
	 * @param profiles the additional profiles to set
	 */
	public void setAdditionalProfiles(Collection<String> profiles) {
		this.profiles = new LinkedHashSet<String>(profiles);
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
		this.environment = environment;
	}

	/**
	 * Returns a mutable set of the sources that will be added to an ApplicationContext
	 * when {@link #run(String...)} is called.
	 * @return the sources the application sources.
	 * @see #SpringApplication(Object...)
	 */
	public Set<Object> getSources() {
		return this.sources;
	}

	/**
	 * The sources that will be used to create an ApplicationContext. A valid source is
	 * one of: a class, class name, package, package name, or an XML resource location.
	 * Can also be set using constructors and static convenience methods (e.g.
	 * {@link #run(Object[], String[])}).
	 * <p>
	 * NOTE: sources defined here will be used in addition to any sources specified on
	 * construction.
	 * @param sources the sources to set
	 * @see #SpringApplication(Object...)
	 */
	public void setSources(Set<Object> sources) {
		Assert.notNull(sources, "Sources must not be null");
		this.sources.addAll(sources);
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
	 * Sets the type of Spring {@link ApplicationContext} that will be created. If not
	 * specified defaults to {@link #DEFAULT_WEB_CONTEXT_CLASS} for web based applications
	 * or {@link AnnotationConfigApplicationContext} for non web based applications.
	 * @param applicationContextClass the context class to set
	 */
	public void setApplicationContextClass(
			Class<? extends ConfigurableApplicationContext> applicationContextClass) {
		this.applicationContextClass = applicationContextClass;
		if (!WebApplicationContext.class.isAssignableFrom(applicationContextClass)) {
			this.webEnvironment = false;
		}
	}

	/**
	 * Sets the {@link ApplicationContextInitializer} that will be applied to the Spring
	 * {@link ApplicationContext}. Any existing initializers will be replaced. Any
	 * initializers that are also {@link ApplicationListener} will be added to the
	 * {@link #addListeners(ApplicationListener...) listeners} automatically
	 * @param initializers the initializers to set
	 */
	public void setInitializers(
			Collection<? extends ApplicationContextInitializer<?>> initializers) {
		this.initializers = new LinkedHashSet<ApplicationContextInitializer<?>>(
				initializers);
		for (ApplicationContextInitializer<?> initializer : initializers) {
			if (initializer instanceof ApplicationListener) {
				this.listeners.add((ApplicationListener<?>) initializer);
			}
		}
	}

	/**
	 * Add {@link ApplicationContextInitializer}s to be applied to the Spring
	 * {@link ApplicationContext}. Any initializers that are also
	 * {@link ApplicationListener} will be added to the
	 * {@link #addListeners(ApplicationListener...) listeners} automatically.
	 * @param initializers the initializers to add
	 */
	public void addInitializers(ApplicationContextInitializer<?>... initializers) {
		this.initializers.addAll(Arrays.asList(initializers));
		for (ApplicationContextInitializer<?> initializer : initializers) {
			if (initializer instanceof ApplicationListener) {
				this.listeners.add((ApplicationListener<?>) initializer);
			}
		}
	}

	/**
	 * Returns readonly set of the {@link ApplicationContextInitializer}s that will be
	 * applied to the Spring {@link ApplicationContext}.
	 * @return the initializers
	 */
	public Set<ApplicationContextInitializer<?>> getInitializers() {
		return Collections.unmodifiableSet(this.initializers);
	}

	/**
	 * Sets the {@link ApplicationListener}s that will be applied to the SpringApplication
	 * and registered with the {@link ApplicationContext}. Any existing listeners will be
	 * replaced. Any listeners that are also {@link ApplicationContextInitializer} will be
	 * added to the {@link #addInitializers(ApplicationContextInitializer...)
	 * initializers} automatically.
	 * @param listeners the listeners to set
	 */
	public void setListeners(Collection<? extends ApplicationListener<?>> listeners) {
		this.listeners = new LinkedHashSet<ApplicationListener<?>>(listeners);
		for (ApplicationListener<?> listener : listeners) {
			if (listener instanceof ApplicationContextInitializer) {
				this.initializers.add((ApplicationContextInitializer<?>) listener);
			}
		}
	}

	/**
	 * Add {@link ApplicationListener}s to be applied to the SpringApplication and
	 * registered with the {@link ApplicationContext}. Any listeners that are also
	 * {@link ApplicationContextInitializer} will be added to the
	 * {@link #addInitializers(ApplicationContextInitializer...) initializers}
	 * automatically.
	 * @param listeners the listeners to add
	 */
	public void addListeners(ApplicationListener<?>... listeners) {
		this.listeners.addAll(Arrays.asList(listeners));
		for (ApplicationListener<?> listener : listeners) {
			if (listener instanceof ApplicationContextInitializer) {
				this.initializers.add((ApplicationContextInitializer<?>) listener);
			}
		}
	}

	/**
	 * Returns readonly set of the {@link ApplicationListener}s that will be applied to
	 * the SpringApplication and registered with the {@link ApplicationContext}.
	 * @return the listeners
	 */
	public Set<ApplicationListener<?>> getListeners() {
		return Collections.unmodifiableSet(new LinkedHashSet<ApplicationListener<?>>(
				this.listeners));
	}

	/**
	 * Static helper that can be used to run a {@link SpringApplication} from the
	 * specified source using default settings.
	 * @param source the source to load
	 * @param args the application arguments (usually passed from a Java main method)
	 * @return the running {@link ApplicationContext}
	 */
	public static ConfigurableApplicationContext run(Object source, String... args) {
		return run(new Object[] { source }, args);
	}

	/**
	 * Static helper that can be used to run a {@link SpringApplication} from the
	 * specified sources using default settings and user supplied arguments.
	 * @param sources the sources to load
	 * @param args the application arguments (usually passed from a Java main method)
	 * @return the running {@link ApplicationContext}
	 */
	public static ConfigurableApplicationContext run(Object[] sources, String[] args) {
		return new SpringApplication(sources).run(args);
	}

	/**
	 * A basic main that can be used to launch an application. This method is useful when
	 * application sources are defined via a {@literal --spring.main.sources} command line
	 * argument.
	 * <p>
	 * Most developers will want to define their own main method can call the
	 * {@link #run(Object, String...) run} method instead.
	 * @param args command line arguments
	 * @see SpringApplication#run(Object[], String[])
	 * @see SpringApplication#run(Object, String...)
	 */
	public static void main(String[] args) throws Exception {
		SpringApplication.run(new Object[0], args);
	}

	/**
	 * Static helper that can be used to exit a {@link SpringApplication} and obtain a
	 * code indicating success (0) or otherwise. Does not throw exceptions but should
	 * print stack traces of any encountered. Applies the specified
	 * {@link ExitCodeGenerator} in addition to any Spring beans that implement
	 * {@link ExitCodeGenerator}. In the case of multiple exit codes the highest value
	 * will be used (or if all values are negative, the lowest value will be used)
	 * @param context the context to close if possible
	 * @param exitCodeGenerators exist code generators
	 * @return the outcome (0 if successful)
	 */
	public static int exit(ApplicationContext context,
			ExitCodeGenerator... exitCodeGenerators) {
		int exitCode = 0;
		try {
			try {
				List<ExitCodeGenerator> generators = new ArrayList<ExitCodeGenerator>();
				generators.addAll(Arrays.asList(exitCodeGenerators));
				generators.addAll(context.getBeansOfType(ExitCodeGenerator.class)
						.values());
				exitCode = getExitCode(generators);
			}
			finally {
				close(context);
			}

		}
		catch (Exception ex) {
			ex.printStackTrace();
			exitCode = (exitCode == 0 ? 1 : exitCode);
		}
		return exitCode;
	}

	private static int getExitCode(List<ExitCodeGenerator> exitCodeGenerators) {
		int exitCode = 0;
		for (ExitCodeGenerator exitCodeGenerator : exitCodeGenerators) {
			try {
				int value = exitCodeGenerator.getExitCode();
				if (value > 0 && value > exitCode || value < 0 && value < exitCode) {
					exitCode = value;
				}
			}
			catch (Exception ex) {
				exitCode = (exitCode == 0 ? 1 : exitCode);
				ex.printStackTrace();
			}
		}
		return exitCode;
	}

	private static void close(ApplicationContext context) {
		if (context instanceof ConfigurableApplicationContext) {
			ConfigurableApplicationContext closable = (ConfigurableApplicationContext) context;
			closable.close();
		}
	}

	private static class SpringApplicationEventMulticaster extends
			SimpleApplicationEventMulticaster implements ApplicationEventPublisher {

		@Override
		public void publishEvent(ApplicationEvent event) {
			multicastEvent(event);
		}

		@Override
		protected Collection<ApplicationListener<?>> getApplicationListeners(
				ApplicationEvent event) {
			List<ApplicationListener<?>> listeners = new ArrayList<ApplicationListener<?>>(
					super.getApplicationListeners(event));
			if (event instanceof SpringApplicationErrorEvent) {
				Collections.reverse(listeners);
			}
			return listeners;
		}

		@Override
		public void addApplicationListener(ApplicationListener<?> listener) {
			super.addApplicationListener(listener);
			if (listener instanceof ApplicationEventPublisherAware) {
				((ApplicationEventPublisherAware) listener)
						.setApplicationEventPublisher(this);
			}
		}

	}

}
