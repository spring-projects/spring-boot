/*
 * Copyright 2012-2023 the original author or authors.
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

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.bind.Binder.Context;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertyState;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;

/**
 * {@link DataObjectBinder} for mutable Java Beans.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class JavaBeanBinder implements DataObjectBinder {

	static final JavaBeanBinder INSTANCE = new JavaBeanBinder();

	/**
	 * Binds the given configuration property name to the target bindable object using the
	 * provided property binder.
	 * @param name The configuration property name to bind.
	 * @param target The bindable object to bind the configuration property to.
	 * @param context The context for the binding operation.
	 * @param propertyBinder The property binder to use for binding.
	 * @return The bound object if the binding was successful, otherwise null.
	 */
	@Override
	public <T> T bind(ConfigurationPropertyName name, Bindable<T> target, Context context,
			DataObjectPropertyBinder propertyBinder) {
		boolean hasKnownBindableProperties = target.getValue() != null && hasKnownBindableProperties(name, context);
		Bean<T> bean = Bean.get(target, hasKnownBindableProperties);
		if (bean == null) {
			return null;
		}
		BeanSupplier<T> beanSupplier = bean.getSupplier(target);
		boolean bound = bind(propertyBinder, bean, beanSupplier, context);
		return (bound ? beanSupplier.get() : null);
	}

	/**
	 * Creates an instance of the specified target type using the provided context.
	 * @param target the target type to create an instance of
	 * @param context the context used for creating the instance
	 * @return an instance of the target type, or null if the target type cannot be
	 * resolved
	 * @throws IllegalArgumentException if the target type is not a valid bindable type
	 * @throws BeanInstantiationException if an error occurs while instantiating the
	 * target type
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> T create(Bindable<T> target, Context context) {
		Class<T> type = (Class<T>) target.getType().resolve();
		return (type != null) ? BeanUtils.instantiateClass(type) : null;
	}

	/**
	 * Checks if the given ConfigurationPropertyName has any known bindable properties in
	 * the provided Context.
	 * @param name the ConfigurationPropertyName to check
	 * @param context the Context containing the ConfigurationPropertySources to search in
	 * @return true if the ConfigurationPropertyName has known bindable properties, false
	 * otherwise
	 */
	private boolean hasKnownBindableProperties(ConfigurationPropertyName name, Context context) {
		for (ConfigurationPropertySource source : context.getSources()) {
			if (source.containsDescendantOf(name) == ConfigurationPropertyState.PRESENT) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Binds the properties of a bean using the provided property binder and bean
	 * supplier.
	 * @param propertyBinder the data object property binder
	 * @param bean the bean to bind
	 * @param beanSupplier the bean supplier
	 * @param context the context
	 * @return true if any properties were bound, false otherwise
	 */
	private <T> boolean bind(DataObjectPropertyBinder propertyBinder, Bean<T> bean, BeanSupplier<T> beanSupplier,
			Context context) {
		boolean bound = false;
		for (BeanProperty beanProperty : bean.getProperties().values()) {
			bound |= bind(beanSupplier, propertyBinder, beanProperty);
			context.clearConfigurationProperty();
		}
		return bound;
	}

	/**
	 * Binds a property of a JavaBean using the provided bean supplier, property binder,
	 * and bean property.
	 * @param <T> the type of the bean
	 * @param beanSupplier the supplier for the bean
	 * @param propertyBinder the property binder to use for binding the property
	 * @param property the bean property to bind
	 * @return true if the property was successfully bound, false otherwise
	 * @throws IllegalStateException if no setter is found for the property and the value
	 * is not null or does not match the current value
	 */
	private <T> boolean bind(BeanSupplier<T> beanSupplier, DataObjectPropertyBinder propertyBinder,
			BeanProperty property) {
		String propertyName = property.getName();
		ResolvableType type = property.getType();
		Supplier<Object> value = property.getValue(beanSupplier);
		Annotation[] annotations = property.getAnnotations();
		Object bound = propertyBinder.bindProperty(propertyName,
				Bindable.of(type).withSuppliedValue(value).withAnnotations(annotations));
		if (bound == null) {
			return false;
		}
		if (property.isSettable()) {
			property.setValue(beanSupplier, bound);
		}
		else if (value == null || !bound.equals(value.get())) {
			throw new IllegalStateException("No setter found for property: " + property.getName());
		}
		return true;
	}

	/**
	 * The properties of a bean that may be bound.
	 */
	static class BeanProperties {

		private final Map<String, BeanProperty> properties = new LinkedHashMap<>();

		private final ResolvableType type;

		private final Class<?> resolvedType;

		/**
		 * Constructs a new instance of BeanProperties with the specified ResolvableType
		 * and resolvedType.
		 * @param type the ResolvableType representing the type of the bean
		 * @param resolvedType the Class representing the resolved type of the bean
		 */
		BeanProperties(ResolvableType type, Class<?> resolvedType) {
			this.type = type;
			this.resolvedType = resolvedType;
			addProperties(resolvedType);
		}

		/**
		 * Adds properties to the given class and its superclasses.
		 * @param type the class to add properties to
		 */
		private void addProperties(Class<?> type) {
			while (type != null && !Object.class.equals(type)) {
				Method[] declaredMethods = getSorted(type, this::getDeclaredMethods, Method::getName);
				Field[] declaredFields = getSorted(type, Class::getDeclaredFields, Field::getName);
				addProperties(declaredMethods, declaredFields);
				type = type.getSuperclass();
			}
		}

		/**
		 * Retrieves an array of all declared methods of the specified class.
		 * @param type the class for which to retrieve the declared methods
		 * @return an array of all declared methods of the specified class
		 */
		private Method[] getDeclaredMethods(Class<?> type) {
			Method[] methods = type.getDeclaredMethods();
			Set<Method> result = new LinkedHashSet<>(methods.length);
			for (Method method : methods) {
				result.add(BridgeMethodResolver.findBridgedMethod(method));
			}
			return result.toArray(new Method[0]);
		}

		/**
		 * Returns an array of elements sorted in ascending order based on the provided
		 * name function.
		 * @param <S> the type of the source object
		 * @param <E> the type of the elements in the array
		 * @param source the source object from which to retrieve the elements
		 * @param elements a function that retrieves an array of elements from the source
		 * object
		 * @param name a function that maps an element to its name
		 * @return an array of elements sorted in ascending order based on the provided
		 * name function
		 */
		private <S, E> E[] getSorted(S source, Function<S, E[]> elements, Function<E, String> name) {
			E[] result = elements.apply(source);
			Arrays.sort(result, Comparator.comparing(name));
			return result;
		}

		/**
		 * Adds properties to the BeanProperties class.
		 * @param declaredMethods an array of Method objects representing the declared
		 * methods
		 * @param declaredFields an array of Field objects representing the declared
		 * fields
		 */
		protected void addProperties(Method[] declaredMethods, Field[] declaredFields) {
			for (int i = 0; i < declaredMethods.length; i++) {
				if (!isCandidate(declaredMethods[i])) {
					declaredMethods[i] = null;
				}
			}
			for (Method method : declaredMethods) {
				addMethodIfPossible(method, "is", 0, BeanProperty::addGetter);
			}
			for (Method method : declaredMethods) {
				addMethodIfPossible(method, "get", 0, BeanProperty::addGetter);
			}
			for (Method method : declaredMethods) {
				addMethodIfPossible(method, "set", 1, BeanProperty::addSetter);
			}
			for (Field field : declaredFields) {
				addField(field);
			}
		}

		/**
		 * Checks if the given method is a valid candidate for processing.
		 * @param method the method to be checked
		 * @return true if the method is a valid candidate, false otherwise
		 */
		private boolean isCandidate(Method method) {
			int modifiers = method.getModifiers();
			return !Modifier.isPrivate(modifiers) && !Modifier.isProtected(modifiers) && !Modifier.isAbstract(modifiers)
					&& !Modifier.isStatic(modifiers) && !method.isBridge()
					&& !Object.class.equals(method.getDeclaringClass())
					&& !Class.class.equals(method.getDeclaringClass()) && method.getName().indexOf('$') == -1;
		}

		/**
		 * Adds a method to the BeanProperties if it meets the specified conditions.
		 * @param method the method to be added
		 * @param prefix the prefix that the method name should start with
		 * @param parameterCount the number of parameters the method should have
		 * @param consumer the consumer function to be applied if the method is added
		 */
		private void addMethodIfPossible(Method method, String prefix, int parameterCount,
				BiConsumer<BeanProperty, Method> consumer) {
			if (method != null && method.getParameterCount() == parameterCount && method.getName().startsWith(prefix)
					&& method.getName().length() > prefix.length()) {
				String propertyName = Introspector.decapitalize(method.getName().substring(prefix.length()));
				consumer.accept(this.properties.computeIfAbsent(propertyName, this::getBeanProperty), method);
			}
		}

		/**
		 * Returns a BeanProperty object with the specified name and type.
		 * @param name the name of the bean property
		 * @return a BeanProperty object
		 */
		private BeanProperty getBeanProperty(String name) {
			return new BeanProperty(name, this.type);
		}

		/**
		 * Adds a field to the BeanProperties object.
		 * @param field the Field object to be added
		 */
		private void addField(Field field) {
			BeanProperty property = this.properties.get(field.getName());
			if (property != null) {
				property.addField(field);
			}
		}

		/**
		 * Returns the ResolvableType of the current instance.
		 * @return the ResolvableType of the current instance
		 */
		protected final ResolvableType getType() {
			return this.type;
		}

		/**
		 * Returns the resolved type of the bean property.
		 * @return the resolved type of the bean property
		 */
		protected final Class<?> getResolvedType() {
			return this.resolvedType;
		}

		/**
		 * Returns the properties of the BeanProperties object.
		 * @return a Map containing the properties of the BeanProperties object
		 */
		final Map<String, BeanProperty> getProperties() {
			return this.properties;
		}

		/**
		 * Returns the BeanProperties object for the given Bindable.
		 * @param bindable the Bindable object for which to retrieve the BeanProperties
		 * @return the BeanProperties object
		 */
		static BeanProperties of(Bindable<?> bindable) {
			ResolvableType type = bindable.getType();
			Class<?> resolvedType = type.resolve(Object.class);
			return new BeanProperties(type, resolvedType);
		}

	}

	/**
	 * The bean being bound.
	 *
	 * @param <T> the bean type
	 */
	static class Bean<T> extends BeanProperties {

		private static Bean<?> cached;

		/**
		 * Constructs a new Bean instance with the specified type and resolved type.
		 * @param type the ResolvableType of the bean
		 * @param resolvedType the Class representing the resolved type of the bean
		 */
		Bean(ResolvableType type, Class<?> resolvedType) {
			super(type, resolvedType);
		}

		/**
		 * Returns a BeanSupplier for the given Bindable target.
		 * @param target the Bindable target
		 * @return a BeanSupplier for the given Bindable target
		 */
		@SuppressWarnings("unchecked")
		BeanSupplier<T> getSupplier(Bindable<T> target) {
			return new BeanSupplier<>(() -> {
				T instance = null;
				if (target.getValue() != null) {
					instance = target.getValue().get();
				}
				if (instance == null) {
					instance = (T) BeanUtils.instantiateClass(getResolvedType());
				}
				return instance;
			});
		}

		/**
		 * Retrieves a Bean instance based on the provided Bindable and canCallGetValue
		 * parameters.
		 * @param bindable The Bindable object representing the type of the Bean.
		 * @param canCallGetValue A boolean indicating whether the getValue method can be
		 * called on the Bindable object.
		 * @param <T> The type of the Bean.
		 * @return The Bean instance.
		 */
		@SuppressWarnings("unchecked")
		static <T> Bean<T> get(Bindable<T> bindable, boolean canCallGetValue) {
			ResolvableType type = bindable.getType();
			Class<?> resolvedType = type.resolve(Object.class);
			Supplier<T> value = bindable.getValue();
			T instance = null;
			if (canCallGetValue && value != null) {
				instance = value.get();
				resolvedType = (instance != null) ? instance.getClass() : resolvedType;
			}
			if (instance == null && !isInstantiable(resolvedType)) {
				return null;
			}
			Bean<?> bean = Bean.cached;
			if (bean == null || !bean.isOfType(type, resolvedType)) {
				bean = new Bean<>(type, resolvedType);
				cached = bean;
			}
			return (Bean<T>) bean;
		}

		/**
		 * Checks if a given class is instantiable.
		 * @param type the class to check
		 * @return true if the class is instantiable, false otherwise
		 */
		private static boolean isInstantiable(Class<?> type) {
			if (type.isInterface()) {
				return false;
			}
			try {
				type.getDeclaredConstructor();
				return true;
			}
			catch (Exception ex) {
				return false;
			}
		}

		/**
		 * Checks if the given ResolvableType is of the specified resolved type.
		 * @param type the ResolvableType to check
		 * @param resolvedType the resolved type to compare against
		 * @return true if the ResolvableType is of the specified resolved type, false
		 * otherwise
		 */
		private boolean isOfType(ResolvableType type, Class<?> resolvedType) {
			if (getType().hasGenerics() || type.hasGenerics()) {
				return getType().equals(type);
			}
			return getResolvedType() != null && getResolvedType().equals(resolvedType);
		}

	}

	/**
	 * BeanSupplier class.
	 */
	private static class BeanSupplier<T> implements Supplier<T> {

		private final Supplier<T> factory;

		private T instance;

		/**
		 * Constructs a new BeanSupplier with the specified factory.
		 * @param factory the supplier function used to create new instances of type T
		 */
		BeanSupplier(Supplier<T> factory) {
			this.factory = factory;
		}

		/**
		 * Returns the instance of the bean. If the instance is null, it creates a new
		 * instance using the factory and returns it.
		 * @return the instance of the bean
		 */
		@Override
		public T get() {
			if (this.instance == null) {
				this.instance = this.factory.get();
			}
			return this.instance;
		}

	}

	/**
	 * A bean property being bound.
	 */
	static class BeanProperty {

		private final String name;

		private final ResolvableType declaringClassType;

		private Method getter;

		private Method setter;

		private Field field;

		/**
		 * Constructs a new BeanProperty with the specified name and declaring class type.
		 * @param name the name of the property
		 * @param declaringClassType the ResolvableType representing the declaring class
		 * of the property
		 */
		BeanProperty(String name, ResolvableType declaringClassType) {
			this.name = DataObjectPropertyName.toDashedForm(name);
			this.declaringClassType = declaringClassType;
		}

		/**
		 * Adds a getter method to the BeanProperty.
		 * @param getter the getter method to be added
		 */
		void addGetter(Method getter) {
			if (this.getter == null || this.getter.getName().startsWith("is")) {
				this.getter = getter;
			}
		}

		/**
		 * Adds a setter method to the BeanProperty.
		 * @param setter the setter method to be added
		 */
		void addSetter(Method setter) {
			if (this.setter == null || isBetterSetter(setter)) {
				this.setter = setter;
			}
		}

		/**
		 * Checks if the given setter method is a better setter than the existing getter
		 * method.
		 * @param setter the setter method to be checked
		 * @return true if the setter method is a better setter, false otherwise
		 */
		private boolean isBetterSetter(Method setter) {
			return this.getter != null && this.getter.getReturnType().equals(setter.getParameterTypes()[0]);
		}

		/**
		 * Adds a field to the BeanProperty.
		 * @param field the field to be added
		 */
		void addField(Field field) {
			if (this.field == null) {
				this.field = field;
			}
		}

		/**
		 * Returns the name of the BeanProperty.
		 * @return the name of the BeanProperty
		 */
		String getName() {
			return this.name;
		}

		/**
		 * Returns the ResolvableType of the property.
		 *
		 * If a setter method is present, the ResolvableType is determined using the first
		 * parameter of the setter method. Otherwise, the ResolvableType is determined
		 * using the getter method with a parameter index of -1.
		 * @return the ResolvableType of the property
		 */
		ResolvableType getType() {
			if (this.setter != null) {
				MethodParameter methodParameter = new MethodParameter(this.setter, 0);
				return ResolvableType.forMethodParameter(methodParameter, this.declaringClassType);
			}
			MethodParameter methodParameter = new MethodParameter(this.getter, -1);
			return ResolvableType.forMethodParameter(methodParameter, this.declaringClassType);
		}

		/**
		 * Returns an array of annotations declared on the field associated with this
		 * BeanProperty.
		 * @return an array of annotations declared on the field, or null if the field is
		 * null or an exception occurs
		 */
		Annotation[] getAnnotations() {
			try {
				return (this.field != null) ? this.field.getDeclaredAnnotations() : null;
			}
			catch (Exception ex) {
				return null;
			}
		}

		/**
		 * Returns a Supplier that retrieves the value of a property using reflection.
		 * @param instance the Supplier instance that provides the object containing the
		 * property
		 * @return a Supplier that retrieves the value of the property
		 * @throws IllegalStateException if unable to get the value for the property
		 */
		Supplier<Object> getValue(Supplier<?> instance) {
			if (this.getter == null) {
				return null;
			}
			return () -> {
				try {
					this.getter.setAccessible(true);
					return this.getter.invoke(instance.get());
				}
				catch (Exception ex) {
					if (isUninitializedKotlinProperty(ex)) {
						return null;
					}
					throw new IllegalStateException("Unable to get value for property " + this.name, ex);
				}
			};
		}

		/**
		 * Checks if the given exception is an uninitialized Kotlin property exception.
		 * @param ex the exception to check
		 * @return true if the exception is an uninitialized Kotlin property exception,
		 * false otherwise
		 */
		private boolean isUninitializedKotlinProperty(Exception ex) {
			return (ex instanceof InvocationTargetException invocationTargetException)
					&& "kotlin.UninitializedPropertyAccessException"
						.equals(invocationTargetException.getTargetException().getClass().getName());
		}

		/**
		 * Returns a boolean value indicating whether the property is settable.
		 * @return true if the property is settable, false otherwise
		 */
		boolean isSettable() {
			return this.setter != null;
		}

		/**
		 * Sets the value of the property using the provided instance and value.
		 * @param instance the supplier that provides the instance of the object
		 * @param value the value to be set for the property
		 * @throws IllegalStateException if unable to set the value for the property
		 */
		void setValue(Supplier<?> instance, Object value) {
			try {
				this.setter.setAccessible(true);
				this.setter.invoke(instance.get(), value);
			}
			catch (Exception ex) {
				throw new IllegalStateException("Unable to set value for property " + this.name, ex);
			}
		}

		/**
		 * Returns the getter method of the BeanProperty.
		 * @return the getter method of the BeanProperty
		 */
		Method getGetter() {
			return this.getter;
		}

		/**
		 * Returns the setter method associated with this BeanProperty.
		 * @return the setter method associated with this BeanProperty
		 */
		Method getSetter() {
			return this.setter;
		}

	}

}
