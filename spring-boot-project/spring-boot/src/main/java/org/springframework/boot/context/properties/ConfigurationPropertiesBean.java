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

package org.springframework.boot.context.properties;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.bind.BindConstructorProvider;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

/**
 * Provides access to {@link ConfigurationProperties @ConfigurationProperties} bean
 * details, regardless of if the annotation was used directly or on a {@link Bean @Bean}
 * factory method. This class can be used to access {@link #getAll(ApplicationContext)
 * all} configuration properties beans in an ApplicationContext, or
 * {@link #get(ApplicationContext, Object, String) individual beans} on a case-by-case
 * basis (for example, in a {@link BeanPostProcessor}).
 *
 * @author Phillip Webb
 * @since 2.2.0
 * @see #getAll(ApplicationContext)
 * @see #get(ApplicationContext, Object, String)
 */
public final class ConfigurationPropertiesBean {

	private static final org.springframework.boot.context.properties.bind.BindMethod JAVA_BEAN_BIND_METHOD = //
			org.springframework.boot.context.properties.bind.BindMethod.JAVA_BEAN;

	private static final org.springframework.boot.context.properties.bind.BindMethod VALUE_OBJECT_BIND_METHOD = //
			org.springframework.boot.context.properties.bind.BindMethod.VALUE_OBJECT;

	private final String name;

	private final Object instance;

	private final Bindable<?> bindTarget;

	/**
     * Constructs a new ConfigurationPropertiesBean with the specified name, instance, and bind target.
     * 
     * @param name the name of the configuration property
     * @param instance the instance of the configuration property
     * @param bindTarget the bind target for the configuration property
     */
    private ConfigurationPropertiesBean(String name, Object instance, Bindable<?> bindTarget) {
		this.name = name;
		this.instance = instance;
		this.bindTarget = bindTarget;
	}

	/**
	 * Return the name of the Spring bean.
	 * @return the bean name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Return the actual Spring bean instance.
	 * @return the bean instance
	 */
	public Object getInstance() {
		return this.instance;
	}

	/**
	 * Return the bean type.
	 * @return the bean type
	 */
	Class<?> getType() {
		return this.bindTarget.getType().resolve();
	}

	/**
	 * Return the {@link ConfigurationProperties} annotation for the bean. The annotation
	 * may be defined on the bean itself or from the factory method that create the bean
	 * (usually a {@link Bean @Bean} method).
	 * @return the configuration properties annotation
	 */
	public ConfigurationProperties getAnnotation() {
		return this.bindTarget.getAnnotation(ConfigurationProperties.class);
	}

	/**
	 * Return a {@link Bindable} instance suitable that can be used as a target for the
	 * {@link Binder}.
	 * @return a bind target for use with the {@link Binder}
	 */
	public Bindable<?> asBindTarget() {
		return this.bindTarget;
	}

	/**
	 * Return all {@link ConfigurationProperties @ConfigurationProperties} beans contained
	 * in the given application context. Both directly annotated beans, as well as beans
	 * that have {@link ConfigurationProperties @ConfigurationProperties} annotated
	 * factory methods are included.
	 * @param applicationContext the source application context
	 * @return a map of all configuration properties beans keyed by the bean name
	 */
	public static Map<String, ConfigurationPropertiesBean> getAll(ApplicationContext applicationContext) {
		Assert.notNull(applicationContext, "ApplicationContext must not be null");
		if (applicationContext instanceof ConfigurableApplicationContext configurableContext) {
			return getAll(configurableContext);
		}
		Map<String, ConfigurationPropertiesBean> propertiesBeans = new LinkedHashMap<>();
		applicationContext.getBeansWithAnnotation(ConfigurationProperties.class).forEach((name, instance) -> {
			ConfigurationPropertiesBean propertiesBean = get(applicationContext, instance, name);
			if (propertiesBean != null) {
				propertiesBeans.put(name, propertiesBean);
			}
		});
		return propertiesBeans;
	}

	/**
     * Retrieves all ConfigurationPropertiesBean instances from the given ConfigurableApplicationContext.
     * 
     * @param applicationContext The ConfigurableApplicationContext from which to retrieve the ConfigurationPropertiesBean instances.
     * @return A Map containing the ConfigurationPropertiesBean instances, with the bean name as the key and the ConfigurationPropertiesBean as the value.
     */
    private static Map<String, ConfigurationPropertiesBean> getAll(ConfigurableApplicationContext applicationContext) {
		Map<String, ConfigurationPropertiesBean> propertiesBeans = new LinkedHashMap<>();
		ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
		Iterator<String> beanNames = beanFactory.getBeanNamesIterator();
		while (beanNames.hasNext()) {
			String beanName = beanNames.next();
			if (isConfigurationPropertiesBean(beanFactory, beanName)) {
				try {
					Object bean = beanFactory.getBean(beanName);
					ConfigurationPropertiesBean propertiesBean = get(applicationContext, bean, beanName);
					if (propertiesBean != null) {
						propertiesBeans.put(beanName, propertiesBean);
					}
				}
				catch (Exception ex) {
					// Ignore
				}
			}
		}
		return propertiesBeans;
	}

