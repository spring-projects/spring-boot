/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.web.reactive.context;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigRegistry;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ScopeMetadataResolver;
import org.springframework.context.support.AbstractRefreshableConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ConfigurableReactiveWebApplicationContext} that accepts annotated classes as
 * input - in particular
 * {@link org.springframework.context.annotation.Configuration @Configuration}-annotated
 * classes, but also plain {@link Component @Component} classes and JSR-330 compliant
 * classes using {@code javax.inject} annotations. Allows for registering classes one by
 * one (specifying class names as config location) as well as for classpath scanning
 * (specifying base packages as config location).
 * <p>
 * Note: In case of multiple {@code @Configuration} classes, later {@code @Bean}
 * definitions will override ones defined in earlier loaded files. This can be leveraged
 * to deliberately override certain bean definitions via an extra Configuration class.
 *
 * @author Phillip Webb
 * @since 2.0.0
 * @see #register(Class...)
 * @see #scan(String...)
 */
public class AnnotationConfigReactiveWebApplicationContext
		extends AbstractRefreshableConfigApplicationContext
		implements ConfigurableReactiveWebApplicationContext, AnnotationConfigRegistry {

	private BeanNameGenerator beanNameGenerator;

	private ScopeMetadataResolver scopeMetadataResolver;

	private final Set<Class<?>> annotatedClasses = new LinkedHashSet<>();

	private final Set<String> basePackages = new LinkedHashSet<>();

	@Override
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardReactiveWebEnvironment();
	}

	/**
	 * Set a custom {@link BeanNameGenerator} for use with
	 * {@link AnnotatedBeanDefinitionReader} and/or
	 * {@link ClassPathBeanDefinitionScanner}.
	 * <p>
	 * Default is
	 * {@link org.springframework.context.annotation.AnnotationBeanNameGenerator}.
	 * @param beanNameGenerator the bean name generator
	 * @see AnnotatedBeanDefinitionReader#setBeanNameGenerator
	 * @see ClassPathBeanDefinitionScanner#setBeanNameGenerator
	 */
	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		this.beanNameGenerator = beanNameGenerator;
	}

	/**
	 * Return the custom {@link BeanNameGenerator} for use with
	 * {@link AnnotatedBeanDefinitionReader} and/or
	 * {@link ClassPathBeanDefinitionScanner}, if any.
	 * @return the bean name generator
	 */
	protected BeanNameGenerator getBeanNameGenerator() {
		return this.beanNameGenerator;
	}

	/**
	 * Set a custom {@link ScopeMetadataResolver} for use with
	 * {@link AnnotatedBeanDefinitionReader} and/or
	 * {@link ClassPathBeanDefinitionScanner}.
	 * <p>
	 * Default is an
	 * {@link org.springframework.context.annotation.AnnotationScopeMetadataResolver}.
	 * @param scopeMetadataResolver the scope metadata resolver
	 * @see AnnotatedBeanDefinitionReader#setScopeMetadataResolver
	 * @see ClassPathBeanDefinitionScanner#setScopeMetadataResolver
	 */
	public void setScopeMetadataResolver(ScopeMetadataResolver scopeMetadataResolver) {
		this.scopeMetadataResolver = scopeMetadataResolver;
	}

	/**
	 * Return the custom {@link ScopeMetadataResolver} for use with
	 * {@link AnnotatedBeanDefinitionReader} and/or
	 * {@link ClassPathBeanDefinitionScanner}, if any.
	 * @return the scope metadata resolver
	 */
	protected ScopeMetadataResolver getScopeMetadataResolver() {
		return this.scopeMetadataResolver;
	}

	/**
	 * Register one or more annotated classes to be processed.
	 * <p>
	 * Note that {@link #refresh()} must be called in order for the context to fully
	 * process the new classes.
	 * @param annotatedClasses one or more annotated classes, e.g.
	 * {@link org.springframework.context.annotation.Configuration @Configuration} classes
	 * @see #scan(String...)
	 * @see #loadBeanDefinitions(DefaultListableBeanFactory)
	 * @see #setConfigLocation(String)
	 * @see #refresh()
	 */
	@Override
	public void register(Class<?>... annotatedClasses) {
		Assert.notEmpty(annotatedClasses,
				"At least one annotated class must be specified");
		this.annotatedClasses.addAll(Arrays.asList(annotatedClasses));
	}

	/**
	 * Perform a scan within the specified base packages.
	 * <p>
	 * Note that {@link #refresh()} must be called in order for the context to fully
	 * process the new classes.
	 * @param basePackages the packages to check for annotated classes
	 * @see #loadBeanDefinitions(DefaultListableBeanFactory)
	 * @see #register(Class...)
	 * @see #setConfigLocation(String)
	 * @see #refresh()
	 */
	@Override
	public void scan(String... basePackages) {
		Assert.notEmpty(basePackages, "At least one base package must be specified");
		this.basePackages.addAll(Arrays.asList(basePackages));
	}

	/**
	 * Register a {@link org.springframework.beans.factory.config.BeanDefinition} for any
	 * classes specified by {@link #register(Class...)} and scan any packages specified by
	 * {@link #scan(String...)}.
	 * <p>
	 * For any values specified by {@link #setConfigLocation(String)} or
	 * {@link #setConfigLocations(String[])}, attempt first to load each location as a
	 * class, registering a {@code BeanDefinition} if class loading is successful, and if
	 * class loading fails (i.e. a {@code ClassNotFoundException} is raised), assume the
	 * value is a package and attempt to scan it for annotated classes.
	 * <p>
	 * Enables the default set of annotation configuration post processors, such that
	 * {@code @Autowired}, {@code @Required}, and associated annotations can be used.
	 * <p>
	 * Configuration class bean definitions are registered with generated bean definition
	 * names unless the {@code value} attribute is provided to the stereotype annotation.
	 * @param beanFactory the bean factory to load bean definitions into
	 * @see #register(Class...)
	 * @see #scan(String...)
	 * @see #setConfigLocation(String)
	 * @see #setConfigLocations(String[])
	 * @see AnnotatedBeanDefinitionReader
	 * @see ClassPathBeanDefinitionScanner
	 */
	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) {
		AnnotatedBeanDefinitionReader reader = getAnnotatedBeanDefinitionReader(
				beanFactory);
		ClassPathBeanDefinitionScanner scanner = getClassPathBeanDefinitionScanner(
				beanFactory);
		applyBeanNameGenerator(beanFactory, reader, scanner);
		applyScopeMetadataResolver(reader, scanner);
		loadBeanDefinitions(reader, scanner);
	}

	private void applyBeanNameGenerator(DefaultListableBeanFactory beanFactory,
			AnnotatedBeanDefinitionReader reader,
			ClassPathBeanDefinitionScanner scanner) {
		BeanNameGenerator beanNameGenerator = getBeanNameGenerator();
		if (beanNameGenerator != null) {
			reader.setBeanNameGenerator(beanNameGenerator);
			scanner.setBeanNameGenerator(beanNameGenerator);
			beanFactory.registerSingleton(
					AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR,
					beanNameGenerator);
		}
	}

	private void applyScopeMetadataResolver(AnnotatedBeanDefinitionReader reader,
			ClassPathBeanDefinitionScanner scanner) {
		ScopeMetadataResolver scopeMetadataResolver = getScopeMetadataResolver();
		if (scopeMetadataResolver != null) {
			reader.setScopeMetadataResolver(scopeMetadataResolver);
			scanner.setScopeMetadataResolver(scopeMetadataResolver);
		}
	}

	private void loadBeanDefinitions(AnnotatedBeanDefinitionReader reader,
			ClassPathBeanDefinitionScanner scanner) throws LinkageError {
		if (!this.annotatedClasses.isEmpty()) {
			registerAnnotatedClasses(reader);
		}
		if (!this.basePackages.isEmpty()) {
			scanBasePackages(scanner);
		}
		String[] configLocations = getConfigLocations();
		if (configLocations != null) {
			registerConfigLocations(reader, scanner, configLocations);
		}
	}

	private void registerAnnotatedClasses(AnnotatedBeanDefinitionReader reader) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info("Registering annotated classes: ["
					+ StringUtils.collectionToCommaDelimitedString(this.annotatedClasses)
					+ "]");
		}
		reader.register(ClassUtils.toClassArray(this.annotatedClasses));
	}

	private void scanBasePackages(ClassPathBeanDefinitionScanner scanner) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info("Scanning base packages: ["
					+ StringUtils.collectionToCommaDelimitedString(this.basePackages)
					+ "]");
		}
		scanner.scan(StringUtils.toStringArray(this.basePackages));
	}

	private void registerConfigLocations(AnnotatedBeanDefinitionReader reader,
			ClassPathBeanDefinitionScanner scanner, String[] configLocations)
			throws LinkageError {
		for (String configLocation : configLocations) {
			try {
				register(reader, configLocation);
			}
			catch (ClassNotFoundException ex) {
				if (this.logger.isDebugEnabled()) {
					this.logger.debug("Could not load class for config location ["
							+ configLocation + "] - trying package scan. " + ex);
				}
				int count = scanner.scan(configLocation);
				if (this.logger.isInfoEnabled()) {
					logScanResult(configLocation, count);
				}
			}
		}
	}

	private void register(AnnotatedBeanDefinitionReader reader, String configLocation)
			throws ClassNotFoundException, LinkageError {
		Class<?> clazz = ClassUtils.forName(configLocation, getClassLoader());
		if (this.logger.isInfoEnabled()) {
			this.logger.info("Successfully resolved class for [" + configLocation + "]");
		}
		reader.register(clazz);
	}

	private void logScanResult(String configLocation, int count) {
		if (count == 0) {
			this.logger.info("No annotated classes found for specified class/package ["
					+ configLocation + "]");
		}
		else {
			this.logger.info("Found " + count + " annotated classes in package ["
					+ configLocation + "]");
		}
	}

	/**
	 * Build an {@link AnnotatedBeanDefinitionReader} for the given bean factory.
	 * <p>
	 * This should be pre-configured with the {@code Environment} (if desired) but not
	 * with a {@code BeanNameGenerator} or {@code ScopeMetadataResolver} yet.
	 * @param beanFactory the bean factory to load bean definitions into
	 * @return the annotated bean definition reader
	 * @see #getEnvironment()
	 * @see #getBeanNameGenerator()
	 * @see #getScopeMetadataResolver()
	 */
	protected AnnotatedBeanDefinitionReader getAnnotatedBeanDefinitionReader(
			DefaultListableBeanFactory beanFactory) {
		return new AnnotatedBeanDefinitionReader(beanFactory, getEnvironment());
	}

	/**
	 * Build a {@link ClassPathBeanDefinitionScanner} for the given bean factory.
	 * <p>
	 * This should be pre-configured with the {@code Environment} (if desired) but not
	 * with a {@code BeanNameGenerator} or {@code ScopeMetadataResolver} yet.
	 * @param beanFactory the bean factory to load bean definitions into
	 * @return the class path bean definition scanner
	 * @see #getEnvironment()
	 * @see #getBeanNameGenerator()
	 * @see #getScopeMetadataResolver()
	 */
	protected ClassPathBeanDefinitionScanner getClassPathBeanDefinitionScanner(
			DefaultListableBeanFactory beanFactory) {
		return new ClassPathBeanDefinitionScanner(beanFactory, true, getEnvironment());
	}

	@Override
	protected Resource getResourceByPath(String path) {
		// We must be careful not to expose classpath resources
		return new FilteredReactiveWebContextResource(path);
	}

}
