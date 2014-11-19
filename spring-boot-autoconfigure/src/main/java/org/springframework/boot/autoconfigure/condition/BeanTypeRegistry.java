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

package org.springframework.boot.autoconfigure.condition;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * A registry of the bean types that are contained in a {@link ListableBeanFactory}.
 * Provides similar functionality to
 * {@link ListableBeanFactory#getBeanNamesForType(Class, boolean, boolean)} but is
 * optimized for use by {@link OnBeanCondition} based on the following assumptions:
 * <ul>
 * <li>Bean definitions will not change type.</li>
 * <li>Beans definitions will not be removed.</li>
 * <li>Beans will not be created in parallel.</li>
 * </ul>
 *
 * @author Phillip Webb
 * @since 1.2.0
 */
abstract class BeanTypeRegistry {

	static Log logger = LogFactory.getLog(BeanTypeRegistry.class);

	static final String FACTORY_BEAN_OBJECT_TYPE = "factoryBeanObjectType";

	/**
	 * Return the names of beans matching the given type (including subclasses), judging
	 * from either bean definitions or the value of {@code getObjectType} in the case of
	 * FactoryBeans. Will include singletons but not cause early bean initialization.
	 * @param type the class or interface to match (must not be {@code null})
	 * @return the names of beans (or objects created by FactoryBeans) matching the given
	 * object type (including subclasses), or an empty set if none
	 */
	public abstract Set<String> getNamesForType(Class<?> type);

	/**
	 * Attempt to guess the type that a {@link FactoryBean} will return based on the
	 * generics in its method signature.
	 * @param beanFactory the source bean factory
	 * @param definition the bean definition
	 * @param name the name of the bean
	 * @return the generic type of the {@link FactoryBean} or {@code null}
	 */
	protected final Class<?> getFactoryBeanGeneric(
			ConfigurableListableBeanFactory beanFactory, BeanDefinition definition,
			String name) {
		try {
			return doGetFactoryBeanGeneric(beanFactory, definition, name);
		}
		catch (Exception ex) {
			return null;
		}
	}

	private Class<?> doGetFactoryBeanGeneric(ConfigurableListableBeanFactory beanFactory,
			BeanDefinition definition, String name) throws Exception,
			ClassNotFoundException, LinkageError {
		if (StringUtils.hasLength(definition.getFactoryBeanName())
				&& StringUtils.hasLength(definition.getFactoryMethodName())) {
			return getConfigurationClassFactoryBeanGeneric(beanFactory, definition, name);
		}
		if (StringUtils.hasLength(definition.getBeanClassName())) {
			return getDirectFactoryBeanGeneric(beanFactory, definition, name);
		}
		return null;
	}

	private Class<?> getConfigurationClassFactoryBeanGeneric(
			ConfigurableListableBeanFactory beanFactory, BeanDefinition definition,
			String name) throws Exception {
		BeanDefinition factoryDefinition = beanFactory.getBeanDefinition(definition
				.getFactoryBeanName());
		Class<?> factoryClass = ClassUtils.forName(factoryDefinition.getBeanClassName(),
				beanFactory.getBeanClassLoader());
		Method method = ReflectionUtils.findMethod(factoryClass,
				definition.getFactoryMethodName());
		Class<?> generic = ResolvableType.forMethodReturnType(method)
				.as(FactoryBean.class).resolveGeneric();
		if ((generic == null || generic.equals(Object.class))
				&& definition.hasAttribute(FACTORY_BEAN_OBJECT_TYPE)) {
			generic = (Class<?>) definition.getAttribute(FACTORY_BEAN_OBJECT_TYPE);
		}
		return generic;
	}

	private Class<?> getDirectFactoryBeanGeneric(
			ConfigurableListableBeanFactory beanFactory, BeanDefinition definition,
			String name) throws ClassNotFoundException, LinkageError {
		Class<?> factoryBeanClass = ClassUtils.forName(definition.getBeanClassName(),
				beanFactory.getBeanClassLoader());
		Class<?> generic = ResolvableType.forClass(factoryBeanClass)
				.as(FactoryBean.class).resolveGeneric();
		if ((generic == null || generic.equals(Object.class))
				&& definition.hasAttribute(FACTORY_BEAN_OBJECT_TYPE)) {
			generic = (Class<?>) definition.getAttribute(FACTORY_BEAN_OBJECT_TYPE);
		}
		return generic;
	}

	/**
	 * Factory method to get the {@link BeanTypeRegistry} for a given {@link BeanFactory}.
	 * @param beanFactory the source bean factory
	 * @return the {@link BeanTypeRegistry} for the given bean factory
	 */
	public static BeanTypeRegistry get(ListableBeanFactory beanFactory) {
		if (beanFactory instanceof DefaultListableBeanFactory) {
			DefaultListableBeanFactory listableBeanFactory = (DefaultListableBeanFactory) beanFactory;
			if (listableBeanFactory.isAllowEagerClassLoading()) {
				return OptimizedBeanTypeRegistry.getFromFactory(listableBeanFactory);
			}
		}
		return new DefaultBeanTypeRegistry(beanFactory);
	}

	/**
	 * Default (non-optimized) {@link BeanTypeRegistry} implementation.
	 */
	static class DefaultBeanTypeRegistry extends BeanTypeRegistry {

		private final ListableBeanFactory beanFactory;

		public DefaultBeanTypeRegistry(ListableBeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}

		@Override
		public Set<String> getNamesForType(Class<?> type) {
			Set<String> result = new LinkedHashSet<String>();
			result.addAll(Arrays.asList(this.beanFactory.getBeanNamesForType(type, true,
					false)));
			if (this.beanFactory instanceof ConfigurableListableBeanFactory) {
				collectBeanNamesForTypeFromFactoryBeans(result,
						(ConfigurableListableBeanFactory) this.beanFactory, type);
			}
			return result;
		}

		private void collectBeanNamesForTypeFromFactoryBeans(Set<String> result,
				ConfigurableListableBeanFactory beanFactory, Class<?> type) {
			String[] names = beanFactory.getBeanNamesForType(FactoryBean.class, true,
					false);
			for (String name : names) {
				name = BeanFactoryUtils.transformedBeanName(name);
				BeanDefinition beanDefinition = beanFactory.getBeanDefinition(name);
				Class<?> generic = getFactoryBeanGeneric(beanFactory, beanDefinition,
						name);
				if (generic != null && ClassUtils.isAssignable(type, generic)) {
					result.add(name);
				}
			}
		}

	}

	/**
	 * {@link BeanTypeRegistry} optimized for {@link DefaultListableBeanFactory}
	 * implementations that allow eager class loading.
	 */
	static class OptimizedBeanTypeRegistry extends BeanTypeRegistry implements
			SmartInitializingSingleton {

		private static final String BEAN_NAME = BeanTypeRegistry.class.getName();

		private final DefaultListableBeanFactory beanFactory;

		private final Map<String, Class<?>> beanTypes = new HashMap<String, Class<?>>();

		private int lastBeanDefinitionCount = 0;

		public OptimizedBeanTypeRegistry(DefaultListableBeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}

		@Override
		public void afterSingletonsInstantiated() {
			// We're done at this point, free up some memory
			this.beanTypes.clear();
			this.lastBeanDefinitionCount = 0;
		}

		@Override
		public Set<String> getNamesForType(Class<?> type) {
			if (this.lastBeanDefinitionCount != this.beanFactory.getBeanDefinitionCount()) {
				Iterator<String> names = this.beanFactory.getBeanNamesIterator();
				while (names.hasNext()) {
					String name = names.next();
					if (!this.beanTypes.containsKey(name)) {
						addBeanType(name);
					}
				}
				this.lastBeanDefinitionCount = this.beanFactory.getBeanDefinitionCount();
			}
			Set<String> matches = new LinkedHashSet<String>();
			for (Map.Entry<String, Class<?>> entry : this.beanTypes.entrySet()) {
				if (entry.getValue() != null && type.isAssignableFrom(entry.getValue())) {
					matches.add(entry.getKey());
				}
			}
			return matches;
		}

		private void addBeanType(String name) {
			if (this.beanFactory.containsSingleton(name)) {
				this.beanTypes.put(name, this.beanFactory.getType(name));
			}
			else if (!this.beanFactory.isAlias(name)) {
				addBeanTypeForNonAliasDefinition(name);
			}
		}

		private void addBeanTypeForNonAliasDefinition(String name) {
			try {
				String factoryName = BeanFactory.FACTORY_BEAN_PREFIX + name;
				RootBeanDefinition beanDefinition = (RootBeanDefinition) this.beanFactory
						.getMergedBeanDefinition(name);
				if (!beanDefinition.isAbstract()
						&& !requiresEagerInit(beanDefinition.getFactoryBeanName())) {
					if (this.beanFactory.isFactoryBean(factoryName)) {
						Class<?> factoryBeanGeneric = getFactoryBeanGeneric(
								this.beanFactory, beanDefinition, name);
						this.beanTypes.put(name, factoryBeanGeneric);
						this.beanTypes.put(factoryName,
								this.beanFactory.getType(factoryName));
					}
					else {
						this.beanTypes.put(name, this.beanFactory.getType(name));
					}
				}
			}
			catch (CannotLoadBeanClassException ex) {
				// Probably contains a placeholder
				logIgnoredError("bean class loading failure for bean", name, ex);
			}
			catch (BeanDefinitionStoreException ex) {
				// Probably contains a placeholder
				logIgnoredError("unresolvable metadata in bean definition", name, ex);
			}
		}

		private void logIgnoredError(String message, String name, Exception ex) {
			if (BeanTypeRegistry.logger.isDebugEnabled()) {
				BeanTypeRegistry.logger.debug("Ignoring " + message + " '" + name + "'",
						ex);
			}
		}

		private boolean requiresEagerInit(String factoryBeanName) {
			return (factoryBeanName != null
					&& this.beanFactory.isFactoryBean(factoryBeanName) && !this.beanFactory
						.containsSingleton(factoryBeanName));
		}

		/**
		 * Returns the {@link OptimizedBeanTypeRegistry} for the given bean factory.
		 */
		public static OptimizedBeanTypeRegistry getFromFactory(
				DefaultListableBeanFactory factory) {
			if (!factory.containsLocalBean(BEAN_NAME)) {
				BeanDefinition bd = new RootBeanDefinition(
						OptimizedBeanTypeRegistry.class);
				bd.getConstructorArgumentValues().addIndexedArgumentValue(0, factory);
				factory.registerBeanDefinition(BEAN_NAME, bd);

			}
			return factory.getBean(BEAN_NAME, OptimizedBeanTypeRegistry.class);
		}

	}

}
