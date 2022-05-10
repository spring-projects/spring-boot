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

package org.springframework.boot.context.properties;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.BeanInfoFactory;
import org.springframework.beans.ExtendedBeanInfoFactory;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link BeanFactoryInitializationAotProcessor} that contributes runtime hints for
 * configuration properties-annotated beans.
 *
 * @author Stephane Nicoll
 * @author Christoph Strobl
 * @author Sebastien Deleuze
 * @author Andy Wilkinson
 */
class ConfigurationPropertiesBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

	@Override
	public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
		String[] beanNames = beanFactory.getBeanNamesForAnnotation(ConfigurationProperties.class);
		List<Class<?>> types = new ArrayList<>();
		for (String beanName : beanNames) {
			BeanDefinition beanDefinition = beanFactory.getMergedBeanDefinition(beanName);
			types.add(ClassUtils.getUserClass(beanDefinition.getResolvableType().toClass()));
		}
		if (!CollectionUtils.isEmpty(types)) {
			return new ConfigurationPropertiesReflectionHintsContribution(types);
		}
		return null;
	}

	private static class ConfigurationPropertiesReflectionHintsContribution
			implements BeanFactoryInitializationAotContribution {

		private final Iterable<Class<?>> types;

		private ConfigurationPropertiesReflectionHintsContribution(Iterable<Class<?>> types) {
			this.types = types;
		}

		@Override
		public void applyTo(GenerationContext generationContext,
				BeanFactoryInitializationCode beanFactoryInitializationCode) {
			for (Class<?> type : this.types) {
				TypeProcessor.processConfigurationProperties(type, generationContext.getRuntimeHints());
			}
		}

	}

	/**
	 * Process a given type for binding purposes, discovering any nested type it may
	 * expose via a property.
	 */
	private static class TypeProcessor {

		private static final BeanInfoFactory beanInfoFactory = new ExtendedBeanInfoFactory();

		private final Class<?> type;

		private final Constructor<?> bindConstructor;

		private final BeanInfo beanInfo;

		private final Set<Class<?>> seen;

		private TypeProcessor(Class<?> type, Constructor<?> bindConstructor, Set<Class<?>> seen) {
			this.type = type;
			this.bindConstructor = bindConstructor;
			this.beanInfo = getBeanInfo(type);
			this.seen = seen;
		}

		public static void processConfigurationProperties(Class<?> type, RuntimeHints runtimeHints) {
			new TypeProcessor(type, getBindConstructor(type, false), new HashSet<>()).process(runtimeHints);
		}

		private void processNestedType(Class<?> type, RuntimeHints runtimeHints) {
			processNestedType(type, getBindConstructor(type, true), runtimeHints);
		}

		private void processNestedType(Class<?> type, Constructor<?> bindConstructor, RuntimeHints runtimeHints) {
			new TypeProcessor(type, bindConstructor, this.seen).process(runtimeHints);
		}

		private static Constructor<?> getBindConstructor(Class<?> type, boolean nestedType) {
			Bindable<?> bindable = Bindable.of(type);
			return ConfigurationPropertiesBindConstructorProvider.INSTANCE.getBindConstructor(bindable, nestedType);
		}

		private void process(RuntimeHints runtimeHints) {
			if (this.seen.contains(this.type)) {
				return;
			}
			this.seen.add(this.type);
			runtimeHints.reflection().registerType(this.type, (hint) -> hint
					.withMembers(MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.INVOKE_PUBLIC_METHODS));
			handleConstructor(runtimeHints);
			if (this.bindConstructor != null) {
				handleValueObjectProperties(runtimeHints);
			}
			else if (this.beanInfo != null) {
				handleJavaBeanProperties(runtimeHints);
			}
		}

		private void handleConstructor(RuntimeHints runtimeHints) {
			if (this.bindConstructor != null) {
				runtimeHints.reflection().registerConstructor(this.bindConstructor);
			}
			else {
				Arrays.stream(this.type.getDeclaredConstructors())
						.filter((candidate) -> candidate.getParameterCount() == 0).findFirst()
						.ifPresent((constructor) -> runtimeHints.reflection().registerConstructor(constructor));
			}
		}

		private void handleValueObjectProperties(RuntimeHints runtimeHints) {
			for (int i = 0; i < this.bindConstructor.getParameterCount(); i++) {
				String propertyName = this.bindConstructor.getParameters()[i].getName();
				ResolvableType propertyType = ResolvableType.forConstructorParameter(this.bindConstructor, i);
				handleProperty(runtimeHints, propertyName, propertyType);
			}
		}

		private void handleJavaBeanProperties(RuntimeHints runtimeHints) {
			for (PropertyDescriptor propertyDescriptor : this.beanInfo.getPropertyDescriptors()) {
				Method readMethod = propertyDescriptor.getReadMethod();
				if (readMethod != null) {
					ResolvableType propertyType = ResolvableType.forMethodReturnType(readMethod, this.type);
					String propertyName = propertyDescriptor.getName();
					if (isSetterMandatory(propertyName, propertyType) && propertyDescriptor.getWriteMethod() == null) {
						continue;
					}
					handleProperty(runtimeHints, propertyName, propertyType);
				}
			}
		}

		private void handleProperty(RuntimeHints runtimeHints, String propertyName, ResolvableType propertyType) {
			Class<?> propertyClass = propertyType.resolve();
			if (propertyClass == null) {
				return;
			}
			if (propertyClass.equals(this.type)) {
				return; // Prevent infinite recursion
			}
			Class<?> componentType = getComponentType(propertyType);
			if (componentType != null) {
				// Can be a list of simple types
				if (!isJavaType(componentType)) {
					processNestedType(componentType, runtimeHints);
				}
			}
			else if (isNestedType(propertyName, propertyClass)) {
				processNestedType(propertyClass, runtimeHints);
			}
		}

		private boolean isSetterMandatory(String propertyName, ResolvableType propertyType) {
			Class<?> propertyClass = propertyType.resolve();
			if (propertyClass == null) {
				return true;
			}
			if (getComponentType(propertyType) != null) {
				return false;
			}
			return !isNestedType(propertyName, propertyClass);
		}

		private Class<?> getComponentType(ResolvableType propertyType) {
			Class<?> propertyClass = propertyType.toClass();
			if (propertyType.isArray()) {
				return propertyType.getComponentType().toClass();
			}
			else if (Collection.class.isAssignableFrom(propertyClass)) {
				return propertyType.as(Collection.class).getGeneric(0).toClass();
			}
			else if (Map.class.isAssignableFrom(propertyClass)) {
				return propertyType.as(Map.class).getGeneric(1).toClass();
			}
			return null;
		}

		/**
		 * Specify whether the specified property refer to a nested type. A nested type
		 * represents a sub-namespace that need to be fully resolved.
		 * @param propertyName the name of the property
		 * @param propertyType the type of the property
		 * @return whether the specified {@code propertyType} is a nested type
		 */
		private boolean isNestedType(String propertyName, Class<?> propertyType) {
			if (this.type.equals(propertyType.getDeclaringClass())) {
				return true;
			}
			else {
				Field field = ReflectionUtils.findField(this.type, propertyName);
				return field != null && MergedAnnotations.from(field).isPresent(NestedConfigurationProperty.class);
			}
		}

		private boolean isJavaType(Class<?> candidate) {
			return candidate.getPackageName().startsWith("java.");
		}

		private static BeanInfo getBeanInfo(Class<?> beanType) {
			try {
				BeanInfo beanInfo = beanInfoFactory.getBeanInfo(beanType);
				if (beanInfo != null) {
					return beanInfo;
				}
				return Introspector.getBeanInfo(beanType, Introspector.IGNORE_ALL_BEANINFO);
			}
			catch (IntrospectionException ex) {
				return null;
			}
		}

	}

}