	/**
     * Checks if the specified bean is a ConfigurationProperties bean.
     * 
     * @param beanFactory the ConfigurableListableBeanFactory to use
     * @param beanName the name of the bean to check
     * @return true if the bean is a ConfigurationProperties bean, false otherwise
     */
    private static boolean isConfigurationPropertiesBean(ConfigurableListableBeanFactory beanFactory, String beanName) {
		try {
			if (beanFactory.getBeanDefinition(beanName).isAbstract()) {
				return false;
			}
			if (beanFactory.findAnnotationOnBean(beanName, ConfigurationProperties.class) != null) {
				return true;
			}
			Method factoryMethod = findFactoryMethod(beanFactory, beanName);
			return findMergedAnnotation(factoryMethod, ConfigurationProperties.class).isPresent();
		}
		catch (NoSuchBeanDefinitionException ex) {
			return false;
		}
	}

	/**
	 * Return a {@link ConfigurationPropertiesBean @ConfigurationPropertiesBean} instance
	 * for the given bean details or {@code null} if the bean is not a
	 * {@link ConfigurationProperties @ConfigurationProperties} object. Annotations are
	 * considered both on the bean itself, as well as any factory method (for example a
	 * {@link Bean @Bean} method).
	 * @param applicationContext the source application context
	 * @param bean the bean to consider
	 * @param beanName the bean name
	 * @return a configuration properties bean or {@code null} if the neither the bean nor
	 * factory method are annotated with
	 * {@link ConfigurationProperties @ConfigurationProperties}
	 */
	public static ConfigurationPropertiesBean get(ApplicationContext applicationContext, Object bean, String beanName) {
		Method factoryMethod = findFactoryMethod(applicationContext, beanName);
		Bindable<Object> bindTarget = createBindTarget(bean, bean.getClass(), factoryMethod);
		if (bindTarget == null) {
			return null;
		}
		bindTarget = bindTarget.withBindMethod(BindMethodAttribute.get(applicationContext, beanName));
		if (bindTarget.getBindMethod() == null && factoryMethod != null) {
			bindTarget = bindTarget.withBindMethod(JAVA_BEAN_BIND_METHOD);
		}
		if (bindTarget.getBindMethod() != VALUE_OBJECT_BIND_METHOD) {
			bindTarget = bindTarget.withExistingValue(bean);
		}
		return create(beanName, bean, bindTarget);
	}

	/**
     * Finds the factory method for the specified bean name in the given application context.
     * 
     * @param applicationContext The application context to search for the factory method.
     * @param beanName The name of the bean to find the factory method for.
     * @return The factory method for the specified bean name, or null if not found.
     */
    private static Method findFactoryMethod(ApplicationContext applicationContext, String beanName) {
		if (applicationContext instanceof ConfigurableApplicationContext configurableContext) {
			return findFactoryMethod(configurableContext, beanName);
		}
		return null;
	}

	/**
     * Finds the factory method for the specified bean name in the given application context.
     * 
     * @param applicationContext the configurable application context
     * @param beanName the name of the bean
     * @return the factory method for the specified bean name
     */
    private static Method findFactoryMethod(ConfigurableApplicationContext applicationContext, String beanName) {
		return findFactoryMethod(applicationContext.getBeanFactory(), beanName);
	}

	/**
     * Finds the factory method for the specified bean in the given bean factory.
     * 
     * @param beanFactory the configurable listable bean factory
     * @param beanName the name of the bean
     * @return the factory method for the bean, or null if not found
     */
    private static Method findFactoryMethod(ConfigurableListableBeanFactory beanFactory, String beanName) {
		if (beanFactory.containsBeanDefinition(beanName)) {
			BeanDefinition beanDefinition = beanFactory.getMergedBeanDefinition(beanName);
			if (beanDefinition instanceof RootBeanDefinition rootBeanDefinition) {
				return rootBeanDefinition.getResolvedFactoryMethod();
			}
		}
		return null;
	}

	/**
     * Creates a {@link ConfigurationPropertiesBean} for a value object.
     * 
     * @param beanType the type of the value object
     * @param beanName the name of the bean
     * @return the created {@link ConfigurationPropertiesBean}
     * @throws IllegalStateException if the bean is not a {@code @ConfigurationProperties} value object
     */
    static ConfigurationPropertiesBean forValueObject(Class<?> beanType, String beanName) {
		Bindable<Object> bindTarget = createBindTarget(null, beanType, null);
		Assert.state(bindTarget != null && deduceBindMethod(bindTarget) == VALUE_OBJECT_BIND_METHOD,
				() -> "Bean '" + beanName + "' is not a @ConfigurationProperties value object");
		return create(beanName, null, bindTarget.withBindMethod(VALUE_OBJECT_BIND_METHOD));
	}

