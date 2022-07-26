/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.builder;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.boot.ApplicationContextFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.BootstrapRegistryInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.util.StringUtils;

/**
 * Builder for {@link SpringApplication} and {@link ApplicationContext} instances with
 * convenient fluent API and context hierarchy support. Simple example of a context
 * hierarchy:
 *
 * <pre class="code">
 * new SpringApplicationBuilder(ParentConfig.class).child(ChildConfig.class).run(args);
 * </pre>
 *
 * Another common use case is setting active profiles and default properties to set up the
 * environment for an application:
 *
 * <pre class="code">
 * new SpringApplicationBuilder(Application.class).profiles(&quot;server&quot;)
 * 		.properties(&quot;transport=local&quot;).run(args);
 * </pre>
 *
 * <p>
 * If your needs are simpler, consider using the static convenience methods in
 * SpringApplication instead.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @since 1.0.0
 * @see SpringApplication
 */
public class SpringApplicationBuilder {

	private final SpringApplication application;

	private ConfigurableApplicationContext context;

	private SpringApplicationBuilder parent;

	private final AtomicBoolean running = new AtomicBoolean();

	private final Set<Class<?>> sources = new LinkedHashSet<>();

	private final Map<String, Object> defaultProperties = new LinkedHashMap<>();

	private ConfigurableEnvironment environment;

	private Set<String> additionalProfiles = new LinkedHashSet<>();

	private boolean registerShutdownHookApplied;

	private boolean configuredAsChild = false;

	public SpringApplicationBuilder(Class<?>... sources) {
		this(null, sources);
	}

	public SpringApplicationBuilder(ResourceLoader resourceLoader, Class<?>... sources) {
		this.application = createSpringApplication(resourceLoader, sources);
	}

	/**
	 * Creates a new {@link SpringApplication} instance from the given sources using the
	 * given {@link ResourceLoader}. Subclasses may override in order to provide a custom
	 * subclass of {@link SpringApplication}.
	 * @param resourceLoader the resource loader (can be null)
	 * @param sources the sources
	 * @return the {@link SpringApplication} instance
	 * @since 2.6.0
	 */
	protected SpringApplication createSpringApplication(ResourceLoader resourceLoader, Class<?>... sources) {
		return new SpringApplication(resourceLoader, sources);
	}

	/**
	 * Accessor for the current application context.
	 * @return the current application context (or null if not yet running)
	 */
	public ConfigurableApplicationContext context() {
		return this.context;
	}

	/**
	 * Accessor for the current application.
	 * @return the current application (never null)
	 */
	public SpringApplication application() {
		return this.application;
	}

	/**
	 * Create an application context (and its parent if specified) with the command line
	 * args provided. The parent is run first with the same arguments if it has not yet
	 * been started.
	 * @param args the command line arguments
	 * @return an application context created from the current state
	 */
	public ConfigurableApplicationContext run(String... args) {
		if (this.running.get()) {
			// If already created we just return the existing context
			return this.context;
		}
		configureAsChildIfNecessary(args);
		if (this.running.compareAndSet(false, true)) {
			synchronized (this.running) {
				// If not already running copy the sources over and then run.
				this.context = build().run(args);
			}
		}
		return this.context;
	}

	private void configureAsChildIfNecessary(String... args) {
		if (this.parent != null && !this.configuredAsChild) {
			this.configuredAsChild = true;
			if (!this.registerShutdownHookApplied) {
				this.application.setRegisterShutdownHook(false);
			}
			initializers(new ParentContextApplicationContextInitializer(this.parent.run(args)));
		}
	}

	/**
	 * Returns a fully configured {@link SpringApplication} that is ready to run.
	 * @return the fully configured {@link SpringApplication}.
	 */
	public SpringApplication build() {
		return build(new String[0]);
	}

	/**
	 * Returns a fully configured {@link SpringApplication} that is ready to run. Any
	 * parent that has been configured will be run with the given {@code args}.
	 * @param args the parent's args
	 * @return the fully configured {@link SpringApplication}.
	 */
	public SpringApplication build(String... args) {
		configureAsChildIfNecessary(args);
		this.application.addPrimarySources(this.sources);
		return this.application;
	}

