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
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.beans.BeanInfoFactory;
import org.springframework.beans.ExtendedBeanInfoFactory;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.core.ResolvableType;

/**
 * Registers a given type on {@link ReflectionHints} for binding purposes, discovering any
 * referenced types it may expose via a property.
 *
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 * @since 3.0.0
 */
public final class ConfigurationPropertiesReflectionHintsProcessor {

	private static final BeanInfoFactory beanInfoFactory = new ExtendedBeanInfoFactory();

	private final Class<?> type;

	private final Constructor<?> bindConstructor;

	private final BeanInfo beanInfo;

	private final Set<Class<?>> seen;

	private ConfigurationPropertiesReflectionHintsProcessor(Class<?> type, Constructor<?> bindConstructor,
			Set<Class<?>> seen) {
		this.type = type;
		this.bindConstructor = bindConstructor;
		this.beanInfo = getBeanInfo(type);
		this.seen = seen;
	}

	/**
	 * Registers a given type on {@link ReflectionHints} for binding purposes, discovering
	 * any nested type it may expose via a property.
	 * @param type type to process
	 * @param reflectionHints {@link ReflectionHints} to register the types on
	 */
	public static void processConfigurationProperties(Class<?> type, ReflectionHints reflectionHints) {
		new ConfigurationPropertiesReflectionHintsProcessor(type, getBindConstructor(type, false), new HashSet<>())
				.process(reflectionHints);
	}

	private void processType(Class<?> type, ReflectionHints reflectionHints) {
		if (isTypeIgnored(type)) {
			return;
		}
		new ConfigurationPropertiesReflectionHintsProcessor(type, getBindConstructor(type, true), this.seen)
				.process(reflectionHints);
	}

	private boolean isTypeIgnored(Class<?> type) {
		if (type.getPackageName().startsWith("java.")) {
			return true;
		}
		if (type.isInterface()) {
			return true;
		}
		return false;
	}

	private static Constructor<?> getBindConstructor(Class<?> type, boolean nestedType) {
		Bindable<?> bindable = Bindable.of(type);
		return ConfigurationPropertiesBindConstructorProvider.INSTANCE.getBindConstructor(bindable, nestedType);
	}

	private void process(ReflectionHints reflectionHints) {
		if (this.seen.contains(this.type)) {
			return;
		}
		this.seen.add(this.type);
		reflectionHints.registerType(this.type, (hint) -> hint.withMembers(MemberCategory.INVOKE_DECLARED_METHODS,
				MemberCategory.INVOKE_PUBLIC_METHODS));
		handleConstructor(reflectionHints);
		if (this.bindConstructor != null) {
			handleValueObjectProperties(reflectionHints);
		}
		else if (this.beanInfo != null) {
			handleJavaBeanProperties(reflectionHints);
		}
	}

	private void handleConstructor(ReflectionHints reflectionHints) {
		if (this.bindConstructor != null) {
			reflectionHints.registerConstructor(this.bindConstructor);
		}
		else {
			Arrays.stream(this.type.getDeclaredConstructors()).filter((candidate) -> candidate.getParameterCount() == 0)
					.findFirst().ifPresent(reflectionHints::registerConstructor);
		}
	}

	private void handleValueObjectProperties(ReflectionHints reflectionHints) {
		for (int i = 0; i < this.bindConstructor.getParameterCount(); i++) {
			ResolvableType propertyType = ResolvableType.forConstructorParameter(this.bindConstructor, i);
			registerType(reflectionHints, propertyType);
		}
	}

	private void handleJavaBeanProperties(ReflectionHints reflectionHints) {
		for (PropertyDescriptor propertyDescriptor : this.beanInfo.getPropertyDescriptors()) {
			Method readMethod = propertyDescriptor.getReadMethod();
			if (readMethod != null) {
				ResolvableType propertyType = ResolvableType.forMethodReturnType(readMethod, this.type);
				registerType(reflectionHints, propertyType);
			}
		}
	}

	private void registerType(ReflectionHints reflectionHints, ResolvableType propertyType) {
		Class<?> propertyClass = propertyType.resolve();
		if (propertyClass == null) {
			return;
		}
		if (propertyClass.equals(this.type)) {
			return; // Prevent infinite recursion
		}
		Class<?> componentType = getComponentType(propertyType);
		if (componentType != null) {
			processType(componentType, reflectionHints);
		}
		else {
			processType(propertyClass, reflectionHints);
		}
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
