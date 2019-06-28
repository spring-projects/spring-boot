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

package org.springframework.boot.context.properties.bind;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.bind.Binder.Context;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertyState;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;

/**
 * {@link DataObjectBinder} for mutable Java Beans.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class JavaBeanBinder implements DataObjectBinder {

	@Override
	public <T> T bind(ConfigurationPropertyName name, Bindable<T> target, Context context,
			DataObjectPropertyBinder propertyBinder) {
		boolean hasKnownBindableProperties = hasKnownBindableProperties(name, context);
		Bean<T> bean = Bean.get(target, hasKnownBindableProperties);
		if (bean == null) {
			return null;
		}
		BeanSupplier<T> beanSupplier = bean.getSupplier(target);
		boolean bound = bind(propertyBinder, bean, beanSupplier);
		return (bound ? beanSupplier.get() : null);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T create(Bindable<T> target, Context context) {
		Class<T> type = (Class<T>) target.getType().resolve();
		return (type != null) ? BeanUtils.instantiateClass(type) : null;
	}

	private boolean hasKnownBindableProperties(ConfigurationPropertyName name, Context context) {
		for (ConfigurationPropertySource source : context.getSources()) {
			if (source.containsDescendantOf(name) == ConfigurationPropertyState.PRESENT) {
				return true;
			}
		}
		return false;
	}

	private <T> boolean bind(DataObjectPropertyBinder propertyBinder, Bean<T> bean, BeanSupplier<T> beanSupplier) {
		boolean bound = false;
		for (BeanProperty beanProperty : bean.getProperties().values()) {
			bound |= bind(beanSupplier, propertyBinder, beanProperty);
		}
		return bound;
	}

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
	 * The bean being bound.
	 *
	 * @param <T> the bean type
	 */
	static class Bean<T> {

		private static Bean<?> cached;

		private final ResolvableType type;

		private final Class<?> resolvedType;

		private final Map<String, BeanProperty> properties = new LinkedHashMap<>();

		Bean(ResolvableType type, Class<?> resolvedType) {
			this.type = type;
			this.resolvedType = resolvedType;
			addProperties(resolvedType);
		}

		private void addProperties(Class<?> type) {
			while (type != null && !Object.class.equals(type)) {
				Method[] declaredMethods = type.getDeclaredMethods();
				Field[] declaredFields = type.getDeclaredFields();
				addProperties(declaredMethods, declaredFields);
				type = type.getSuperclass();
			}
		}

		protected void addProperties(Method[] declaredMethods, Field[] declaredFields) {
			for (int i = 0; i < declaredMethods.length; i++) {
				if (!isCandidate(declaredMethods[i])) {
					declaredMethods[i] = null;
				}
			}
			for (Method method : declaredMethods) {
				addMethodIfPossible(method, "get", 0, BeanProperty::addGetter);
				addMethodIfPossible(method, "is", 0, BeanProperty::addGetter);
			}
			for (Method method : declaredMethods) {
				addMethodIfPossible(method, "set", 1, BeanProperty::addSetter);
			}
			for (Field field : declaredFields) {
				addField(field);
			}
		}

		private boolean isCandidate(Method method) {
			int modifiers = method.getModifiers();
			return Modifier.isPublic(modifiers) && !Modifier.isAbstract(modifiers) && !Modifier.isStatic(modifiers)
					&& !Object.class.equals(method.getDeclaringClass())
					&& !Class.class.equals(method.getDeclaringClass());
		}

		private void addMethodIfPossible(Method method, String prefix, int parameterCount,
				BiConsumer<BeanProperty, Method> consumer) {
			if (method != null && method.getParameterCount() == parameterCount && method.getName().startsWith(prefix)
					&& method.getName().length() > prefix.length()) {
				String propertyName = Introspector.decapitalize(method.getName().substring(prefix.length()));
				consumer.accept(this.properties.computeIfAbsent(propertyName, this::getBeanProperty), method);
			}
		}

		private BeanProperty getBeanProperty(String name) {
			return new BeanProperty(name, this.type);
		}

		private void addField(Field field) {
			BeanProperty property = this.properties.get(field.getName());
			if (property != null) {
				property.addField(field);
			}
		}

		public Map<String, BeanProperty> getProperties() {
			return this.properties;
		}

		@SuppressWarnings("unchecked")
		public BeanSupplier<T> getSupplier(Bindable<T> target) {
			return new BeanSupplier<>(() -> {
				T instance = null;
				if (target.getValue() != null) {
					instance = target.getValue().get();
				}
				if (instance == null) {
					instance = (T) BeanUtils.instantiateClass(this.resolvedType);
				}
				return instance;
			});
		}

		@SuppressWarnings("unchecked")
		public static <T> Bean<T> get(Bindable<T> bindable, boolean canCallGetValue) {
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

		private boolean isOfType(ResolvableType type, Class<?> resolvedType) {
			if (this.type.hasGenerics() || type.hasGenerics()) {
				return this.type.equals(type);
			}
			return this.resolvedType != null && this.resolvedType.equals(resolvedType);
		}

	}

	private static class BeanSupplier<T> implements Supplier<T> {

		private final Supplier<T> factory;

		private T instance;

		BeanSupplier(Supplier<T> factory) {
			this.factory = factory;
		}

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

		BeanProperty(String name, ResolvableType declaringClassType) {
			this.name = DataObjectPropertyName.toDashedForm(name);
			this.declaringClassType = declaringClassType;
		}

		public void addGetter(Method getter) {
			if (this.getter == null) {
				this.getter = getter;
			}
		}

		public void addSetter(Method setter) {
			if (this.setter == null || isBetterSetter(setter)) {
				this.setter = setter;
			}
		}

		private boolean isBetterSetter(Method setter) {
			return this.getter != null && this.getter.getReturnType().equals(setter.getParameterTypes()[0]);
		}

		public void addField(Field field) {
			if (this.field == null) {
				this.field = field;
			}
		}

		public String getName() {
			return this.name;
		}

		public ResolvableType getType() {
			if (this.setter != null) {
				MethodParameter methodParameter = new MethodParameter(this.setter, 0);
				return ResolvableType.forMethodParameter(methodParameter, this.declaringClassType);
			}
			MethodParameter methodParameter = new MethodParameter(this.getter, -1);
			return ResolvableType.forMethodParameter(methodParameter, this.declaringClassType);
		}

		public Annotation[] getAnnotations() {
			try {
				return (this.field != null) ? this.field.getDeclaredAnnotations() : null;
			}
			catch (Exception ex) {
				return null;
			}
		}

		public Supplier<Object> getValue(Supplier<?> instance) {
			if (this.getter == null) {
				return null;
			}
			return () -> {
				try {
					this.getter.setAccessible(true);
					return this.getter.invoke(instance.get());
				}
				catch (Exception ex) {
					throw new IllegalStateException("Unable to get value for property " + this.name, ex);
				}
			};
		}

		public boolean isSettable() {
			return this.setter != null;
		}

		public void setValue(Supplier<?> instance, Object value) {
			try {
				this.setter.setAccessible(true);
				this.setter.invoke(instance.get(), value);
			}
			catch (Exception ex) {
				throw new IllegalStateException("Unable to set value for property " + this.name, ex);
			}
		}

	}

}
