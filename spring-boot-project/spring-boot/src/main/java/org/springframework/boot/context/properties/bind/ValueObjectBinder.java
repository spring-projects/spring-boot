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

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.jvm.ReflectJvmMapping;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.bind.Binder.Context;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.core.CollectionFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;

/**
 * {@link DataObjectBinder} for immutable value objects.
 *
 * @author Madhura Bhave
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Scott Frederick
 */
class ValueObjectBinder implements DataObjectBinder {

	private static final Log logger = LogFactory.getLog(ValueObjectBinder.class);

	private final BindConstructorProvider constructorProvider;

	/**
	 * Constructs a new ValueObjectBinder with the specified BindConstructorProvider.
	 * @param constructorProvider the BindConstructorProvider to be used for binding value
	 * objects
	 */
	ValueObjectBinder(BindConstructorProvider constructorProvider) {
		this.constructorProvider = constructorProvider;
	}

	/**
	 * Binds the given configuration property name to the target bindable object using the
	 * provided context and property binder.
	 * @param name the configuration property name to bind
	 * @param target the target bindable object
	 * @param context the binder context
	 * @param propertyBinder the data object property binder
	 * @return the bound object of type T, or null if binding was unsuccessful
	 */
	@Override
	public <T> T bind(ConfigurationPropertyName name, Bindable<T> target, Binder.Context context,
			DataObjectPropertyBinder propertyBinder) {
		ValueObject<T> valueObject = ValueObject.get(target, this.constructorProvider, context, Discoverer.LENIENT);
		if (valueObject == null) {
			return null;
		}
		context.pushConstructorBoundTypes(target.getType().resolve());
		List<ConstructorParameter> parameters = valueObject.getConstructorParameters();
		List<Object> args = new ArrayList<>(parameters.size());
		boolean bound = false;
		for (ConstructorParameter parameter : parameters) {
			Object arg = parameter.bind(propertyBinder);
			bound = bound || arg != null;
			arg = (arg != null) ? arg : getDefaultValue(context, parameter);
			args.add(arg);
		}
		context.clearConfigurationProperty();
		context.popConstructorBoundTypes();
		return bound ? valueObject.instantiate(args) : null;
	}

	/**
	 * Creates a new instance of the specified target type using the provided binder
	 * context.
	 * @param target the bindable target type
	 * @param context the binder context
	 * @return a new instance of the target type, or null if the value object could not be
	 * created
	 */
	@Override
	public <T> T create(Bindable<T> target, Binder.Context context) {
		ValueObject<T> valueObject = ValueObject.get(target, this.constructorProvider, context, Discoverer.LENIENT);
		if (valueObject == null) {
			return null;
		}
		List<ConstructorParameter> parameters = valueObject.getConstructorParameters();
		List<Object> args = new ArrayList<>(parameters.size());
		for (ConstructorParameter parameter : parameters) {
			args.add(getDefaultValue(context, parameter));
		}
		return valueObject.instantiate(args);
	}

	/**
	 * This method is called when an instance of a bindable target cannot be created. It
	 * attempts to retrieve the value object using the provided target, constructor
	 * provider, and context. If an exception occurs during the retrieval process, it is
	 * added as a suppressed exception to the original exception.
	 * @param target the bindable target
	 * @param context the context
	 * @param exception the runtime exception that occurred during instance creation
	 * @param <T> the type of the bindable target
	 */
	@Override
	public <T> void onUnableToCreateInstance(Bindable<T> target, Context context, RuntimeException exception) {
		try {
			ValueObject.get(target, this.constructorProvider, context, Discoverer.STRICT);
		}
		catch (Exception ex) {
			exception.addSuppressed(ex);
		}
	}

