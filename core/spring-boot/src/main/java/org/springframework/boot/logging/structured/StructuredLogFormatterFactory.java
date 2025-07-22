/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.logging.structured;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.springframework.boot.json.JsonWriter.Members;
import org.springframework.boot.logging.StackTracePrinter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonProperties.Context;
import org.springframework.boot.util.Instantiator;
import org.springframework.boot.util.Instantiator.AvailableParameters;
import org.springframework.boot.util.Instantiator.FailureHandler;
import org.springframework.boot.util.LambdaSafe;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.io.support.SpringFactoriesLoader.ArgumentResolver;
import org.springframework.util.Assert;

/**
 * Factory that can be used to create a fully instantiated {@link StructuredLogFormatter}
 * for either a {@link CommonStructuredLogFormat#getId() common format} or a
 * fully-qualified class name.
 *
 * @param <E> the log even type
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @since 3.4.0
 * @see StructuredLogFormatter
 */
public class StructuredLogFormatterFactory<E> {

	private static final FailureHandler failureHandler = (type, implementationName, failure) -> {
		if (!(failure instanceof ClassNotFoundException)) {
			throw new IllegalArgumentException(
					"Unable to instantiate " + implementationName + " [" + type.getName() + "]", failure);
		}
	};

	private final SpringFactoriesLoader factoriesLoader;

	private final Class<E> logEventType;

	private final Instantiator<?> instantiator;

	private final CommonFormatters<E> commonFormatters;

	/**
	 * Create a new {@link StructuredLogFormatterFactory} instance.
	 * @param logEventType the log event type
	 * @param environment the Spring {@link Environment}
	 * @param availableParameters callback used to configure available parameters for the
	 * specific logging system
	 * @param commonFormatters callback used to define supported common formatters
	 */
	public StructuredLogFormatterFactory(Class<E> logEventType, Environment environment,
			Consumer<AvailableParameters> availableParameters, Consumer<CommonFormatters<E>> commonFormatters) {
		this(SpringFactoriesLoader.forDefaultResourceLocation(), logEventType, environment, availableParameters,
				commonFormatters);
	}

	StructuredLogFormatterFactory(SpringFactoriesLoader factoriesLoader, Class<E> logEventType, Environment environment,
			Consumer<AvailableParameters> availableParameters, Consumer<CommonFormatters<E>> commonFormatters) {
		StructuredLoggingJsonProperties properties = StructuredLoggingJsonProperties.get(environment);
		this.factoriesLoader = factoriesLoader;
		this.logEventType = logEventType;
		this.instantiator = new Instantiator<>(Object.class, (allAvailableParameters) -> {
			allAvailableParameters.add(Environment.class, environment);
			allAvailableParameters.add(StructuredLoggingJsonMembersCustomizer.class,
					new JsonMembersCustomizerBuilder(properties).build());
			allAvailableParameters.add(StructuredLoggingJsonMembersCustomizer.Builder.class,
					new JsonMembersCustomizerBuilder(properties));
			allAvailableParameters.add(StackTracePrinter.class, (type) -> getStackTracePrinter(properties));
			allAvailableParameters.add(ContextPairs.class, (type) -> getContextPairs(properties));
			if (availableParameters != null) {
				availableParameters.accept(allAvailableParameters);
			}
		}, failureHandler);
		this.commonFormatters = new CommonFormatters<>();
		commonFormatters.accept(this.commonFormatters);
	}

	private StackTracePrinter getStackTracePrinter(StructuredLoggingJsonProperties properties) {
		return (properties != null && properties.stackTrace() != null) ? properties.stackTrace().createPrinter() : null;
	}

	private ContextPairs getContextPairs(StructuredLoggingJsonProperties properties) {
		Context contextProperties = (properties != null) ? properties.context() : null;
		contextProperties = (contextProperties != null) ? contextProperties : new Context(true, null);
		return new ContextPairs(contextProperties.include(), contextProperties.prefix());
	}

	/**
	 * Get a new {@link StructuredLogFormatter} instance for the specified format.
	 * @param format the format requested (either a {@link CommonStructuredLogFormat} ID
	 * or a fully-qualified class name)
	 * @return a new {@link StructuredLogFormatter} instance
	 * @throws IllegalArgumentException if the format is unknown
	 */
	public StructuredLogFormatter<E> get(String format) {
		StructuredLogFormatter<E> formatter = this.commonFormatters.get(this.instantiator, format);
		formatter = (formatter != null) ? formatter : getUsingClassName(format);
		if (formatter != null) {
			return formatter;
		}
		throw new IllegalArgumentException(
				"Unknown format '%s'. Values can be a valid fully-qualified class name or one of the common formats: %s"
					.formatted(format, this.commonFormatters.getCommonNames()));
	}

