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

package org.springframework.boot.context.properties.bind;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.BeanInfoFactory;
import org.springframework.beans.ExtendedBeanInfoFactory;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link RuntimeHintsRegistrar} that can be used to register {@link ReflectionHints} for
 * {@link Bindable} types, discovering any nested type it may expose via a property.
 * <p>
 * This class can be used as a base-class, or instantiated using the {@code forTypes}
 * factory methods.
 *
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 * @author Sebastien Deleuze
 * @author Phillip Webb
 * @since 3.0.0
 */
public class BindableRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

	private static final BeanInfoFactory beanInfoFactory = new ExtendedBeanInfoFactory();

	private final Class<?>[] types;

	protected BindableRuntimeHintsRegistrar(Class<?>... types) {
		this.types = types;
	}

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		registerHints(hints);
	}

	public void registerHints(RuntimeHints hints) {
		for (Class<?> type : this.types) {
			new Processor(type).process(hints.reflection());
		}
	}

	public static BindableRuntimeHintsRegistrar forTypes(Iterable<Class<?>> types) {
		Assert.notNull(types, "Types must not be null");
		return forTypes(StreamSupport.stream(types.spliterator(), false).toArray(Class<?>[]::new));
	}

	public static BindableRuntimeHintsRegistrar forTypes(Class<?>... types) {
		return new BindableRuntimeHintsRegistrar(types);
	}

	private final class Processor {

		private final Class<?> type;

		private final Constructor<?> bindConstructor;

		private final BeanInfo beanInfo;

		private final Set<Class<?>> seen;

		Processor(Class<?> type) {
			this(type, false, new HashSet<>());
		}

		private Processor(Class<?> type, boolean nestedType, Set<Class<?>> seen) {
			this.type = type;
			this.bindConstructor = BindConstructorProvider.DEFAULT.getBindConstructor(Bindable.of(type), nestedType);
			this.beanInfo = getBeanInfo(type);
			this.seen = seen;
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

		void process(ReflectionHints hints) {
			if (this.seen.contains(this.type)) {
				return;
			}
			this.seen.add(this.type);
			handleConstructor(hints);
			if (this.bindConstructor != null) {
				handleValueObjectProperties(hints);
			}
			else if (this.beanInfo != null) {
				handleJavaBeanProperties(hints);
			}
		}

		private void handleConstructor(ReflectionHints hints) {
			if (this.bindConstructor != null) {
				hints.registerConstructor(this.bindConstructor, ExecutableMode.INVOKE);
				return;
			}
			Arrays.stream(this.type.getDeclaredConstructors()).filter(this::hasNoParameters).findFirst()
					.ifPresent((constructor) -> hints.registerConstructor(constructor, ExecutableMode.INVOKE));
		}

		private boolean hasNoParameters(Constructor<?> candidate) {
			return candidate.getParameterCount() == 0;
		}

		private void handleValueObjectProperties(ReflectionHints hints) {
			for (int i = 0; i < this.bindConstructor.getParameterCount(); i++) {
				String propertyName = this.bindConstructor.getParameters()[i].getName();
				ResolvableType propertyType = ResolvableType.forConstructorParameter(this.bindConstructor, i);
				handleProperty(hints, propertyName, propertyType);
			}
		}

		private void handleJavaBeanProperties(ReflectionHints hints) {
			for (PropertyDescriptor propertyDescriptor : this.beanInfo.getPropertyDescriptors()) {
				Method writeMethod = propertyDescriptor.getWriteMethod();
				if (writeMethod != null) {
					hints.registerMethod(writeMethod, ExecutableMode.INVOKE);
				}
				Method readMethod = propertyDescriptor.getReadMethod();
				if (readMethod != null) {
					ResolvableType propertyType = ResolvableType.forMethodReturnType(readMethod, this.type);
					String propertyName = propertyDescriptor.getName();
					if (isSetterMandatory(propertyName, propertyType) && writeMethod == null) {
						continue;
					}
					handleProperty(hints, propertyName, propertyType);
					hints.registerMethod(readMethod, ExecutableMode.INVOKE);
				}
			}
		}

		private boolean isSetterMandatory(String propertyName, ResolvableType propertyType) {
			Class<?> propertyClass = propertyType.resolve();
			if (propertyClass == null) {
				return true;
			}
			if (isContainer(propertyType)) {
				return false;
			}
			return !isNestedType(propertyName, propertyClass);
		}

		private void handleProperty(ReflectionHints hints, String propertyName, ResolvableType propertyType) {
			Class<?> propertyClass = propertyType.resolve();
			if (propertyClass == null) {
				return;
			}
			if (propertyClass.equals(this.type)) {
				return; // Prevent infinite recursion
			}
			Class<?> componentType = getComponentClass(propertyType);
			if (componentType != null) {
				// Can be a list of simple types
				if (!isJavaType(componentType)) {
					processNested(componentType, hints);
				}
			}
			else if (isNestedType(propertyName, propertyClass)) {
				processNested(propertyClass, hints);
			}
		}

		private void processNested(Class<?> type, ReflectionHints hints) {
			new Processor(type, true, this.seen).process(hints);
		}

		private Class<?> getComponentClass(ResolvableType type) {
			ResolvableType componentType = getComponentType(type);
			if (componentType == null) {
				return null;
			}
			if (isContainer(componentType)) {
				// Resolve nested generics like Map<String, List<SomeType>>
				return getComponentClass(componentType);
			}
			return componentType.toClass();
		}

		private ResolvableType getComponentType(ResolvableType type) {
			if (type.isArray()) {
				return type.getComponentType();
			}
			if (isCollection(type)) {
				return type.asCollection().getGeneric();
			}
			if (isMap(type)) {
				return type.asMap().getGeneric(1);
			}
			return null;
		}

		private boolean isContainer(ResolvableType type) {
			return type.isArray() || isCollection(type) || isMap(type);
		}

		private boolean isCollection(ResolvableType type) {
			return Collection.class.isAssignableFrom(type.toClass());
		}

		private boolean isMap(ResolvableType type) {
			return Map.class.isAssignableFrom(type.toClass());
		}

		/**
		 * Specify whether the specified property refer to a nested type. A nested type
		 * represents a sub-namespace that need to be fully resolved. Nested types are
		 * either inner classes or annotated with {@link NestedConfigurationProperty}.
		 * @param propertyName the name of the property
		 * @param propertyType the type of the property
		 * @return whether the specified {@code propertyType} is a nested type
		 */
		private boolean isNestedType(String propertyName, Class<?> propertyType) {
			if (this.type.equals(propertyType.getDeclaringClass())) {
				return true;
			}
			Field field = ReflectionUtils.findField(this.type, propertyName);
			return (field != null) && MergedAnnotations.from(field).isPresent(Nested.class);
		}

		private boolean isJavaType(Class<?> candidate) {
			return candidate.getPackageName().startsWith("java.");
		}

	}

}
