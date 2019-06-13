/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.web.reactive.context;

import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.ScopeMetadataResolver;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * {@link ConfigurableReactiveWebApplicationContext} that accepts annotated classes as
 * input - in particular {@link Configuration @Configuration}-annotated classes, but also
 * plain {@link Component @Component} classes and JSR-330 compliant classes using
 * {@code javax.inject} annotations. Allows for registering classes one by one (specifying
 * class names as config location) as well as for classpath scanning (specifying base
 * packages as config location).
 * <p>
 * Note: In case of multiple {@code @Configuration} classes, later {@code @Bean}
 * definitions will override ones defined in earlier loaded files. This can be leveraged
 * to deliberately override certain bean definitions via an extra Configuration class.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 2.0.0
 * @see AnnotationConfigApplicationContext
 */
public class AnnotationConfigReactiveWebApplicationContext extends AnnotationConfigApplicationContext
		implements ConfigurableReactiveWebApplicationContext {

	/**
	 * Create a new AnnotationConfigReactiveWebApplicationContext that needs to be
	 * populated through {@link #register} calls and then manually {@linkplain #refresh
	 * refreshed}.
	 */
	public AnnotationConfigReactiveWebApplicationContext() {
	}

	/**
	 * Create a new AnnotationConfigApplicationContext with the given
	 * DefaultListableBeanFactory.
	 * @param beanFactory the DefaultListableBeanFactory instance to use for this context
	 * @since 2.2.0
	 */
	public AnnotationConfigReactiveWebApplicationContext(DefaultListableBeanFactory beanFactory) {
		super(beanFactory);
	}

	/**
	 * Create a new AnnotationConfigApplicationContext, deriving bean definitions from the
	 * given annotated classes and automatically refreshing the context.
	 * @param annotatedClasses one or more annotated classes, e.g.
	 * {@link Configuration @Configuration} classes
	 * @since 2.2.0
	 */
	public AnnotationConfigReactiveWebApplicationContext(Class<?>... annotatedClasses) {
		super(annotatedClasses);
	}

	/**
	 * Create a new AnnotationConfigApplicationContext, scanning for bean definitions in
	 * the given packages and automatically refreshing the context.
	 * @param basePackages the packages to check for annotated classes
	 * @since 2.2.0
	 */
	public AnnotationConfigReactiveWebApplicationContext(String... basePackages) {
		super(basePackages);
	}

	@Override
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardReactiveWebEnvironment();
	}

	@Override
	protected Resource getResourceByPath(String path) {
		// We must be careful not to expose classpath resources
		return new FilteredReactiveWebContextResource(path);
	}

	/**
	 * Return the custom {@link BeanNameGenerator} for use with
	 * {@link AnnotatedBeanDefinitionReader} and/or
	 * {@link ClassPathBeanDefinitionScanner}, if any.
	 * @return the bean name generator
	 * @deprecated since 2.2.0 since this class no longer extends
	 * {@code AbstractRefreshableConfigApplicationContext}
	 */
	@Deprecated
	protected final BeanNameGenerator getBeanNameGenerator() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Return the custom {@link ScopeMetadataResolver} for use with
	 * {@link AnnotatedBeanDefinitionReader} and/or
	 * {@link ClassPathBeanDefinitionScanner}, if any.
	 * @return the scope metadata resolver
	 * @deprecated since 2.2.0 since this class no longer extends
	 * {@code AbstractRefreshableConfigApplicationContext}
	 */
	@Deprecated
	protected ScopeMetadataResolver getScopeMetadataResolver() {
		throw new UnsupportedOperationException();
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
	 * @deprecated since 2.2.0 since this class no longer extends
	 * {@code AbstractRefreshableConfigApplicationContext}
	 */
	@Deprecated
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) {
		throw new UnsupportedOperationException();
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
	 * @deprecated since 2.2.0 since this class no longer extends
	 * {@code AbstractRefreshableConfigApplicationContext}
	 */
	@Deprecated
	protected AnnotatedBeanDefinitionReader getAnnotatedBeanDefinitionReader(DefaultListableBeanFactory beanFactory) {
		throw new UnsupportedOperationException();
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
	 * @deprecated since 2.2.0 since this class no longer extends
	 * {@code AbstractRefreshableConfigApplicationContext}
	 */
	@Deprecated
	protected ClassPathBeanDefinitionScanner getClassPathBeanDefinitionScanner(DefaultListableBeanFactory beanFactory) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Set the config locations for this application context in init-param style, i.e.
	 * with distinct locations separated by commas, semicolons or whitespace.
	 * <p>
	 * If not set, the implementation may use a default as appropriate.
	 * @param location the config location
	 * @deprecated since 2.2.0 since this class no longer extends
	 * {@code AbstractRefreshableConfigApplicationContext}. Use {@link ImportResource}
	 * instead.
	 */
	@Deprecated
	public void setConfigLocation(String location) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Set the config locations for this application context.
	 * <p>
	 * If not set, the implementation may use a default as appropriate.
	 * @param locations the config locations
	 * @deprecated since 2.2.0 since this class no longer extends
	 * {@code AbstractRefreshableConfigApplicationContext}. Use {@link ImportResource}
	 * instead.
	 */
	@Deprecated
	public void setConfigLocations(@Nullable String... locations) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Return an array of resource locations, referring to the XML bean definition files
	 * that this context should be built with. Can also include location patterns, which
	 * will get resolved via a ResourcePatternResolver.
	 * <p>
	 * The default implementation returns {@code null}. Subclasses can override this to
	 * provide a set of resource locations to load bean definitions from.
	 * @return an array of resource locations, or {@code null} if none
	 * @see #getResources
	 * @see #getResourcePatternResolver
	 * @deprecated since 2.2.0 since this class no longer extends
	 * {@code AbstractRefreshableConfigApplicationContext}.
	 */
	@Deprecated
	protected String[] getConfigLocations() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Return the default config locations to use, for the case where no explicit config
	 * locations have been specified.
	 * <p>
	 * The default implementation returns {@code null}, requiring explicit config
	 * locations.
	 * @return an array of default config locations, if any
	 * @see #setConfigLocations
	 * @deprecated since 2.2.0 since this class no longer extends
	 * {@code AbstractRefreshableConfigApplicationContext}.
	 */
	@Deprecated
	protected String[] getDefaultConfigLocations() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Resolve the given path, replacing placeholders with corresponding environment
	 * property values if necessary. Applied to config locations.
	 * @param path the original file path
	 * @return the resolved file path
	 * @see org.springframework.core.env.Environment#resolveRequiredPlaceholders(String)
	 * @deprecated since 2.2.0 since this class no longer extends
	 * {@code AbstractRefreshableConfigApplicationContext}.
	 */
	@Deprecated
	protected String resolvePath(String path) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Determine whether this context currently holds a bean factory, i.e. has been
	 * refreshed at least once and not been closed yet.
	 * @return {@code true} if the context holds a bean factory
	 * @deprecated since 2.2.0 since this class no longer extends
	 * {@code AbstractRefreshableConfigApplicationContext}.
	 */
	@Deprecated
	protected final boolean hasBeanFactory() {
		return true;
	}

	/**
	 * Create an internal bean factory for this context. Called for each
	 * {@link #refresh()} attempt.
	 * <p>
	 * The default implementation creates a
	 * {@link org.springframework.beans.factory.support.DefaultListableBeanFactory} with
	 * the {@linkplain #getInternalParentBeanFactory() internal bean factory} of this
	 * context's parent as parent bean factory. Can be overridden in subclasses, for
	 * example to customize DefaultListableBeanFactory's settings.
	 * @return the bean factory for this context
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowEagerClassLoading
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowCircularReferences
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowRawInjectionDespiteWrapping
	 * @deprecated since 2.2.0 since this class no longer extends
	 * {@code AbstractRefreshableConfigApplicationContext}.
	 */
	@Deprecated
	protected DefaultListableBeanFactory createBeanFactory() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Customize the internal bean factory used by this context. Called for each
	 * {@link #refresh()} attempt.
	 * <p>
	 * The default implementation applies this context's
	 * {@linkplain #setAllowBeanDefinitionOverriding "allowBeanDefinitionOverriding"} and
	 * {@linkplain #setAllowCircularReferences "allowCircularReferences"} settings, if
	 * specified. Can be overridden in subclasses to customize any of
	 * {@link DefaultListableBeanFactory}'s settings.
	 * @param beanFactory the newly created bean factory for this context
	 * @see DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
	 * @see DefaultListableBeanFactory#setAllowCircularReferences
	 * @see DefaultListableBeanFactory#setAllowRawInjectionDespiteWrapping
	 * @see DefaultListableBeanFactory#setAllowEagerClassLoading
	 * @deprecated since 2.2.0 since this class no longer extends
	 * {@code AbstractRefreshableConfigApplicationContext}.
	 */
	@Deprecated
	protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
		throw new UnsupportedOperationException();
	}

}
