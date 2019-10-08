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

package org.springframework.boot.context.properties;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.KotlinDetector;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

/**
 * Provides access to {@link ConfigurationProperties} beans from an
 * {@link ApplicationContext}.
 *
 * @author Phillip Webb
 * @since 2.2.0
 * @see #get(ApplicationContext, Object, String)
 * @see #getAll(ApplicationContext)
 */
public final class ConfigurationPropertiesBean {

	private final String name;

	private final Object instance;

	private final ConfigurationProperties annotation;

	private final Bindable<?> bindTarget;

	private final BindMethod bindMethod;

	private ConfigurationPropertiesBean(String name, Object instance, ConfigurationProperties annotation,
			Bindable<?> bindTarget, BindMethod bindMethod) {
		this.name = name;
		this.instance = instance;
		this.annotation = annotation;
		this.bindTarget = bindTarget;
		this.bindMethod = bindMethod;
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
	 * Return the property binding method that was used for the bean.
	 * @return the bind type
	 */
	public BindMethod getBindMethod() {
		return this.bindMethod;
	}

	/**
	 * Return the {@link ConfigurationProperties} annotation for the bean. The annotation
	 * may be defined on the bean itself or from the factory method that create the bean
	 * (usually a {@link Bean @Bean} method).
	 * @return the configuration properties annotation
	 */
	public ConfigurationProperties getAnnotation() {
		return this.annotation;
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
		if (applicationContext instanceof ConfigurableApplicationContext) {
			return getAll((ConfigurableApplicationContext) applicationContext);
		}
		Map<String, ConfigurationPropertiesBean> propertiesBeans = new LinkedHashMap<>();
		applicationContext.getBeansWithAnnotation(ConfigurationProperties.class)
				.forEach((beanName, bean) -> propertiesBeans.put(beanName, get(applicationContext, bean, beanName)));
		return propertiesBeans;
	}

	private static Map<String, ConfigurationPropertiesBean> getAll(ConfigurableApplicationContext applicationContext) {
		Map<String, ConfigurationPropertiesBean> propertiesBeans = new LinkedHashMap<>();
		ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
		Iterator<String> beanNames = beanFactory.getBeanNamesIterator();
		while (beanNames.hasNext()) {
			String beanName = beanNames.next();
			try {
				Object bean = beanFactory.getBean(beanName);
				ConfigurationPropertiesBean propertiesBean = get(applicationContext, bean, beanName);
				if (propertiesBean != null) {
					propertiesBeans.put(beanName, propertiesBean);
				}
			}
			catch (NoSuchBeanDefinitionException ex) {
			}
		}
		return propertiesBeans;
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
	 * @return a configuration properties bean or {@code null} if the neither the bean or
	 * factory method are annotated with
	 * {@link ConfigurationProperties @ConfigurationProperties}
	 */
	public static ConfigurationPropertiesBean get(ApplicationContext applicationContext, Object bean, String beanName) {
		Method factoryMethod = findFactoryMethod(applicationContext, beanName);
		return create(beanName, bean, bean.getClass(), factoryMethod);
	}

	private static Method findFactoryMethod(ApplicationContext applicationContext, String beanName) {
		if (applicationContext instanceof ConfigurableApplicationContext) {
			return findFactoryMethod((ConfigurableApplicationContext) applicationContext, beanName);
		}
		return null;
	}

	private static Method findFactoryMethod(ConfigurableApplicationContext applicationContext, String beanName) {
		ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
		if (beanFactory.containsBeanDefinition(beanName)) {
			BeanDefinition beanDefinition = beanFactory.getMergedBeanDefinition(beanName);
			if (beanDefinition instanceof RootBeanDefinition) {
				return ((RootBeanDefinition) beanDefinition).getResolvedFactoryMethod();
			}
		}
		return null;
	}

	static ConfigurationPropertiesBean forValueObject(Class<?> beanClass, String beanName) {
		ConfigurationPropertiesBean propertiesBean = create(beanName, null, beanClass, null);
		Assert.state(propertiesBean != null && propertiesBean.getBindMethod() == BindMethod.VALUE_OBJECT,
				"Bean '" + beanName + "' is not a @ConfigurationProperties value object");
		return propertiesBean;
	}

	private static ConfigurationPropertiesBean create(String name, Object instance, Class<?> type, Method factory) {
		ConfigurationProperties annotation = findAnnotation(instance, type, factory, ConfigurationProperties.class);
		if (annotation == null) {
			return null;
		}
		Validated validated = findAnnotation(instance, type, factory, Validated.class);
		Annotation[] annotations = (validated != null) ? new Annotation[] { annotation, validated }
				: new Annotation[] { annotation };
		Constructor<?> bindConstructor = BindMethod.findBindConstructor(type);
		BindMethod bindMethod = (bindConstructor != null) ? BindMethod.VALUE_OBJECT : BindMethod.forClass(type);
		ResolvableType bindType = (factory != null) ? ResolvableType.forMethodReturnType(factory)
				: ResolvableType.forClass(type);
		Bindable<Object> bindTarget = Bindable.of(bindType).withAnnotations(annotations)
				.withConstructorFilter(ConfigurationPropertiesBean::isBindableConstructor);
		if (instance != null) {
			bindTarget = bindTarget.withExistingValue(instance);
		}
		return new ConfigurationPropertiesBean(name, instance, annotation, bindTarget, bindMethod);
	}

	private static <A extends Annotation> A findAnnotation(Object instance, Class<?> type, Method factory,
			Class<A> annotationType) {
		MergedAnnotation<A> annotation = MergedAnnotation.missing();
		if (factory != null) {
			annotation = MergedAnnotations.from(factory, SearchStrategy.TYPE_HIERARCHY).get(annotationType);
		}
		if (!annotation.isPresent()) {
			annotation = MergedAnnotations.from(type, SearchStrategy.TYPE_HIERARCHY).get(annotationType);
		}
		if (!annotation.isPresent() && AopUtils.isAopProxy(instance)) {
			annotation = MergedAnnotations.from(AopUtils.getTargetClass(instance), SearchStrategy.TYPE_HIERARCHY)
					.get(annotationType);
		}
		return annotation.isPresent() ? annotation.synthesize() : null;
	}

	private static boolean isBindableConstructor(Constructor<?> constructor) {
		Class<?> declaringClass = constructor.getDeclaringClass();
		Constructor<?> bindConstructor = BindMethod.findBindConstructor(declaringClass);
		if (bindConstructor != null) {
			return bindConstructor.equals(constructor);
		}
		return BindMethod.forClass(declaringClass) == BindMethod.VALUE_OBJECT;
	}

	/**
	 * The binding method that is used for the bean.
	 */
	public enum BindMethod {

		/**
		 * Java Bean using getter/setter binding.
		 */
		JAVA_BEAN,

		/**
		 * Value object using constructor binding.
		 */
		VALUE_OBJECT;

		static BindMethod forClass(Class<?> type) {
			if (isConstructorBindingType(type) || findBindConstructor(type) != null) {
				return VALUE_OBJECT;
			}
			return JAVA_BEAN;
		}

		private static boolean isConstructorBindingType(Class<?> type) {
			return MergedAnnotations.from(type, SearchStrategy.TYPE_HIERARCHY_AND_ENCLOSING_CLASSES)
					.isPresent(ConstructorBinding.class);
		}

		static Constructor<?> findBindConstructor(Class<?> type) {
			if (KotlinDetector.isKotlinPresent() && KotlinDetector.isKotlinType(type)) {
				Constructor<?> constructor = BeanUtils.findPrimaryConstructor(type);
				if (constructor != null) {
					return findBindConstructor(type, constructor);
				}
			}
			return findBindConstructor(type, type.getDeclaredConstructors());
		}

		private static Constructor<?> findBindConstructor(Class<?> type, Constructor<?>... candidates) {
			Constructor<?> constructor = null;
			for (Constructor<?> candidate : candidates) {
				if (MergedAnnotations.from(candidate).isPresent(ConstructorBinding.class)) {
					Assert.state(candidate.getParameterCount() > 0,
							type.getName() + " declares @ConstructorBinding on a no-args constructor");
					Assert.state(constructor == null,
							type.getName() + " has more than one @ConstructorBinding constructor");
					constructor = candidate;
				}
			}
			return constructor;
		}

	}

}
