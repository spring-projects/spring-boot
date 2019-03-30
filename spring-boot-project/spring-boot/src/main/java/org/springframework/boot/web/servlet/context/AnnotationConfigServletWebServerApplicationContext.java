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

package org.springframework.boot.web.servlet.context;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigRegistry;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.AnnotationScopeMetadataResolver;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ScopeMetadataResolver;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * {@link ServletWebServerApplicationContext} that accepts annotated classes as input - in
 * particular {@link org.springframework.context.annotation.Configuration @Configuration}
 * -annotated classes, but also plain {@link Component @Component} classes and JSR-330
 * compliant classes using {@code javax.inject} annotations. Allows for registering
 * classes one by one (specifying class names as config location) as well as for classpath
 * scanning (specifying base packages as config location).
 * <p>
 * Note: In case of multiple {@code @Configuration} classes, later {@code @Bean}
 * definitions will override ones defined in earlier loaded files. This can be leveraged
 * to deliberately override certain bean definitions via an extra Configuration class.
 *
 * @author Phillip Webb
 * @see #register(Class...)
 * @see #scan(String...)
 * @see ServletWebServerApplicationContext
 * @see AnnotationConfigWebApplicationContext
 */
public class AnnotationConfigServletWebServerApplicationContext
		extends ServletWebServerApplicationContext implements AnnotationConfigRegistry {

	private final Log logger = LogFactory.getLog(getClass());

	private final AnnotatedBeanDefinitionReader reader;

	private final ClassPathBeanDefinitionScanner scanner;

	private final Set<Class<?>> annotatedClasses = new LinkedHashSet<>();

	private String[] basePackages;

	private final Set<BeanRegistration> registeredBeans = new LinkedHashSet<>();

	/**
	 * Create a new {@link AnnotationConfigServletWebServerApplicationContext} that needs
	 * to be populated through {@link #register} calls and then manually
	 * {@linkplain #refresh refreshed}.
	 */
	public AnnotationConfigServletWebServerApplicationContext() {
		this.reader = new AnnotatedBeanDefinitionReader(this);
		this.scanner = new ClassPathBeanDefinitionScanner(this);
	}

	/**
	 * Create a new {@link AnnotationConfigServletWebServerApplicationContext} with the
	 * given {@code DefaultListableBeanFactory}. The context needs to be populated through
	 * {@link #register} calls and then manually {@linkplain #refresh refreshed}.
	 * @param beanFactory the DefaultListableBeanFactory instance to use for this context
	 */
	public AnnotationConfigServletWebServerApplicationContext(
			DefaultListableBeanFactory beanFactory) {
		super(beanFactory);
		this.reader = new AnnotatedBeanDefinitionReader(this);
		this.scanner = new ClassPathBeanDefinitionScanner(this);
	}

	/**
	 * Create a new {@link AnnotationConfigServletWebServerApplicationContext}, deriving
	 * bean definitions from the given annotated classes and automatically refreshing the
	 * context.
	 * @param annotatedClasses one or more annotated classes, e.g. {@code @Configuration}
	 * classes
	 */
	public AnnotationConfigServletWebServerApplicationContext(
			Class<?>... annotatedClasses) {
		this();
		register(annotatedClasses);
		refresh();
	}

	/**
	 * Create a new {@link AnnotationConfigServletWebServerApplicationContext}, scanning
	 * for bean definitions in the given packages and automatically refreshing the
	 * context.
	 * @param basePackages the packages to check for annotated classes
	 */
	public AnnotationConfigServletWebServerApplicationContext(String... basePackages) {
		this();
		scan(basePackages);
		refresh();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Delegates given environment to underlying {@link AnnotatedBeanDefinitionReader} and
	 * {@link ClassPathBeanDefinitionScanner} members.
	 */
	@Override
	public void setEnvironment(ConfigurableEnvironment environment) {
		super.setEnvironment(environment);
		this.reader.setEnvironment(environment);
		this.scanner.setEnvironment(environment);
	}

	/**
	 * Provide a custom {@link BeanNameGenerator} for use with
	 * {@link AnnotatedBeanDefinitionReader} and/or
	 * {@link ClassPathBeanDefinitionScanner}, if any.
	 * <p>
	 * Default is
	 * {@link org.springframework.context.annotation.AnnotationBeanNameGenerator}.
	 * <p>
	 * Any call to this method must occur prior to calls to {@link #register(Class...)}
	 * and/or {@link #scan(String...)}.
	 * @param beanNameGenerator the bean name generator
	 * @see AnnotatedBeanDefinitionReader#setBeanNameGenerator
	 * @see ClassPathBeanDefinitionScanner#setBeanNameGenerator
	 */
	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		this.reader.setBeanNameGenerator(beanNameGenerator);
		this.scanner.setBeanNameGenerator(beanNameGenerator);
		this.getBeanFactory().registerSingleton(
				AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR,
				beanNameGenerator);
	}

	/**
	 * Set the {@link ScopeMetadataResolver} to use for detected bean classes.
	 * <p>
	 * The default is an {@link AnnotationScopeMetadataResolver}.
	 * <p>
	 * Any call to this method must occur prior to calls to {@link #register(Class...)}
	 * and/or {@link #scan(String...)}.
	 * @param scopeMetadataResolver the scope metadata resolver
	 */
	public void setScopeMetadataResolver(ScopeMetadataResolver scopeMetadataResolver) {
		this.reader.setScopeMetadataResolver(scopeMetadataResolver);
		this.scanner.setScopeMetadataResolver(scopeMetadataResolver);
	}

	/**
	 * Register one or more annotated classes to be processed. Note that
	 * {@link #refresh()} must be called in order for the context to fully process the new
	 * class.
	 * <p>
	 * Calls to {@code #register} are idempotent; adding the same annotated class more
	 * than once has no additional effect.
	 * @param annotatedClasses one or more annotated classes, e.g. {@code @Configuration}
	 * classes
	 * @see #scan(String...)
	 * @see #refresh()
	 */
	@Override
	public final void register(Class<?>... annotatedClasses) {
		Assert.notEmpty(annotatedClasses,
				"At least one annotated class must be specified");
		this.annotatedClasses.addAll(Arrays.asList(annotatedClasses));
	}

	/**
	 * Perform a scan within the specified base packages. Note that {@link #refresh()}
	 * must be called in order for the context to fully process the new class.
	 * @param basePackages the packages to check for annotated classes
	 * @see #register(Class...)
	 * @see #refresh()
	 */
	@Override
	public final void scan(String... basePackages) {
		Assert.notEmpty(basePackages, "At least one base package must be specified");
		this.basePackages = basePackages;
	}

	/**
	 * Register a bean from the given bean class.
	 * @param annotatedClass the class of the bean
	 * @param <T> the type of the bean
	 * @since 2.2.0
	 */
	public final <T> void registerBean(Class<T> annotatedClass) {
		this.registeredBeans.add(new BeanRegistration(annotatedClass, null, null));
	}

	/**
	 * Register a bean from the given bean class, using the given supplier for obtaining a
	 * new instance (typically declared as a lambda expression or method reference).
	 * @param annotatedClass the class of the bean
	 * @param supplier a callback for creating an instance of the bean
	 * @param <T> the type of the bean
	 * @since 2.2.0
	 */
	public final <T> void registerBean(Class<T> annotatedClass, Supplier<T> supplier) {
		this.registeredBeans.add(new BeanRegistration(annotatedClass, supplier, null));
	}

	@Override
	@SafeVarargs
	@SuppressWarnings("varargs")
	public final <T> void registerBean(Class<T> annotatedClass,
			Class<? extends Annotation>... qualifiers) {
		this.registeredBeans.add(new BeanRegistration(annotatedClass, null, qualifiers));
	}

	@Override
	@SafeVarargs
	@SuppressWarnings("varargs")
	public final <T> void registerBean(Class<T> annotatedClass, Supplier<T> supplier,
			Class<? extends Annotation>... qualifiers) {
		this.registeredBeans
				.add(new BeanRegistration(annotatedClass, supplier, qualifiers));
	}

	@Override
	protected void prepareRefresh() {
		this.scanner.clearCache();
		super.prepareRefresh();
	}

	@Override
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		super.postProcessBeanFactory(beanFactory);
		if (this.basePackages != null && this.basePackages.length > 0) {
			this.scanner.scan(this.basePackages);
		}
		if (!this.annotatedClasses.isEmpty()) {
			this.reader.register(ClassUtils.toClassArray(this.annotatedClasses));
		}
		if (!this.registeredBeans.isEmpty()) {
			registerBeans(this.reader);
		}
	}

	private void registerBeans(AnnotatedBeanDefinitionReader reader) {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("Registering supplied beans: ["
					+ StringUtils.collectionToCommaDelimitedString(this.registeredBeans)
					+ "]");
		}
		this.registeredBeans.forEach((reg) -> reader.registerBean(reg.getAnnotatedClass(),
				reg.getSupplier(), reg.getQualifiers()));
	}

	/**
	 * Holder for a programmatic bean registration.
	 *
	 * @see #registerBean(Class, Class[])
	 * @see #registerBean(Class, Supplier, Class[])
	 */
	private static class BeanRegistration {

		private final Class<?> annotatedClass;

		private final Supplier<?> supplier;

		private final Class<? extends Annotation>[] qualifiers;

		BeanRegistration(Class<?> annotatedClass, Supplier<?> supplier,
				Class<? extends Annotation>[] qualifiers) {
			this.annotatedClass = annotatedClass;
			this.supplier = supplier;
			this.qualifiers = qualifiers;
		}

		public Class<?> getAnnotatedClass() {
			return this.annotatedClass;
		}

		@SuppressWarnings("rawtypes")
		public Supplier getSupplier() {
			return this.supplier;
		}

		public Class<? extends Annotation>[] getQualifiers() {
			return this.qualifiers;
		}

		@Override
		public String toString() {
			return this.annotatedClass.getName();
		}

	}

}
