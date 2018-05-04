/*
 * Copyright 2012-2018 the original author or authors.
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
import org.springframework.boot.context.properties.source.ConfigurationPropertyState;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;

/**
 * {@link BeanBinder} for mutable Java Beans.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class JavaBeanBinder implements BeanBinder {

	@Override
	public <T> T bind(ConfigurationPropertyName name, Bindable<T> target, Context context,
			BeanPropertyBinder propertyBinder) {
		boolean hasKnownBindableProperties = context.streamSources().anyMatch((
				s) -> s.containsDescendantOf(name) == ConfigurationPropertyState.PRESENT);
		Bean<T> bean = Bean.get(target, hasKnownBindableProperties);
		if (bean == null) {
			return null;
		}
		BeanSupplier<T> beanSupplier = bean.getSupplier(target);
		boolean bound = bind(propertyBinder, bean, beanSupplier);
		return (bound ? beanSupplier.get() : null);
	}

	private <T> boolean bind(BeanPropertyBinder propertyBinder, Bean<T> bean,
			BeanSupplier<T> beanSupplier) {
		boolean bound = false;
		for (Map.Entry<String, BeanProperty> entry : bean.getProperties().entrySet()) {
			bound |= bind(beanSupplier, propertyBinder, entry.getValue());
		}
		return bound;
	}

	private <T> boolean bind(BeanSupplier<T> beanSupplier,
			BeanPropertyBinder propertyBinder, BeanProperty property) {
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
			throw new IllegalStateException(
					"No setter found for property: " + property.getName());
		}
		return true;
	}

	/**
	 * The bean being bound.
	 */
	private static class Bean<T> {

		private static Bean<?> cached;

		private final Class<?> type;

		private final ResolvableType resolvableType;

		private final Map<String, BeanProperty> properties = new LinkedHashMap<>();

		Bean(ResolvableType resolvableType, Class<?> type) {
			this.resolvableType = resolvableType;
			this.type = type;
			putProperties(type);
		}

		private void putProperties(Class<?> type) {
			while (type != null && !Object.class.equals(type)) {
				for (Method method : type.getDeclaredMethods()) {
					if (isCandidate(method)) {
						addMethod(method);
					}
				}
				for (Field field : type.getDeclaredFields()) {
					addField(field);
				}
				type = type.getSuperclass();
			}
		}

		private boolean isCandidate(Method method) {
			int modifiers = method.getModifiers();
			return Modifier.isPublic(modifiers) && !Modifier.isAbstract(modifiers)
					&& !Modifier.isStatic(modifiers)
					&& !Object.class.equals(method.getDeclaringClass())
					&& !Class.class.equals(method.getDeclaringClass());
		}

		private void addMethod(Method method) {
			addMethodIfPossible(method, "get", 0, BeanProperty::addGetter);
			addMethodIfPossible(method, "is", 0, BeanProperty::addGetter);
			addMethodIfPossible(method, "set", 1, BeanProperty::addSetter);
		}

		private void addMethodIfPossible(Method method, String prefix, int parameterCount,
				BiConsumer<BeanProperty, Method> consumer) {
			if (method.getParameterCount() == parameterCount
					&& method.getName().startsWith(prefix)
					&& method.getName().length() > prefix.length()) {
				String propertyName = Introspector
						.decapitalize(method.getName().substring(prefix.length()));
				consumer.accept(this.properties.computeIfAbsent(propertyName,
						this::getBeanProperty), method);
			}
		}

		private BeanProperty getBeanProperty(String name) {
			return new BeanProperty(name, this.resolvableType);
		}

		private void addField(Field field) {
			BeanProperty property = this.properties.get(field.getName());
			if (property != null) {
				property.addField(field);
			}
		}

		public Class<?> getType() {
			return this.type;
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
					instance = (T) BeanUtils.instantiateClass(this.type);
				}
				return instance;
			});
		}

		@SuppressWarnings("unchecked")
		public static <T> Bean<T> get(Bindable<T> bindable, boolean canCallGetValue) {
			Class<?> type = bindable.getType().resolve(Object.class);
			Supplier<T> value = bindable.getValue();
			T instance = null;
			if (canCallGetValue && value != null) {
				instance = value.get();
				type = (instance != null ? instance.getClass() : type);
			}
			if (instance == null && !isInstantiable(type)) {
				return null;
			}
			Bean<?> bean = Bean.cached;
			if (bean == null || !type.equals(bean.getType())) {
				bean = new Bean<>(bindable.getType(), type);
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
	private static class BeanProperty {

		private final String name;

		private final ResolvableType declaringClassType;

		private Method getter;

		private Method setter;

		private Field field;

		BeanProperty(String name, ResolvableType declaringClassType) {
			this.name = BeanPropertyName.toDashedForm(name);
			this.declaringClassType = declaringClassType;
		}

		public void addGetter(Method getter) {
			if (this.getter == null) {
				this.getter = getter;
			}
		}

		public void addSetter(Method setter) {
			if (this.setter == null) {
				this.setter = setter;
			}
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
				return ResolvableType.forMethodParameter(methodParameter,
						this.declaringClassType);
			}
			MethodParameter methodParameter = new MethodParameter(this.getter, -1);
			return ResolvableType.forMethodParameter(methodParameter,
					this.declaringClassType);
		}

		public Annotation[] getAnnotations() {
			try {
				return (this.field == null ? null : this.field.getDeclaredAnnotations());
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
					throw new IllegalStateException(
							"Unable to get value for property " + this.name, ex);
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
				throw new IllegalStateException(
						"Unable to set value for property " + this.name, ex);
			}
		}

	}

}