	/**
	 * Create a child application with the provided sources. Default args and environment
	 * are copied down into the child, but everything else is a clean sheet.
	 * @param sources the sources for the application (Spring configuration)
	 * @return the child application builder
	 */
	public SpringApplicationBuilder child(Class<?>... sources) {
		SpringApplicationBuilder child = new SpringApplicationBuilder();
		child.sources(sources);

		// Copy environment stuff from parent to child
		child.properties(this.defaultProperties).environment(this.environment)
				.additionalProfiles(this.additionalProfiles);
		child.parent = this;

		// It's not possible if embedded web server are enabled to support web contexts as
		// parents because the servlets cannot be initialized at the right point in
		// lifecycle.
		web(WebApplicationType.NONE);

		// Probably not interested in multiple banners
		bannerMode(Banner.Mode.OFF);

		// Make sure sources get copied over
		this.application.addPrimarySources(this.sources);

		return child;
	}

	/**
	 * Add a parent application with the provided sources. Default args and environment
	 * are copied up into the parent, but everything else is a clean sheet.
	 * @param sources the sources for the application (Spring configuration)
	 * @return the parent builder
	 */
	public SpringApplicationBuilder parent(Class<?>... sources) {
		if (this.parent == null) {
			this.parent = new SpringApplicationBuilder(sources).web(WebApplicationType.NONE)
					.properties(this.defaultProperties).environment(this.environment);
		}
		else {
			this.parent.sources(sources);
		}
		return this.parent;
	}

	private SpringApplicationBuilder runAndExtractParent(String... args) {
		if (this.context == null) {
			run(args);
		}
		if (this.parent != null) {
			return this.parent;
		}
		throw new IllegalStateException(
				"No parent defined yet (please use the other overloaded parent methods to set one)");
	}

	/**
	 * Add an already running parent context to an existing application.
	 * @param parent the parent context
	 * @return the current builder (not the parent)
	 */
	public SpringApplicationBuilder parent(ConfigurableApplicationContext parent) {
		this.parent = new SpringApplicationBuilder();
		this.parent.context = parent;
		this.parent.running.set(true);
		return this;
	}

	/**
	 * Create a sibling application (one with the same parent). A side effect of calling
	 * this method is that the current application (and its parent) are started without
	 * any arguments if they are not already running. To supply arguments when starting
	 * the current application and its parent use {@link #sibling(Class[], String...)}
	 * instead.
	 * @param sources the sources for the application (Spring configuration)
	 * @return the new sibling builder
	 */
	public SpringApplicationBuilder sibling(Class<?>... sources) {
		return runAndExtractParent().child(sources);
	}

	/**
	 * Create a sibling application (one with the same parent). A side effect of calling
	 * this method is that the current application (and its parent) are started if they
	 * are not already running.
	 * @param sources the sources for the application (Spring configuration)
	 * @param args the command line arguments to use when starting the current app and its
	 * parent
	 * @return the new sibling builder
	 */
	public SpringApplicationBuilder sibling(Class<?>[] sources, String... args) {
		return runAndExtractParent(args).child(sources);
	}

	/**
	 * Explicitly set the factory used to create the application context.
	 * @param factory the factory to use
	 * @return the current builder
	 * @since 2.4.0
	 */
	public SpringApplicationBuilder contextFactory(ApplicationContextFactory factory) {
		this.application.setApplicationContextFactory(factory);
		return this;
	}

	/**
	 * Add more sources (configuration classes and components) to this application.
	 * @param sources the sources to add
	 * @return the current builder
	 */
	public SpringApplicationBuilder sources(Class<?>... sources) {
		this.sources.addAll(new LinkedHashSet<>(Arrays.asList(sources)));
		return this;
	}

	/**
	 * Flag to explicitly request a specific type of web application. Auto-detected based
	 * on the classpath if not set.
	 * @param webApplicationType the type of web application
	 * @return the current builder
	 * @since 2.0.0
	 */
	public SpringApplicationBuilder web(WebApplicationType webApplicationType) {
		this.application.setWebApplicationType(webApplicationType);
		return this;
	}

	/**
	 * Flag to indicate the startup information should be logged.
	 * @param logStartupInfo the flag to set. Default true.
	 * @return the current builder
	 */
	public SpringApplicationBuilder logStartupInfo(boolean logStartupInfo) {
		this.application.setLogStartupInfo(logStartupInfo);
		return this;
	}

	/**
	 * Sets the {@link Banner} instance which will be used to print the banner when no
	 * static banner file is provided.
	 * @param banner the banner to use
	 * @return the current builder
	 */
	public SpringApplicationBuilder banner(Banner banner) {
		this.application.setBanner(banner);
		return this;
	}

	public SpringApplicationBuilder bannerMode(Banner.Mode bannerMode) {
		this.application.setBannerMode(bannerMode);
		return this;
	}

