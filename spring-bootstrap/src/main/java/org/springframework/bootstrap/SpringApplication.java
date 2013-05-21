/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.bootstrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;

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
 * <li>{@link Resource} - A XML resource to be loaded by {@link XmlBeanDefinitionReader}</li>
 * 
 * <li>{@link Package} - A Java package to be scanned by
 * {@link ClassPathBeanDefinitionScanner}</li>
 * 
 * <li>{@link CharSequence} - A class name, resource handle or package name to loaded as
 * appropriate. If the {@link CharSequence} cannot be resolved to class and does not
 * resolve to a {@link Resource} that exists it will be considered a {@link Package}.</li>
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

	public static final String DEFAULT_WEB_CONTEXT_CLASS = "org.springframework.bootstrap."
			+ "context.embedded.AnnotationConfigEmbeddedWebApplicationContext";

	private static final String[] WEB_ENVIRONMENT_CLASSES = { "javax.servlet.Servlet",
			"org.springframework.web.context.ConfigurableWebApplicationContext" };

	private Object[] sources;

	private boolean showBanner = true;

	private boolean addCommandLineProperties = true;

	private ResourceLoader resourceLoader;

	private BeanNameGenerator beanNameGenerator;

	private ConfigurableEnvironment environment;

	private ApplicationContext applicationContext;

	private Class<? extends ApplicationContext> applicationContextClass;

	private boolean webEnvironment;

	private List<ApplicationContextInitializer<?>> initializers;

	private String[] defaultCommandLineArgs;

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
		Assert.notEmpty(sources, "Sources must not be empty");
		this.sources = sources;
		initialize();
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
		Assert.notEmpty(sources, "Sources must not be empty");
		this.resourceLoader = resourceLoader;
		this.sources = sources;
		initialize();
	}

	protected void initialize() {
		this.webEnvironment = deduceWebEnvironment();
		this.initializers = new ArrayList<ApplicationContextInitializer<?>>();
		@SuppressWarnings("rawtypes")
		Collection<ApplicationContextInitializer> factories = SpringFactoriesLoader
				.loadFactories(ApplicationContextInitializer.class,
						SpringApplication.class.getClassLoader());
		for (ApplicationContextInitializer<?> initializer : factories) {
			this.initializers.add(initializer);
		}
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
	 * Run the Spring application, creating and refreshing a new
	 * {@link ApplicationContext}.
	 * @param args the application arguments (usually passed from a Java main method)
	 * @return a running {@link ApplicationContext}
	 */
	public ApplicationContext run(String... args) {
		if (this.showBanner) {
			printBanner();
		}
		ApplicationContext context = createApplicationContext();
		postProcessApplicationContext(context);
		addPropertySources(context, args);
		if (context instanceof ConfigurableApplicationContext) {
			applyInitializers((ConfigurableApplicationContext) context);
		}
		load(context, this.sources);
		refresh(context);
		runCommandLineRunners(context, args);
		return context;
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
		for (ApplicationContextInitializer initializer : this.initializers) {
			Class<?> requiredType = GenericTypeResolver.resolveTypeArgument(
					initializer.getClass(), ApplicationContextInitializer.class);
			Assert.isInstanceOf(requiredType, context, "Unable to call initializer.");
			initializer.initialize(context);
		}
	}

	/**
	 * Strategy method used to create the {@link ApplicationContext}. By default this
	 * method will respect any explicitly set application context or application context
	 * class before falling back to a suitable default.
	 * @return the application context (not yet refreshed)
	 * @see #setApplicationContext(ApplicationContext)
	 * @see #setApplicationContextClass(Class)
	 */
	protected ApplicationContext createApplicationContext() {
		if (this.applicationContext != null) {
			return this.applicationContext;
		}

		Class<?> contextClass = this.applicationContextClass;
		if (contextClass == null) {
			try {
				contextClass = Class
						.forName(this.webEnvironment ? DEFAULT_WEB_CONTEXT_CLASS
								: DEFAULT_CONTEXT_CLASS);
			} catch (ClassNotFoundException ex) {
				throw new IllegalStateException(
						"Unable create a default ApplicationContext, "
								+ "please specify an ApplicationContextClass", ex);
			}
		}

		return (ApplicationContext) BeanUtils.instantiate(contextClass);
	}

	/**
	 * Apply any relevant post processing the {@link ApplicationContext}. Subclasses can
	 * apply additional processing as required.
	 * @param context the application context
	 */
	protected void postProcessApplicationContext(ApplicationContext context) {
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

		if (context instanceof AbstractApplicationContext && this.environment != null) {
			((AbstractApplicationContext) context).setEnvironment(this.environment);
		}

		if (context instanceof GenericApplicationContext && this.resourceLoader != null) {
			((GenericApplicationContext) context).setResourceLoader(this.resourceLoader);
		}
	}

	/**
	 * Add any {@link PropertySource}s to the application context environment.
	 * @param context the application context
	 * @param args run arguments
	 */
	protected void addPropertySources(ApplicationContext context, String[] args) {
		Environment environment = context.getEnvironment();
		if (environment instanceof ConfigurableEnvironment) {
			ConfigurableEnvironment configurable = (ConfigurableEnvironment) environment;
			if (this.addCommandLineProperties) {
				// Don't use SimpleCommandLinePropertySource (SPR-10579)
				PropertySource<?> propertySource = new MapPropertySource(
						"commandLineArgs", mergeCommandLineArgs(
								this.defaultCommandLineArgs, args));
				configurable.getPropertySources().addFirst(propertySource);
			}
		}
	}

	/**
	 * Merge two sets of command lines, the defaults and the ones passed in at run time.
	 * 
	 * @param defaults the default values
	 * @param args the ones passed in at runtime
	 * @return a new command line
	 */
	protected Map<String, Object> mergeCommandLineArgs(String[] defaults, String[] args) {

		if (defaults == null) {
			defaults = new String[0];
		}

		List<String> nonopts = new ArrayList<String>();
		Map<String, Object> options = new LinkedHashMap<String, Object>();

		for (String arg : defaults) {
			if (isOptionArg(arg)) {
				addOptionArg(options, arg);
			} else {
				nonopts.add(arg);
			}
		}
		for (String arg : args) {
			if (isOptionArg(arg)) {
				addOptionArg(options, arg);
			} else if (!nonopts.contains(arg)) {
				nonopts.add(arg);
			}
		}

		for (String key : nonopts) {
			options.put(key, "");
		}

		return options;

	}

	private boolean isOptionArg(String arg) {
		return arg.startsWith("--");
	}

	private void addOptionArg(Map<String, Object> map, String arg) {
		String optionText = arg.substring(2, arg.length());
		String optionName;
		String optionValue = "";
		if (optionText.contains("=")) {
			optionName = optionText.substring(0, optionText.indexOf("="));
			optionValue = optionText.substring(optionText.indexOf("=") + 1,
					optionText.length());
		} else {
			optionName = optionText;
		}
		if (optionName.isEmpty()) {
			throw new IllegalArgumentException("Invalid argument syntax: " + arg);
		}
		map.put(optionName, optionValue);
	}

	/**
	 * Load beans into the application context.
	 * @param context the context to load beans into
	 * @param sources the sources to load
	 */
	protected void load(ApplicationContext context, Object[] sources) {
		Assert.isInstanceOf(BeanDefinitionRegistry.class, context);
		BeanDefinitionLoader loader = createBeanDefinitionLoader(
				(BeanDefinitionRegistry) context, sources);
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
			} catch (Exception e) {
				throw new IllegalStateException("Failed to execute CommandLineRunner", e);
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
	 * Sets if this application is running within a web environment. If not specified will
	 * attempt to deduce the environment based on the classpath.
	 * @param webEnvironment if the application is running in a web environment
	 */
	public void setWebEnvironment(boolean webEnvironment) {
		this.webEnvironment = webEnvironment;
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
	 * Sets if a {@link CommandLinePropertySource} should be added to the application
	 * context in order to expose arguments. Defaults to {@code true}.
	 * @param addCommandLineProperties if command line arguments should be exposed
	 */
	public void setAddCommandLineProperties(boolean addCommandLineProperties) {
		this.addCommandLineProperties = addCommandLineProperties;
	}

	/**
	 * Set some default command line arguments which can be overridden by those passed
	 * into the run methods.
	 * @param defaultCommandLineArgs the default command line args to set
	 */
	public void setDefaultCommandLineArgs(String... defaultCommandLineArgs) {
		this.defaultCommandLineArgs = defaultCommandLineArgs;
	}

	/**
	 * Sets the bean name generator that should be used when generating bean names.
	 * @param beanNameGenerator the bean name generator
	 */
	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		this.beanNameGenerator = beanNameGenerator;
	}

	/**
	 * Sets the underlying environment that should be used when loading.
	 * @param environment the environment
	 */
	public void setEnvironment(ConfigurableEnvironment environment) {
		this.environment = environment;
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
	 * @see #setApplicationContext(ApplicationContext)
	 */
	public void setApplicationContextClass(
			Class<? extends ApplicationContext> applicationContextClass) {
		this.applicationContextClass = applicationContextClass;
	}

	/**
	 * Sets a Spring {@link ApplicationContext} that will be used for the application. If
	 * not specified an {@link #DEFAULT_WEB_CONTEXT_CLASS} will be created for web based
	 * applications or an {@link AnnotationConfigApplicationContext} for non web based
	 * applications.
	 * @param applicationContext the spring application context.
	 * @see #setApplicationContextClass(Class)
	 */
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * Sets the {@link ApplicationContextInitializer} that will be applied to the Spring
	 * {@link ApplicationContext}. Any existing initializers will be replaced.
	 * @param initializers the initializers to set
	 */
	public void setInitializers(
			Collection<? extends ApplicationContextInitializer<?>> initializers) {
		this.initializers = new ArrayList<ApplicationContextInitializer<?>>(initializers);
	}

	/**
	 * Add {@link ApplicationContextInitializer}s to be applied to the Spring
	 * {@link ApplicationContext} .
	 * @param initializers the initializers to add
	 */
	public void addInitializers(ApplicationContextInitializer<?>... initializers) {
		this.initializers.addAll(Arrays.asList(initializers));
	}

	/**
	 * Returns a mutable list of the {@link ApplicationContextInitializer}s that will be
	 * applied to the Spring {@link ApplicationContext}.
	 * @return the initializers
	 */
	public List<ApplicationContextInitializer<?>> getInitializers() {
		return this.initializers;
	}

	/**
	 * Static helper that can be used to run a {@link SpringApplication} from the
	 * specified source using default settings.
	 * @param source the source to load
	 * @param args the application arguments (usually passed from a Java main method)
	 * @return the running {@link ApplicationContext}
	 */
	public static ApplicationContext run(Object source, String... args) {
		return run(new Object[] { source }, args);
	}

	/**
	 * Static helper that can be used to run a {@link SpringApplication} from the
	 * specified sources using default settings.
	 * @param sources the sources to load
	 * @param args the application arguments (usually passed from a Java main method)
	 * @return the running {@link ApplicationContext}
	 */
	public static ApplicationContext run(Object[] sources, String[] args) {
		return new SpringApplication(sources).run(args);
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
			} finally {
				close(context);
			}

		} catch (Exception e) {
			e.printStackTrace();
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
			} catch (Exception e) {
				exitCode = (exitCode == 0 ? 1 : exitCode);
				e.printStackTrace();
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

}