	/**
	 * Retrieves the default value for a constructor parameter.
	 * @param context the binder context
	 * @param parameter the constructor parameter
	 * @return the default value for the parameter, or null if not found
	 */
	private <T> T getDefaultValue(Binder.Context context, ConstructorParameter parameter) {
		ResolvableType type = parameter.getType();
		Annotation[] annotations = parameter.getAnnotations();
		for (Annotation annotation : annotations) {
			if (annotation instanceof DefaultValue defaultValueAnnotation) {
				String[] defaultValue = defaultValueAnnotation.value();
				if (defaultValue.length == 0) {
					return getNewDefaultValueInstanceIfPossible(context, type);
				}
				return convertDefaultValue(context.getConverter(), defaultValue, type, annotations);
			}
		}
		return null;
	}

	/**
	 * Converts the default value to the specified type using the provided converter.
	 * @param converter the bind converter to use for the conversion
	 * @param defaultValue the default value to convert
	 * @param type the target type to convert the default value to
	 * @param annotations the annotations associated with the target type
	 * @return the converted default value
	 * @throws ConversionException if the conversion fails
	 */
	private <T> T convertDefaultValue(BindConverter converter, String[] defaultValue, ResolvableType type,
			Annotation[] annotations) {
		try {
			return converter.convert(defaultValue, type, annotations);
		}
		catch (ConversionException ex) {
			// Try again in case ArrayToObjectConverter is not in play
			if (defaultValue.length == 1) {
				return converter.convert(defaultValue[0], type, annotations);
			}
			throw ex;
		}
	}

	/**
	 * Returns a new instance of the default value for the given type if possible.
	 * @param context the binder context
	 * @param type the ResolvableType of the parameter
	 * @return a new instance of the default value for the given type, or null if not
	 * possible
	 * @throws IllegalStateException if the parameter type does not allow empty default
	 * values
	 */
	@SuppressWarnings("unchecked")
	private <T> T getNewDefaultValueInstanceIfPossible(Binder.Context context, ResolvableType type) {
		Class<T> resolved = (Class<T>) type.resolve();
		Assert.state(resolved == null || isEmptyDefaultValueAllowed(resolved),
				() -> "Parameter of type " + type + " must have a non-empty default value.");
		if (resolved != null) {
			if (Optional.class == resolved) {
				return (T) Optional.empty();
			}
			if (Collection.class.isAssignableFrom(resolved)) {
				return (T) CollectionFactory.createCollection(resolved, 0);
			}
			if (Map.class.isAssignableFrom(resolved)) {
				return (T) CollectionFactory.createMap(resolved, 0);
			}
			if (resolved.isArray()) {
				return (T) Array.newInstance(resolved.getComponentType(), 0);
			}
		}
		T instance = create(Bindable.of(type), context);
		if (instance != null) {
			return instance;
		}
		return (resolved != null) ? BeanUtils.instantiateClass(resolved) : null;
	}

	/**
	 * Checks if the default value is allowed for an empty value of the given type.
	 * @param type the type to check
	 * @return {@code true} if the default value is allowed for an empty value of the
	 * given type, {@code false} otherwise
	 */
	private boolean isEmptyDefaultValueAllowed(Class<?> type) {
		return (Optional.class == type || isAggregate(type))
				|| !(type.isPrimitive() || type.isEnum() || type.getName().startsWith("java.lang"));
	}

	/**
	 * Checks if the given class is an aggregate type. An aggregate type can be an array,
	 * a Map, or a Collection.
	 * @param type the class to check
	 * @return true if the class is an aggregate type, false otherwise
	 */
	private boolean isAggregate(Class<?> type) {
		return type.isArray() || Map.class.isAssignableFrom(type) || Collection.class.isAssignableFrom(type);
	}

	/**
	 * The value object being bound.
	 *
	 * @param <T> the value object type
	 */
	private abstract static class ValueObject<T> {

		private final Constructor<T> constructor;

		/**
		 * Constructs a new ValueObject with the specified constructor.
		 * @param constructor the constructor to be used for creating new instances of
		 * ValueObject
		 */
		protected ValueObject(Constructor<T> constructor) {
			this.constructor = constructor;
		}

		/**
		 * Instantiates a new instance of the ValueObject class using the provided
		 * arguments.
		 * @param args the list of arguments to be passed to the constructor
		 * @return a new instance of the ValueObject class
		 */
		T instantiate(List<Object> args) {
			return BeanUtils.instantiateClass(this.constructor, args.toArray());
		}