	/**
	 * Sets if the application is headless and should not instantiate AWT. Defaults to
	 * {@code true} to prevent java icons appearing.
	 * @param headless if the application is headless
	 * @return the current builder
	 */
	public SpringApplicationBuilder headless(boolean headless) {
		this.application.setHeadless(headless);
		return this;
	}

	/**
	 * Sets if the created {@link ApplicationContext} should have a shutdown hook
	 * registered.
	 * @param registerShutdownHook if the shutdown hook should be registered
	 * @return the current builder
	 */
	public SpringApplicationBuilder registerShutdownHook(boolean registerShutdownHook) {
		this.registerShutdownHookApplied = true;
		this.application.setRegisterShutdownHook(registerShutdownHook);
		return this;
	}

	/**
	 * Fixes the main application class that is used to anchor the startup messages.
	 * @param mainApplicationClass the class to use.
	 * @return the current builder
	 */
	public SpringApplicationBuilder main(Class<?> mainApplicationClass) {
		this.application.setMainApplicationClass(mainApplicationClass);
		return this;
	}

	/**
	 * Flag to indicate that command line arguments should be added to the environment.
	 * @param addCommandLineProperties the flag to set. Default true.
	 * @return the current builder
	 */
	public SpringApplicationBuilder addCommandLineProperties(boolean addCommandLineProperties) {
		this.application.setAddCommandLineProperties(addCommandLineProperties);
		return this;
	}

	/**
	 * Flag to indicate if the {@link ApplicationConversionService} should be added to the
	 * application context's {@link Environment}.
	 * @param addConversionService if the conversion service should be added.
	 * @return the current builder
	 * @since 2.1.0
	 */
	public SpringApplicationBuilder setAddConversionService(boolean addConversionService) {
		this.application.setAddConversionService(addConversionService);
		return this;
	}

	/**
	 * Adds {@link BootstrapRegistryInitializer} instances that can be used to initialize
	 * the {@link BootstrapRegistry}.
	 * @param bootstrapRegistryInitializer the bootstrap registry initializer to add
	 * @return the current builder
	 * @since 2.4.5
	 */
	public SpringApplicationBuilder addBootstrapRegistryInitializer(
			BootstrapRegistryInitializer bootstrapRegistryInitializer) {
		this.application.addBootstrapRegistryInitializer(bootstrapRegistryInitializer);
		return this;
	}

	/**
	 * Flag to control whether the application should be initialized lazily.
	 * @param lazyInitialization the flag to set. Defaults to false.
	 * @return the current builder
	 * @since 2.2
	 */
	public SpringApplicationBuilder lazyInitialization(boolean lazyInitialization) {
		this.application.setLazyInitialization(lazyInitialization);
		return this;
	}

	/**
	 * Default properties for the environment in the form {@code key=value} or
	 * {@code key:value}. Multiple calls to this method are cumulative and will not clear
	 * any previously set properties.
	 * @param defaultProperties the properties to set.
	 * @return the current builder
	 * @see SpringApplicationBuilder#properties(Properties)
	 * @see SpringApplicationBuilder#properties(Map)
	 */
	public SpringApplicationBuilder properties(String... defaultProperties) {
		return properties(getMapFromKeyValuePairs(defaultProperties));
	}

	private Map<String, Object> getMapFromKeyValuePairs(String[] properties) {
		Map<String, Object> map = new HashMap<>();
		for (String property : properties) {
			int index = lowestIndexOf(property, ":", "=");
			String key = (index > 0) ? property.substring(0, index) : property;
			String value = (index > 0) ? property.substring(index + 1) : "";
			map.put(key, value);
		}
		return map;
	}

	private int lowestIndexOf(String property, String... candidates) {
		int index = -1;
		for (String candidate : candidates) {
			int candidateIndex = property.indexOf(candidate);
			if (candidateIndex > 0) {
				index = (index != -1) ? Math.min(index, candidateIndex) : candidateIndex;
			}
		}
		return index;
	}

	/**
	 * Default properties for the environment.Multiple calls to this method are cumulative
	 * and will not clear any previously set properties.
	 * @param defaultProperties the properties to set.
	 * @return the current builder
	 * @see SpringApplicationBuilder#properties(String...)
	 * @see SpringApplicationBuilder#properties(Map)
	 */
	public SpringApplicationBuilder properties(Properties defaultProperties) {
		return properties(getMapFromProperties(defaultProperties));
	}

	private Map<String, Object> getMapFromProperties(Properties properties) {
		Map<String, Object> map = new HashMap<>();
		for (Object key : Collections.list(properties.propertyNames())) {
			map.put((String) key, properties.get(key));
		}
		return map;
	}

