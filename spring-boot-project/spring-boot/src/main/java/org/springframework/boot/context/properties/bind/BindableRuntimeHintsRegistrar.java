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

package org.springframework.boot.context.properties.bind;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.context.properties.bind.JavaBeanBinder.BeanProperties;
import org.springframework.boot.context.properties.bind.JavaBeanBinder.BeanProperty;
import org.springframework.core.KotlinDetector;
import org.springframework.core.KotlinReflectionParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.PrioritizedParameterNameDiscoverer;
import org.springframework.core.ResolvableType;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link RuntimeHintsRegistrar} that can be used to register {@link ReflectionHints} for
 * {@link Bindable} types, discovering any nested type it may expose through a property.
 * <p>
 * This class can be used as a base-class, or instantiated using the {@code forTypes} and
 * {@code forBindables} factory methods.
 *
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 * @author Sebastien Deleuze
 * @author Phillip Webb
 * @since 3.0.0
 */
public class BindableRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

	private final Bindable<?>[] bindables;

	/**
	 * Create a new {@link BindableRuntimeHintsRegistrar} for the specified types.
	 * @param types the types to process
	 */
	protected BindableRuntimeHintsRegistrar(Class<?>... types) {
		this(Stream.of(types).map(Bindable::of).toArray(Bindable[]::new));
	}

	/**
	 * Create a new {@link BindableRuntimeHintsRegistrar} for the specified bindables.
	 * @param bindables the bindables to process
	 * @since 3.0.8
	 */
	protected BindableRuntimeHintsRegistrar(Bindable<?>... bindables) {
		this.bindables = bindables;
	}

	/**
	 * Registers the runtime hints with the specified class loader.
	 * @param hints the runtime hints to register
	 * @param classLoader the class loader to use for registration
	 */
	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		registerHints(hints);
	}

	/**
	 * Contribute hints to the given {@link RuntimeHints} instance.
	 * @param hints the hints contributed so far for the deployment unit
	 */
	public void registerHints(RuntimeHints hints) {
		Set<Class<?>> compiledWithoutParameters = new HashSet<>();
		for (Bindable<?> bindable : this.bindables) {
			new Processor(bindable, compiledWithoutParameters).process(hints.reflection());
		}
		if (!compiledWithoutParameters.isEmpty()) {
			throw new MissingParametersCompilerArgumentException(compiledWithoutParameters);
		}
	}

	/**
	 * Create a new {@link BindableRuntimeHintsRegistrar} for the specified types.
	 * @param types the types to process
	 * @return a new {@link BindableRuntimeHintsRegistrar} instance
	 */
	public static BindableRuntimeHintsRegistrar forTypes(Iterable<Class<?>> types) {
		Assert.notNull(types, "Types must not be null");
		return forTypes(StreamSupport.stream(types.spliterator(), false).toArray(Class<?>[]::new));
	}

	/**
	 * Create a new {@link BindableRuntimeHintsRegistrar} for the specified types.
	 * @param types the types to process
	 * @return a new {@link BindableRuntimeHintsRegistrar} instance
	 */
	public static BindableRuntimeHintsRegistrar forTypes(Class<?>... types) {
		return new BindableRuntimeHintsRegistrar(types);
	}

	/**
	 * Create a new {@link BindableRuntimeHintsRegistrar} for the specified bindables.
	 * @param bindables the bindables to process
	 * @return a new {@link BindableRuntimeHintsRegistrar} instance
	 * @since 3.0.8
	 */
	public static BindableRuntimeHintsRegistrar forBindables(Iterable<Bindable<?>> bindables) {
		Assert.notNull(bindables, "Bindables must not be null");
		return forBindables(StreamSupport.stream(bindables.spliterator(), false).toArray(Bindable[]::new));
	}

	/**
	 * Create a new {@link BindableRuntimeHintsRegistrar} for the specified bindables.
	 * @param bindables the bindables to process
	 * @return a new {@link BindableRuntimeHintsRegistrar} instance
	 * @since 3.0.8
	 */
	public static BindableRuntimeHintsRegistrar forBindables(Bindable<?>... bindables) {
		return new BindableRuntimeHintsRegistrar(bindables);
	}

	/**
	 * Processor used to register the hints.
	 */
	private final class Processor {

		private static final ParameterNameDiscoverer parameterNameDiscoverer;

		static {
			PrioritizedParameterNameDiscoverer discoverer = new PrioritizedParameterNameDiscoverer();
			if (KotlinDetector.isKotlinReflectPresent()) {
				discoverer.addDiscoverer(new KotlinReflectionParameterNameDiscoverer());
			}
			discoverer.addDiscoverer(new StandardReflectionParameterNameDiscoverer());
			parameterNameDiscoverer = discoverer;
		}

		private final Class<?> type;

		private final Constructor<?> bindConstructor;

		private final BeanProperties bean;

		private final Set<Class<?>> seen;

		private final Set<Class<?>> compiledWithoutParameters;

		/**
		 * Constructs a new Processor object with the specified bindable,
		 * compiledWithoutParameters, and default values.
		 * @param bindable The bindable object to be used.
		 * @param compiledWithoutParameters The set of classes compiled without
		 * parameters.
		 */
		Processor(Bindable<?> bindable, Set<Class<?>> compiledWithoutParameters) {
			this(bindable, false, new HashSet<>(), compiledWithoutParameters);
		}

		/**
		 * Constructs a new Processor object.
		 * @param bindable the bindable object to be processed
		 * @param nestedType true if the bindable object is a nested type, false otherwise
		 * @param seen a set of classes that have already been processed
		 * @param compiledWithoutParameters a set of classes that have been compiled
		 * without parameters
		 */
		private Processor(Bindable<?> bindable, boolean nestedType, Set<Class<?>> seen,
				Set<Class<?>> compiledWithoutParameters) {
			this.type = bindable.getType().getRawClass();
			this.bindConstructor = (bindable.getBindMethod() != BindMethod.JAVA_BEAN)
					? BindConstructorProvider.DEFAULT.getBindConstructor(bindable.getType().resolve(), nestedType)
					: null;
			this.bean = JavaBeanBinder.BeanProperties.of(bindable);
			this.seen = seen;
			this.compiledWithoutParameters = compiledWithoutParameters;
		}

		/**
		 * Processes the given reflection hints.
		 * @param hints the reflection hints to be processed
		 */
		void process(ReflectionHints hints) {
			if (this.seen.contains(this.type)) {
				return;
			}
			this.seen.add(this.type);
			handleConstructor(hints);
			if (this.bindConstructor != null) {
				handleValueObjectProperties(hints);
			}
			else if (this.bean != null && !this.bean.getProperties().isEmpty()) {
				handleJavaBeanProperties(hints);
			}
		}

		/**
		 * Handles the constructor of the Processor class.
		 * @param hints the ReflectionHints object containing hints for reflection
		 */
		private void handleConstructor(ReflectionHints hints) {
			if (this.bindConstructor != null) {
				verifyParameterNamesAreAvailable();
				if (KotlinDetector.isKotlinType(this.bindConstructor.getDeclaringClass())) {
					KotlinDelegate.handleConstructor(hints, this.bindConstructor);
				}
				else {
					hints.registerConstructor(this.bindConstructor, ExecutableMode.INVOKE);
				}
				return;
			}
			Arrays.stream(this.type.getDeclaredConstructors())
				.filter(this::hasNoParameters)
				.findFirst()
				.ifPresent((constructor) -> hints.registerConstructor(constructor, ExecutableMode.INVOKE));
		}

		/**
		 * Verifies if parameter names are available for the bindConstructor. If parameter
		 * names are not available, the bindConstructor is added to the
		 * compiledWithoutParameters list.
		 */
		private void verifyParameterNamesAreAvailable() {
			String[] parameterNames = parameterNameDiscoverer.getParameterNames(this.bindConstructor);
			if (parameterNames == null) {
				this.compiledWithoutParameters.add(this.bindConstructor.getDeclaringClass());
			}
		}

		/**
		 * Checks if the given constructor has no parameters.
		 * @param candidate the constructor to be checked
		 * @return true if the constructor has no parameters, false otherwise
		 */
		private boolean hasNoParameters(Constructor<?> candidate) {
			return candidate.getParameterCount() == 0;
		}

		/**
		 * Handles the properties of a value object based on the given reflection hints.
		 * @param hints the reflection hints to be used for handling the properties
		 */
		private void handleValueObjectProperties(ReflectionHints hints) {
			for (int i = 0; i < this.bindConstructor.getParameterCount(); i++) {
				String propertyName = this.bindConstructor.getParameters()[i].getName();
				ResolvableType propertyType = ResolvableType.forConstructorParameter(this.bindConstructor, i);
				handleProperty(hints, propertyName, propertyType);
			}
		}

		/**
		 * Handles the JavaBean properties of the Processor class.
		 * @param hints the reflection hints to be used
		 */
		private void handleJavaBeanProperties(ReflectionHints hints) {
			Map<String, BeanProperty> properties = this.bean.getProperties();
			properties.forEach((name, property) -> {
				Method getter = property.getGetter();
				if (getter != null) {
					hints.registerMethod(getter, ExecutableMode.INVOKE);
				}
				Method setter = property.getSetter();
				if (setter != null) {
					hints.registerMethod(setter, ExecutableMode.INVOKE);
				}
				handleProperty(hints, name, property.getType());
			});
		}

		/**
		 * Handles a property with the given hints, property name, and property type.
		 * @param hints The reflection hints for the property.
		 * @param propertyName The name of the property.
		 * @param propertyType The type of the property.
		 */
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

		/**
		 * Processes the nested class with the given type and reflection hints.
		 * @param type the nested class to be processed
		 * @param hints the reflection hints to be used during processing
		 */
		private void processNested(Class<?> type, ReflectionHints hints) {
			new Processor(Bindable.of(type), true, this.seen, this.compiledWithoutParameters).process(hints);
		}

		/**
		 * Returns the component class for the given ResolvableType.
		 * @param type the ResolvableType to get the component class for
		 * @return the component class, or null if not found
		 */
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

		/**
		 * Returns the component type of the given ResolvableType.
		 * @param type the ResolvableType to get the component type from
		 * @return the component type of the given ResolvableType, or null if the type is
		 * not an array, collection, or map
		 */
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

		/**
		 * Checks if the given ResolvableType is a container type. A container type can be
		 * an array, a collection, or a map.
		 * @param type the ResolvableType to check
		 * @return true if the ResolvableType is a container type, false otherwise
		 */
		private boolean isContainer(ResolvableType type) {
			return type.isArray() || isCollection(type) || isMap(type);
		}

		/**
		 * Checks if the given ResolvableType is a Collection or a subtype of Collection.
		 * @param type the ResolvableType to be checked
		 * @return true if the ResolvableType is a Collection or a subtype of Collection,
		 * false otherwise
		 */
		private boolean isCollection(ResolvableType type) {
			return Collection.class.isAssignableFrom(type.toClass());
		}

		/**
		 * Checks if the given ResolvableType is a Map or a subtype of Map.
		 * @param type the ResolvableType to be checked
		 * @return true if the ResolvableType is a Map or a subtype of Map, false
		 * otherwise
		 */
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
			Class<?> declaringClass = propertyType.getDeclaringClass();
			if (declaringClass != null && isNested(declaringClass, this.type)) {
				return true;
			}
			Field field = ReflectionUtils.findField(this.type, propertyName);
			return (field != null) && MergedAnnotations.from(field).isPresent(Nested.class);
		}

		/**
		 * Checks if a given class is nested within another class.
		 * @param type the class to check if it is nested within another class
		 * @param candidate the class to check if it is the declaring class of the nested
		 * class
		 * @return true if the given class is nested within another class, false otherwise
		 */
		private static boolean isNested(Class<?> type, Class<?> candidate) {
			if (type.isAssignableFrom(candidate)) {
				return true;
			}
			return (candidate.getDeclaringClass() != null && isNested(type, candidate.getDeclaringClass()));
		}

		/**
		 * Checks if the given class is a Java type.
		 * @param candidate the class to be checked
		 * @return true if the class is a Java type, false otherwise
		 */
		private boolean isJavaType(Class<?> candidate) {
			return candidate.getPackageName().startsWith("java.");
		}

	}

	/**
	 * Inner class to avoid a hard dependency on Kotlin at runtime.
	 */
	private static final class KotlinDelegate {

		/**
		 * Handles the constructor of a class by registering it with the given reflection
		 * hints.
		 * @param hints the reflection hints to register the constructor with
		 * @param constructor the constructor to handle
		 */
		static void handleConstructor(ReflectionHints hints, Constructor<?> constructor) {
			KClass<?> kClass = JvmClassMappingKt.getKotlinClass(constructor.getDeclaringClass());
			if (kClass.isData()) {
				hints.registerType(constructor.getDeclaringClass(), MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
			}
			else {
				hints.registerConstructor(constructor, ExecutableMode.INVOKE);
			}
		}

	}

}