		/**
		 * Returns a list of constructor parameters for the ValueObject class.
		 * @return a list of ConstructorParameter objects representing the constructor
		 * parameters
		 */
		abstract List<ConstructorParameter> getConstructorParameters();

		/**
		 * Retrieves a ValueObject for the given Bindable.
		 * @param bindable the Bindable object to retrieve the ValueObject for
		 * @param constructorProvider the BindConstructorProvider used to retrieve the
		 * bind constructor
		 * @param context the Binder.Context object
		 * @param parameterNameDiscoverer the ParameterNameDiscoverer used to discover
		 * parameter names
		 * @param <T> the type of the ValueObject
		 * @return the ValueObject for the given Bindable, or null if it cannot be
		 * retrieved
		 */
		@SuppressWarnings("unchecked")
		static <T> ValueObject<T> get(Bindable<T> bindable, BindConstructorProvider constructorProvider,
				Binder.Context context, ParameterNameDiscoverer parameterNameDiscoverer) {
			Class<T> type = (Class<T>) bindable.getType().resolve();
			if (type == null || type.isEnum() || Modifier.isAbstract(type.getModifiers())) {
				return null;
			}
			Constructor<?> bindConstructor = constructorProvider.getBindConstructor(bindable,
					context.isNestedConstructorBinding());
			if (bindConstructor == null) {
				return null;
			}
			if (KotlinDetector.isKotlinType(type)) {
				return KotlinValueObject.get((Constructor<T>) bindConstructor, bindable.getType(),
						parameterNameDiscoverer);
			}
			return DefaultValueObject.get(bindConstructor, bindable.getType(), parameterNameDiscoverer);
		}

	}

	/**
	 * A {@link ValueObject} implementation that is aware of Kotlin specific constructs.
	 */
	private static final class KotlinValueObject<T> extends ValueObject<T> {

		private static final Annotation[] ANNOTATION_ARRAY = new Annotation[0];

		private final List<ConstructorParameter> constructorParameters;

		/**
		 * Constructs a new KotlinValueObject with the specified primary constructor,
		 * Kotlin constructor, and type.
		 * @param primaryConstructor the primary constructor of the KotlinValueObject
		 * @param kotlinConstructor the Kotlin constructor of the KotlinValueObject
		 * @param type the ResolvableType of the KotlinValueObject
		 */
		private KotlinValueObject(Constructor<T> primaryConstructor, KFunction<T> kotlinConstructor,
				ResolvableType type) {
			super(primaryConstructor);
			this.constructorParameters = parseConstructorParameters(kotlinConstructor, type);
		}

		/**
		 * Parses the constructor parameters of a Kotlin constructor.
		 * @param kotlinConstructor the Kotlin constructor to parse
		 * @param type the ResolvableType of the constructor
		 * @return a list of ConstructorParameter objects representing the parsed
		 * parameters
		 */
		private List<ConstructorParameter> parseConstructorParameters(KFunction<T> kotlinConstructor,
				ResolvableType type) {
			List<KParameter> parameters = kotlinConstructor.getParameters();
			List<ConstructorParameter> result = new ArrayList<>(parameters.size());
			for (KParameter parameter : parameters) {
				String name = getParameterName(parameter);
				ResolvableType parameterType = ResolvableType
					.forType(ReflectJvmMapping.getJavaType(parameter.getType()), type);
				Annotation[] annotations = parameter.getAnnotations().toArray(ANNOTATION_ARRAY);
				result.add(new ConstructorParameter(name, parameterType, annotations));
			}
			return Collections.unmodifiableList(result);
		}

		/**
		 * Returns the name of the parameter.
		 * @param parameter the parameter to get the name from
		 * @return the name of the parameter
		 */
		private String getParameterName(KParameter parameter) {
			return MergedAnnotations.from(parameter, parameter.getAnnotations().toArray(ANNOTATION_ARRAY))
				.get(Name.class)
				.getValue(MergedAnnotation.VALUE, String.class)
				.orElseGet(parameter::getName);
		}