	/**
	 * Default properties for the environment. Multiple calls to this method are
	 * cumulative and will not clear any previously set properties.
	 * @param defaults the default properties
	 * @return the current builder
	 * @see SpringApplicationBuilder#properties(String...)
	 * @see SpringApplicationBuilder#properties(Properties)
	 */
	public SpringApplicationBuilder properties(Map<String, Object> defaults) {
		this.defaultProperties.putAll(defaults);
		this.application.setDefaultProperties(this.defaultProperties);
		if (this.parent != null) {
			this.parent.properties(this.defaultProperties);
			this.parent.environment(this.environment);
		}
		return this;
	}

	/**
	 * Add to the active Spring profiles for this app (and its parent and children).
	 * @param profiles the profiles to add.
	 * @return the current builder
	 */
	public SpringApplicationBuilder profiles(String... profiles) {
		this.additionalProfiles.addAll(Arrays.asList(profiles));
		this.application.setAdditionalProfiles(StringUtils.toStringArray(this.additionalProfiles));
		return this;
	}

	private SpringApplicationBuilder additionalProfiles(Collection<String> additionalProfiles) {
		this.additionalProfiles = new LinkedHashSet<>(additionalProfiles);
		this.application.setAdditionalProfiles(StringUtils.toStringArray(this.additionalProfiles));
		return this;
	}

	/**
	 * Bean name generator for automatically generated bean names in the application
	 * context.
	 * @param beanNameGenerator the generator to set.
	 * @return the current builder
	 */
	public SpringApplicationBuilder beanNameGenerator(BeanNameGenerator beanNameGenerator) {
		this.application.setBeanNameGenerator(beanNameGenerator);
		return this;
	}

	/**
	 * Environment for the application context.
	 * @param environment the environment to set.
	 * @return the current builder
	 */
	public SpringApplicationBuilder environment(ConfigurableEnvironment environment) {
		this.application.setEnvironment(environment);
		this.environment = environment;
		return this;
	}

	/**
	 * Prefix that should be applied when obtaining configuration properties from the
	 * system environment.
	 * @param environmentPrefix the environment property prefix to set
	 * @return the current builder
	 * @since 2.5.0
	 */
	public SpringApplicationBuilder environmentPrefix(String environmentPrefix) {
		this.application.setEnvironmentPrefix(environmentPrefix);
		return this;
	}

	/**
	 * {@link ResourceLoader} for the application context. If a custom class loader is
	 * needed, this is where it would be added.
	 * @param resourceLoader the resource loader to set.
	 * @return the current builder
	 */
	public SpringApplicationBuilder resourceLoader(ResourceLoader resourceLoader) {
		this.application.setResourceLoader(resourceLoader);
		return this;
	}

	/**
	 * Add some initializers to the application (applied to the {@link ApplicationContext}
	 * before any bean definitions are loaded).
	 * @param initializers some initializers to add
	 * @return the current builder
	 */
	public SpringApplicationBuilder initializers(ApplicationContextInitializer<?>... initializers) {
		this.application.addInitializers(initializers);
		return this;
	}

	/**
	 * Add some listeners to the application (listening for SpringApplication events as
	 * well as regular Spring events once the context is running). Any listeners that are
	 * also {@link ApplicationContextInitializer} will be added to the
	 * {@link #initializers(ApplicationContextInitializer...) initializers} automatically.
	 * @param listeners some listeners to add
	 * @return the current builder
	 */
	public SpringApplicationBuilder listeners(ApplicationListener<?>... listeners) {
		this.application.addListeners(listeners);
		return this;
	}

	/**
	 * Configure the {@link ApplicationStartup} to be used with the
	 * {@link ApplicationContext} for collecting startup metrics.
	 * @param applicationStartup the application startup to use
	 * @return the current builder
	 * @since 2.4.0
	 */
	public SpringApplicationBuilder applicationStartup(ApplicationStartup applicationStartup) {
		this.application.setApplicationStartup(applicationStartup);
		return this;
	}

	/**
	 * Whether to allow circular references between beans and automatically try to resolve
	 * them.
	 * @param allowCircularReferences whether circular references are allowed
	 * @return the current builder
	 * @since 2.6.0
	 * @see AbstractAutowireCapableBeanFactory#setAllowCircularReferences(boolean)
	 */
	public SpringApplicationBuilder allowCircularReferences(boolean allowCircularReferences) {
		this.application.setAllowCircularReferences(allowCircularReferences);
		return this;
	}

}
