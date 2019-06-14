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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.jvm.ReflectJvmMapping;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.KotlinDetector;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

/**
 * {@link DataObjectBinder} for immutable value objects.
 *
 * @author Madhura Bhave
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class ValueObjectBinder implements DataObjectBinder {

	@Override
	public <T> T bind(ConfigurationPropertyName name, Bindable<T> target, Binder.Context context,
			DataObjectPropertyBinder propertyBinder) {
		ValueObject<T> valueObject = ValueObject.get(target);
		if (valueObject == null) {
			return null;
		}
		List<ConstructorParameter> parameters = valueObject.getConstructorParameters();
		List<Object> args = new ArrayList<>(parameters.size());
		boolean bound = false;
		for (ConstructorParameter parameter : parameters) {
			Object arg = parameter.bind(propertyBinder);
			bound = bound || arg != null;
			arg = (arg != null) ? arg : parameter.getDefaultValue(context.getConverter());
			args.add(arg);
		}
		return bound ? valueObject.instantiate(args) : null;
	}

	@Override
	public <T> T create(Bindable<T> target, Binder.Context context) {
		ValueObject<T> valueObject = ValueObject.get(target);
		if (valueObject == null) {
			return null;
		}
		List<ConstructorParameter> parameters = valueObject.getConstructorParameters();
		List<Object> args = new ArrayList<>(parameters.size());
		for (ConstructorParameter parameter : parameters) {
			args.add(parameter.getDefaultValue(context.getConverter()));
		}
		return valueObject.instantiate(args);
	}

	/**
	 * The value object being bound.
	 *
	 * @param <T> the value object type
	 */
	private abstract static class ValueObject<T> {

		private final Constructor<T> constructor;

		protected ValueObject(Constructor<T> constructor) {
			this.constructor = constructor;
		}

		public T instantiate(List<Object> args) {
			return BeanUtils.instantiateClass(this.constructor, args.toArray());
		}

		public abstract List<ConstructorParameter> getConstructorParameters();

		@SuppressWarnings("unchecked")
		public static <T> ValueObject<T> get(Bindable<T> bindable) {
			if (bindable.getValue() != null) {
				return null;
			}
			Class<T> type = (Class<T>) bindable.getType().resolve();
			if (type == null || type.isEnum() || Modifier.isAbstract(type.getModifiers())) {
				return null;
			}
			if (KotlinDetector.isKotlinType(type)) {
				return KotlinValueObject.get(type);
			}
			return DefaultValueObject.get(type);
		}

	}

	/**
	 * A {@link ValueObject} implementation that is aware of Kotlin specific constructs.
	 */
	private static final class KotlinValueObject<T> extends ValueObject<T> {

		private final List<ConstructorParameter> constructorParameters;

		private KotlinValueObject(Constructor<T> primaryConstructor, KFunction<T> kotlinConstructor) {
			super(primaryConstructor);
			this.constructorParameters = parseConstructorParameters(kotlinConstructor);
		}

		private List<ConstructorParameter> parseConstructorParameters(KFunction<T> kotlinConstructor) {
			List<KParameter> parameters = kotlinConstructor.getParameters();
			List<ConstructorParameter> result = new ArrayList<>(parameters.size());
			for (KParameter parameter : parameters) {
				String name = parameter.getName();
				ResolvableType type = ResolvableType.forType(ReflectJvmMapping.getJavaType(parameter.getType()));
				Annotation[] annotations = parameter.getAnnotations().toArray(new Annotation[0]);
				result.add(new ConstructorParameter(name, type, annotations));
			}
			return Collections.unmodifiableList(result);
		}

		@Override
		public List<ConstructorParameter> getConstructorParameters() {
			return this.constructorParameters;
		}

		public static <T> ValueObject<T> get(Class<T> type) {
			Constructor<T> primaryConstructor = BeanUtils.findPrimaryConstructor(type);
			if (primaryConstructor == null || primaryConstructor.getParameterCount() == 0) {
				return null;
			}
			KFunction<T> kotlinConstructor = ReflectJvmMapping.getKotlinFunction(primaryConstructor);
			if (kotlinConstructor != null) {
				return new KotlinValueObject<>(primaryConstructor, kotlinConstructor);
			}
			return DefaultValueObject.get(primaryConstructor);
		}

	}

	/**
	 * A default {@link ValueObject} implementation that uses only standard Java
	 * reflection calls.
	 */
	private static final class DefaultValueObject<T> extends ValueObject<T> {

		private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

		private final List<ConstructorParameter> constructorParameters;

		private DefaultValueObject(Constructor<T> constructor) {
			super(constructor);
			this.constructorParameters = parseConstructorParameters(constructor);
		}

		private static List<ConstructorParameter> parseConstructorParameters(Constructor<?> constructor) {
			String[] names = PARAMETER_NAME_DISCOVERER.getParameterNames(constructor);
			Assert.state(names != null, () -> "Failed to extract parameter names for " + constructor);
			Parameter[] parameters = constructor.getParameters();
			List<ConstructorParameter> result = new ArrayList<>(parameters.length);
			for (int i = 0; i < parameters.length; i++) {
				String name = names[i];
				ResolvableType type = ResolvableType.forConstructorParameter(constructor, i);
				Annotation[] annotations = parameters[i].getDeclaredAnnotations();
				result.add(new ConstructorParameter(name, type, annotations));
			}
			return Collections.unmodifiableList(result);
		}

		@Override
		public List<ConstructorParameter> getConstructorParameters() {
			return this.constructorParameters;
		}

		@SuppressWarnings("unchecked")
		static <T> ValueObject<T> get(Class<T> type) {
			Constructor<?>[] constructors = type.getDeclaredConstructors();
			return (constructors.length != 1) ? null : get((Constructor<T>) constructors[0]);
		}

		static <T> DefaultValueObject<T> get(Constructor<T> constructor) {
			if (constructor == null || constructor.getParameterCount() == 0) {
				return null;
			}
			return new DefaultValueObject<>(constructor);
		}

	}

	/**
	 * A constructor parameter being bound.
	 */
	private static class ConstructorParameter {

		private final String name;

		private final ResolvableType type;

		private final Annotation[] annotations;

		ConstructorParameter(String name, ResolvableType type, Annotation[] annotations) {
			this.name = DataObjectPropertyName.toDashedForm(name);
			this.type = type;
			this.annotations = annotations;
		}

		public Object getDefaultValue(BindConverter converter) {
			for (Annotation annotation : this.annotations) {
				if (annotation instanceof DefaultValue) {
					return converter.convert(((DefaultValue) annotation).value(), this.type, this.annotations);
				}
			}
			return null;
		}

		public Object bind(DataObjectPropertyBinder propertyBinder) {
			return propertyBinder.bindProperty(this.name, Bindable.of(this.type).withAnnotations(this.annotations));
		}

	}

}