		/**
		 * Returns the list of constructor parameters for this KotlinValueObject.
		 * @return the list of constructor parameters
		 */
		@Override
		List<ConstructorParameter> getConstructorParameters() {
			return this.constructorParameters;
		}

		/**
		 * Returns a ValueObject instance based on the provided constructor, type, and
		 * parameter name discoverer.
		 * @param bindConstructor the constructor to bind the ValueObject to
		 * @param type the ResolvableType of the ValueObject
		 * @param parameterNameDiscoverer the ParameterNameDiscoverer to use for resolving
		 * parameter names
		 * @return a ValueObject instance
		 */
		static <T> ValueObject<T> get(Constructor<T> bindConstructor, ResolvableType type,
				ParameterNameDiscoverer parameterNameDiscoverer) {
			KFunction<T> kotlinConstructor = ReflectJvmMapping.getKotlinFunction(bindConstructor);
			if (kotlinConstructor != null) {
				return new KotlinValueObject<>(bindConstructor, kotlinConstructor, type);
			}
			return DefaultValueObject.get(bindConstructor, type, parameterNameDiscoverer);
		}

	}

	/**
	 * A default {@link ValueObject} implementation that uses only standard Java
	 * reflection calls.
	 */
	private static final class DefaultValueObject<T> extends ValueObject<T> {

		private final List<ConstructorParameter> constructorParameters;

		/**
		 * Constructs a new DefaultValueObject with the specified constructor and
		 * constructor parameters.
		 * @param constructor the constructor to be used for creating the
		 * DefaultValueObject
		 * @param constructorParameters the list of constructor parameters to be used for
		 * creating the DefaultValueObject
		 */
		private DefaultValueObject(Constructor<T> constructor, List<ConstructorParameter> constructorParameters) {
			super(constructor);
			this.constructorParameters = constructorParameters;
		}

		/**
		 * Returns the list of constructor parameters for this DefaultValueObject.
		 * @return the list of constructor parameters
		 */
		@Override
		List<ConstructorParameter> getConstructorParameters() {
			return this.constructorParameters;
		}

		/**
		 * Retrieves a ValueObject instance using the provided bindConstructor, type, and
		 * parameterNameDiscoverer.
		 * @param bindConstructor The constructor to bind the ValueObject to.
		 * @param type The ResolvableType of the ValueObject.
		 * @param parameterNameDiscoverer The ParameterNameDiscoverer to use for
		 * retrieving parameter names.
		 * @param <T> The type of the ValueObject.
		 * @return A ValueObject instance with the specified bindConstructor and
		 * constructorParameters, or null if parameter names are not available.
		 */
		@SuppressWarnings("unchecked")
		static <T> ValueObject<T> get(Constructor<?> bindConstructor, ResolvableType type,
				ParameterNameDiscoverer parameterNameDiscoverer) {
			String[] names = parameterNameDiscoverer.getParameterNames(bindConstructor);
			if (names == null) {
				return null;
			}
			List<ConstructorParameter> constructorParameters = parseConstructorParameters(bindConstructor, type, names);
			return new DefaultValueObject<>((Constructor<T>) bindConstructor, constructorParameters);
		}

		/**
		 * Parses the constructor parameters of a given constructor and returns a list of
		 * ConstructorParameter objects.
		 * @param constructor The constructor to parse the parameters from.
		 * @param type The ResolvableType of the object being constructed.
		 * @param names The names of the constructor parameters.
		 * @return A list of ConstructorParameter objects representing the parsed
		 * constructor parameters.
		 */
		private static List<ConstructorParameter> parseConstructorParameters(Constructor<?> constructor,
				ResolvableType type, String[] names) {
			Parameter[] parameters = constructor.getParameters();
			List<ConstructorParameter> result = new ArrayList<>(parameters.length);
			for (int i = 0; i < parameters.length; i++) {
				String name = MergedAnnotations.from(parameters[i])
					.get(Name.class)
					.getValue(MergedAnnotation.VALUE, String.class)
					.orElse(names[i]);
				ResolvableType parameterType = ResolvableType.forMethodParameter(new MethodParameter(constructor, i),
						type);
				Annotation[] annotations = parameters[i].getDeclaredAnnotations();
				result.add(new ConstructorParameter(name, parameterType, annotations));
			}
			return Collections.unmodifiableList(result);
		}

	}