	@SuppressWarnings("unchecked")
	private StructuredLogFormatter<E> getUsingClassName(String className) {
		Object formatter = this.instantiator.instantiate(className);
		if (formatter != null) {
			Assert.state(formatter instanceof StructuredLogFormatter,
					() -> "'%s' is not a StructuredLogFormatter".formatted(className));
			checkTypeArgument(formatter);
		}
		return (StructuredLogFormatter<E>) formatter;
	}

	private void checkTypeArgument(Object formatter) {
		Class<?> typeArgument = GenericTypeResolver.resolveTypeArgument(formatter.getClass(),
				StructuredLogFormatter.class);
		Assert.state(this.logEventType.equals(typeArgument),
				() -> "Type argument of %s must be %s but was %s".formatted(formatter.getClass().getName(),
						this.logEventType.getName(), (typeArgument != null) ? typeArgument.getName() : "null"));

	}

	/**
	 * Callback used for configure the {@link CommonFormatterFactory} to use for a given
	 * {@link CommonStructuredLogFormat}.
	 *
	 * @param <E> the log event type
	 */
	public static class CommonFormatters<E> {

		private final Map<CommonStructuredLogFormat, CommonFormatterFactory<E>> factories = new TreeMap<>();

		/**
		 * Add the factory that should be used for the given
		 * {@link CommonStructuredLogFormat}.
		 * @param format the common structured log format
		 * @param factory the factory to use
		 */
		public void add(CommonStructuredLogFormat format, CommonFormatterFactory<E> factory) {
			this.factories.put(format, factory);
		}

		Collection<String> getCommonNames() {
			return this.factories.keySet().stream().map(CommonStructuredLogFormat::getId).toList();
		}

		StructuredLogFormatter<E> get(Instantiator<?> instantiator, String format) {
			CommonStructuredLogFormat commonFormat = CommonStructuredLogFormat.forId(format);
			CommonFormatterFactory<E> factory = (commonFormat != null) ? this.factories.get(commonFormat) : null;
			return (factory != null) ? factory.createFormatter(instantiator) : null;
		}

	}

	/**
	 * Factory used to create a {@link StructuredLogFormatter} for a given
	 * {@link CommonStructuredLogFormat}.
	 *
	 * @param <E> the log event type
	 */
	@FunctionalInterface
	public interface CommonFormatterFactory<E> {

		/**
		 * Create the {@link StructuredLogFormatter} instance.
		 * @param instantiator instantiator that can be used to obtain arguments
		 * @return a new {@link StructuredLogFormatter} instance
		 */
		StructuredLogFormatter<E> createFormatter(Instantiator<?> instantiator);

	}

	/**
	 * {@link StructuredLoggingJsonMembersCustomizer.Builder} implementation.
	 */
	class JsonMembersCustomizerBuilder implements StructuredLoggingJsonMembersCustomizer.Builder<E> {

		private final StructuredLoggingJsonProperties properties;

		private boolean nested;

		JsonMembersCustomizerBuilder(StructuredLoggingJsonProperties properties) {
			this.properties = properties;
		}

		@Override
		public JsonMembersCustomizerBuilder nested(boolean nested) {
			this.nested = nested;
			return this;
		}

		@Override
		public StructuredLoggingJsonMembersCustomizer<E> build() {
			return (members) -> {
				List<StructuredLoggingJsonMembersCustomizer<?>> customizers = new ArrayList<>();
				if (this.properties != null) {
					customizers.add(new StructuredLoggingJsonPropertiesJsonMembersCustomizer(
							StructuredLogFormatterFactory.this.instantiator, this.properties, this.nested));
				}
				customizers.addAll(loadStructuredLoggingJsonMembersCustomizers());
				invokeCustomizers(members, customizers);
			};
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		private List<StructuredLoggingJsonMembersCustomizer<?>> loadStructuredLoggingJsonMembersCustomizers() {
			return (List) StructuredLogFormatterFactory.this.factoriesLoader.load(
					StructuredLoggingJsonMembersCustomizer.class,
					ArgumentResolver.from(StructuredLogFormatterFactory.this.instantiator::getArg));
		}

		@SuppressWarnings("unchecked")
		private void invokeCustomizers(Members<E> members,
				List<StructuredLoggingJsonMembersCustomizer<?>> customizers) {
			LambdaSafe.callbacks(StructuredLoggingJsonMembersCustomizer.class, customizers, members)
				.withFilter(LambdaSafe.Filter.allowAll())
				.invoke((customizer) -> customizer.customize(members));
		}

	}

}
