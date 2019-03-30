/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.condition;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.StandardMethodMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * A registry of the bean types that are contained in a
 * {@link DefaultListableBeanFactory}. Provides similar functionality to
 * {@link ListableBeanFactory#getBeanNamesForType(Class, boolean, boolean)} but is
 * optimized for use by {@link OnBeanCondition} based on the following assumptions:
 * <ul>
 * <li>Bean definitions will not change type.</li>
 * <li>Beans definitions will not be removed.</li>
 * <li>Beans will not be created in parallel.</li>
 * </ul>
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.2.0
 */
final class BeanTypeRegistry implements SmartInitializingSingleton {

	private static final Log logger = LogFactory.getLog(BeanTypeRegistry.class);

	static final String FACTORY_BEAN_OBJECT_TYPE = "factoryBeanObjectType";

	private static final String BEAN_NAME = BeanTypeRegistry.class.getName();

	private final DefaultListableBeanFactory beanFactory;

	private final Map<String, ResolvableType> beanTypes = new HashMap<>();

	private final Map<String, RootBeanDefinition> beanDefinitions = new HashMap<>();

	private BeanTypeRegistry(DefaultListableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * Return the names of beans matching the given type (including subclasses), judging
	 * from either bean definitions or the value of {@link FactoryBean#getObjectType()} in
	 * the case of {@link FactoryBean FactoryBeans}. Will include singletons but will not
	 * cause early bean initialization.
	 * @param type the class or interface to match (must not be {@code null})
	 * @param typeExtractor function used to extract the actual type
	 * @return the names of beans (or objects created by FactoryBeans) matching the given
	 * object type (including subclasses), or an empty set if none
	 */
	public Set<String> getNamesForType(Class<?> type, TypeExtractor typeExtractor) {
		updateTypesIfNecessary();
		return this.beanTypes.entrySet().stream().filter((entry) -> {
			Class<?> beanType = extractType(entry.getValue(), typeExtractor);
			return beanType != null && type.isAssignableFrom(beanType);
		}).map(Map.Entry::getKey).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private Class<?> extractType(ResolvableType type, TypeExtractor extractor) {
		return (type != null) ? extractor.getBeanType(type) : null;
	}

	/**
	 * Returns the names of beans annotated with the given {@code annotation}, judging
	 * from either bean definitions or the value of {@link FactoryBean#getObjectType()} in
	 * the case of {@link FactoryBean FactoryBeans}. Will include singletons but will not
	 * cause early bean initialization.
	 * @param annotation the annotation to match (must not be {@code null})
	 * @return the names of beans (or objects created by FactoryBeans) annotated with the
	 * given annotation, or an empty set if none
	 */
	public Set<String> getNamesForAnnotation(Class<? extends Annotation> annotation) {
		updateTypesIfNecessary();
		return this.beanTypes.entrySet().stream()
				.filter((entry) -> entry.getValue() != null && AnnotationUtils
						.findAnnotation(entry.getValue().resolve(), annotation) != null)
				.map(Map.Entry::getKey)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	@Override
	public void afterSingletonsInstantiated() {
		// We're done at this point, free up some memory
		this.beanTypes.clear();
		this.beanDefinitions.clear();
	}

	private void updateTypesIfNecessary() {
		this.beanFactory.getBeanNamesIterator()
				.forEachRemaining(this::updateTypesIfNecessary);
	}

	private void updateTypesIfNecessary(String name) {
		if (!this.beanTypes.containsKey(name)) {
			addBeanType(name);
		}
		else {
			updateBeanType(name);
		}
	}

	private void addBeanType(String name) {
		if (this.beanFactory.containsSingleton(name)) {
			this.beanTypes.put(name, getType(name, null));
		}
		else if (!this.beanFactory.isAlias(name)) {
			addBeanTypeForNonAliasDefinition(name);
		}
	}

	private void addBeanTypeForNonAliasDefinition(String name) {
		RootBeanDefinition definition = getBeanDefinition(name);
		if (definition != null) {
			addBeanTypeForNonAliasDefinition(name, definition);
		}
	}

	private void updateBeanType(String name) {
		if (this.beanFactory.isAlias(name) || this.beanFactory.containsSingleton(name)) {
			return;
		}
		RootBeanDefinition definition = getBeanDefinition(name);
		if (definition == null) {
			return;
		}
		RootBeanDefinition previous = this.beanDefinitions.put(name, definition);
		if (previous != null && !definition.equals(previous)) {
			addBeanTypeForNonAliasDefinition(name, definition);
		}
	}

	private RootBeanDefinition getBeanDefinition(String name) {
		try {
			return (RootBeanDefinition) this.beanFactory.getMergedBeanDefinition(name);
		}
		catch (BeanDefinitionStoreException ex) {
			logIgnoredError("unresolvable metadata in bean definition", name, ex);
			return null;
		}
	}

	private void addBeanTypeForNonAliasDefinition(String name,
			RootBeanDefinition definition) {
		try {
			if (!definition.isAbstract()
					&& !requiresEagerInit(definition.getFactoryBeanName())) {
				ResolvableType factoryMethodReturnType = getFactoryMethodReturnType(
						definition);
				String factoryBeanName = BeanFactory.FACTORY_BEAN_PREFIX + name;
				if (this.beanFactory.isFactoryBean(factoryBeanName)) {
					ResolvableType factoryBeanGeneric = getFactoryBeanGeneric(
							this.beanFactory, definition, factoryMethodReturnType);
					this.beanTypes.put(name, factoryBeanGeneric);
					this.beanTypes.put(factoryBeanName,
							getType(factoryBeanName, factoryMethodReturnType));
				}
				else {
					this.beanTypes.put(name, getType(name, factoryMethodReturnType));
				}
			}
			this.beanDefinitions.put(name, definition);
		}
		catch (CannotLoadBeanClassException ex) {
			// Probably contains a placeholder
			logIgnoredError("bean class loading failure for bean", name, ex);
		}
	}

	private boolean requiresEagerInit(String factoryBeanName) {
		return (factoryBeanName != null && this.beanFactory.isFactoryBean(factoryBeanName)
				&& !this.beanFactory.containsSingleton(factoryBeanName));
	}

	private ResolvableType getFactoryMethodReturnType(BeanDefinition definition) {
		try {
			if (StringUtils.hasLength(definition.getFactoryBeanName())
					&& StringUtils.hasLength(definition.getFactoryMethodName())) {
				Method method = getFactoryMethod(this.beanFactory, definition);
				ResolvableType type = (method != null)
						? ResolvableType.forMethodReturnType(method) : null;
				return type;
			}
		}
		catch (Exception ex) {
		}
		return null;
	}

	private Method getFactoryMethod(ConfigurableListableBeanFactory beanFactory,
			BeanDefinition definition) throws Exception {
		if (definition instanceof AnnotatedBeanDefinition) {
			MethodMetadata factoryMethodMetadata = ((AnnotatedBeanDefinition) definition)
					.getFactoryMethodMetadata();
			if (factoryMethodMetadata instanceof StandardMethodMetadata) {
				return ((StandardMethodMetadata) factoryMethodMetadata)
						.getIntrospectedMethod();
			}
		}
		BeanDefinition factoryDefinition = beanFactory
				.getBeanDefinition(definition.getFactoryBeanName());
		Class<?> factoryClass = ClassUtils.forName(factoryDefinition.getBeanClassName(),
				beanFactory.getBeanClassLoader());
		return getFactoryMethod(definition, factoryClass);
	}

	private Method getFactoryMethod(BeanDefinition definition, Class<?> factoryClass) {
		Method uniqueMethod = null;
		for (Method candidate : getCandidateFactoryMethods(definition, factoryClass)) {
			if (candidate.getName().equals(definition.getFactoryMethodName())) {
				if (uniqueMethod == null) {
					uniqueMethod = candidate;
				}
				else if (!hasMatchingParameterTypes(candidate, uniqueMethod)) {
					return null;
				}
			}
		}
		return uniqueMethod;
	}

	private Method[] getCandidateFactoryMethods(BeanDefinition definition,
			Class<?> factoryClass) {
		return (shouldConsiderNonPublicMethods(definition)
				? ReflectionUtils.getAllDeclaredMethods(factoryClass)
				: factoryClass.getMethods());
	}

	private boolean shouldConsiderNonPublicMethods(BeanDefinition definition) {
		return (definition instanceof AbstractBeanDefinition)
				&& ((AbstractBeanDefinition) definition).isNonPublicAccessAllowed();
	}

	private boolean hasMatchingParameterTypes(Method candidate, Method current) {
		return Arrays.equals(candidate.getParameterTypes(), current.getParameterTypes());
	}

	private void logIgnoredError(String message, String name, Exception ex) {
		if (logger.isDebugEnabled()) {
			logger.debug("Ignoring " + message + " '" + name + "'", ex);
		}
	}

	/**
	 * Attempt to guess the type that a {@link FactoryBean} will return based on the
	 * generics in its method signature.
	 * @param beanFactory the source bean factory
	 * @param definition the bean definition
	 * @param factoryMethodReturnType the factory method return type
	 * @return the generic type of the {@link FactoryBean} or {@code null}
	 */
	private ResolvableType getFactoryBeanGeneric(
			ConfigurableListableBeanFactory beanFactory, BeanDefinition definition,
			ResolvableType factoryMethodReturnType) {
		try {
			if (factoryMethodReturnType != null) {
				return getFactoryBeanType(definition, factoryMethodReturnType);
			}
			if (StringUtils.hasLength(definition.getBeanClassName())) {
				return getDirectFactoryBeanGeneric(beanFactory, definition);
			}
		}
		catch (Exception ex) {
		}
		return null;
	}

	private ResolvableType getDirectFactoryBeanGeneric(
			ConfigurableListableBeanFactory beanFactory, BeanDefinition definition)
			throws ClassNotFoundException, LinkageError {
		Class<?> factoryBeanClass = ClassUtils.forName(definition.getBeanClassName(),
				beanFactory.getBeanClassLoader());
		return getFactoryBeanType(definition, ResolvableType.forClass(factoryBeanClass));
	}

	private ResolvableType getFactoryBeanType(BeanDefinition definition,
			ResolvableType type) throws ClassNotFoundException, LinkageError {
		ResolvableType generic = type.as(FactoryBean.class).getGeneric();
		if ((generic == null || generic.resolve().equals(Object.class))
				&& definition.hasAttribute(FACTORY_BEAN_OBJECT_TYPE)) {
			generic = getTypeFromAttribute(
					definition.getAttribute(FACTORY_BEAN_OBJECT_TYPE));
		}
		return generic;
	}

	private ResolvableType getTypeFromAttribute(Object attribute)
			throws ClassNotFoundException, LinkageError {
		if (attribute instanceof Class<?>) {
			return ResolvableType.forClass((Class<?>) attribute);
		}
		if (attribute instanceof String) {
			return ResolvableType.forClass(ClassUtils.forName((String) attribute, null));
		}
		return null;
	}

	private ResolvableType getType(String name, ResolvableType factoryMethodReturnType) {
		if (factoryMethodReturnType != null
				&& !factoryMethodReturnType.resolve(Object.class).equals(Object.class)) {
			return factoryMethodReturnType;
		}
		Class<?> type = this.beanFactory.getType(name);
		return (type != null) ? ResolvableType.forClass(type) : null;
	}

	/**
	 * Factory method to get the {@link BeanTypeRegistry} for a given {@link BeanFactory}.
	 * @param beanFactory the source bean factory
	 * @return the {@link BeanTypeRegistry} for the given bean factory
	 */
	static BeanTypeRegistry get(ListableBeanFactory beanFactory) {
		Assert.isInstanceOf(DefaultListableBeanFactory.class, beanFactory);
		DefaultListableBeanFactory listableBeanFactory = (DefaultListableBeanFactory) beanFactory;
		Assert.isTrue(listableBeanFactory.isAllowEagerClassLoading(),
				"Bean factory must allow eager class loading");
		if (!listableBeanFactory.containsLocalBean(BEAN_NAME)) {
			BeanDefinition definition = BeanDefinitionBuilder
					.genericBeanDefinition(BeanTypeRegistry.class,
							() -> new BeanTypeRegistry(
									(DefaultListableBeanFactory) beanFactory))
					.getBeanDefinition();
			listableBeanFactory.registerBeanDefinition(BEAN_NAME, definition);
		}
		return listableBeanFactory.getBean(BEAN_NAME, BeanTypeRegistry.class);
	}

	/**
	 * Function used to extract the actual bean type from a source {@link ResolvableType}.
	 * May be used to support parameterized containers for beans.
	 */
	@FunctionalInterface
	interface TypeExtractor {

		Class<?> getBeanType(ResolvableType type);

	}

}