	/**
	 * A constructor parameter being bound.
	 */
	private static class ConstructorParameter {

		private final String name;

		private final ResolvableType type;

		private final Annotation[] annotations;

		/**
		 * Constructs a new ConstructorParameter with the specified name, type, and
		 * annotations.
		 * @param name the name of the parameter
		 * @param type the type of the parameter
		 * @param annotations the annotations associated with the parameter
		 */
		ConstructorParameter(String name, ResolvableType type, Annotation[] annotations) {
			this.name = DataObjectPropertyName.toDashedForm(name);
			this.type = type;
			this.annotations = annotations;
		}

		/**
		 * Binds the data object property using the provided property binder.
		 * @param propertyBinder the property binder to use for binding
		 * @return the bound object
		 */
		Object bind(DataObjectPropertyBinder propertyBinder) {
			return propertyBinder.bindProperty(this.name, Bindable.of(this.type).withAnnotations(this.annotations));
		}

		/**
		 * Returns an array of annotations associated with this constructor parameter.
		 * @return an array of annotations
		 */
		Annotation[] getAnnotations() {
			return this.annotations;
		}

		/**
		 * Returns the ResolvableType of the constructor parameter.
		 * @return the ResolvableType of the constructor parameter
		 */
		ResolvableType getType() {
			return this.type;
		}

	}

	/**
	 * {@link ParameterNameDiscoverer} used for value data object binding.
	 */
	static final class Discoverer implements ParameterNameDiscoverer {

		private static final ParameterNameDiscoverer DEFAULT_DELEGATE = new DefaultParameterNameDiscoverer();

		private static final ParameterNameDiscoverer LENIENT = new Discoverer(DEFAULT_DELEGATE, (message) -> {
		});

		private static final ParameterNameDiscoverer STRICT = new Discoverer(DEFAULT_DELEGATE, (message) -> {
			throw new IllegalStateException(message.toString());
		});

		private final ParameterNameDiscoverer delegate;

		private final Consumer<LogMessage> noParameterNamesHandler;

		/**
		 * Constructs a new Discoverer with the specified delegate and
		 * noParameterNamesHandler.
		 * @param delegate the ParameterNameDiscoverer delegate to be used
		 * @param noParameterNamesHandler the Consumer<LogMessage> to handle cases where
		 * no parameter names are found
		 */
		private Discoverer(ParameterNameDiscoverer delegate, Consumer<LogMessage> noParameterNamesHandler) {
			this.delegate = delegate;
			this.noParameterNamesHandler = noParameterNamesHandler;
		}

		/**
		 * Retrieves the parameter names of the specified method.
		 * @param method the method for which to retrieve the parameter names
		 * @return an array of parameter names
		 * @throws UnsupportedOperationException if the operation is not supported
		 */
		@Override
		public String[] getParameterNames(Method method) {
			throw new UnsupportedOperationException();
		}

		/**
		 * Returns an array of parameter names for the given constructor.
		 * @param constructor the constructor for which to retrieve the parameter names
		 * @return an array of parameter names, or null if the parameter names cannot be
		 * discovered
		 * @throws IllegalArgumentException if the constructor is null
		 */
		@Override
		public String[] getParameterNames(Constructor<?> constructor) {
			String[] names = this.delegate.getParameterNames(constructor);
			if (names != null) {
				return names;
			}
			LogMessage message = LogMessage.format(
					"Unable to use value object binding with constructor [%s] as parameter names cannot be discovered. "
							+ "Ensure that the compiler uses the '-parameters' flag",
					constructor);
			this.noParameterNamesHandler.accept(message);
			logger.debug(message);
			return null;
		}

	}

}