	/**
     * Creates a bind target for the given bean, bean type, and factory method.
     * 
     * @param bean the bean object
     * @param beanType the class of the bean
     * @param factoryMethod the factory method used to create the bean (optional)
     * @return the bind target for the bean, or null if no annotations are found
     */
    private static Bindable<Object> createBindTarget(Object bean, Class<?> beanType, Method factoryMethod) {
		ResolvableType type = (factoryMethod != null) ? ResolvableType.forMethodReturnType(factoryMethod)
				: ResolvableType.forClass(beanType);
		Annotation[] annotations = findAnnotations(bean, beanType, factoryMethod);
		return (annotations != null) ? Bindable.of(type).withAnnotations(annotations) : null;
	}

	/**
     * Finds and returns an array of annotations for the given instance, type, and factory method.
     * 
     * @param instance the instance object
     * @param type the class type
     * @param factory the factory method
     * @return an array of annotations found, or null if no annotations are found
     */
    private static Annotation[] findAnnotations(Object instance, Class<?> type, Method factory) {
		ConfigurationProperties annotation = findAnnotation(instance, type, factory, ConfigurationProperties.class);
		if (annotation == null) {
			return null;
		}
		Validated validated = findAnnotation(instance, type, factory, Validated.class);
		return (validated != null) ? new Annotation[] { annotation, validated } : new Annotation[] { annotation };
	}

	/**
     * Finds the specified annotation on the given instance, type, or factory method.
     * 
     * @param instance        the instance to search for the annotation on
     * @param type            the type to search for the annotation on
     * @param factory         the factory method to search for the annotation on
     * @param annotationType  the type of annotation to search for
     * @param <A>             the type of the annotation
     * @return                the found annotation, or null if not found
     */
    private static <A extends Annotation> A findAnnotation(Object instance, Class<?> type, Method factory,
			Class<A> annotationType) {
		MergedAnnotation<A> annotation = MergedAnnotation.missing();
		if (factory != null) {
			annotation = findMergedAnnotation(factory, annotationType);
		}
		if (!annotation.isPresent()) {
			annotation = findMergedAnnotation(type, annotationType);
		}
		if (!annotation.isPresent() && AopUtils.isAopProxy(instance)) {
			annotation = MergedAnnotations.from(AopUtils.getTargetClass(instance), SearchStrategy.TYPE_HIERARCHY)
				.get(annotationType);
		}
		return annotation.isPresent() ? annotation.synthesize() : null;
	}

	/**
     * Finds the merged annotation of the specified type on the given annotated element.
     * 
     * @param element the annotated element to search for the annotation on
     * @param annotationType the type of the annotation to find
     * @param <A> the type of the annotation
     * @return the merged annotation of the specified type, or a missing merged annotation if not found
     */
    private static <A extends Annotation> MergedAnnotation<A> findMergedAnnotation(AnnotatedElement element,
			Class<A> annotationType) {
		return (element != null) ? MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY).get(annotationType)
				: MergedAnnotation.missing();
	}

	/**
     * Creates a new ConfigurationPropertiesBean with the specified name, instance, and bindTarget.
     * 
     * @param name the name of the ConfigurationPropertiesBean
     * @param instance the instance of the ConfigurationPropertiesBean
     * @param bindTarget the bind target of the ConfigurationPropertiesBean
     * @return a new ConfigurationPropertiesBean if bindTarget is not null, otherwise null
     */
    private static ConfigurationPropertiesBean create(String name, Object instance, Bindable<Object> bindTarget) {
		return (bindTarget != null) ? new ConfigurationPropertiesBean(name, instance, bindTarget) : null;
	}

	/**
	 * Deduce the {@code BindMethod} that should be used for the given type.
	 * @param type the source type
	 * @return the bind method to use
	 */
	static org.springframework.boot.context.properties.bind.BindMethod deduceBindMethod(Class<?> type) {
		return deduceBindMethod(BindConstructorProvider.DEFAULT.getBindConstructor(type, false));
	}

	/**
	 * Deduce the {@code BindMethod} that should be used for the given {@link Bindable}.
	 * @param bindable the source bindable
	 * @return the bind method to use
	 */
	static org.springframework.boot.context.properties.bind.BindMethod deduceBindMethod(Bindable<Object> bindable) {
		return deduceBindMethod(BindConstructorProvider.DEFAULT.getBindConstructor(bindable, false));
	}

	/**
     * Deduces the appropriate bind method based on the given bind constructor.
     * 
     * @param bindConstructor the constructor used for binding
     * @return the bind method to be used
     */
    private static org.springframework.boot.context.properties.bind.BindMethod deduceBindMethod(
			Constructor<?> bindConstructor) {
		return (bindConstructor != null) ? VALUE_OBJECT_BIND_METHOD : JAVA_BEAN_BIND_METHOD;
	}

}
