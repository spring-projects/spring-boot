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

package org.springframework.boot.util;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Utility that can be used to invoke lambdas in a safe way. Primarily designed to help
 * support generically typed callbacks where {@link ClassCastException class cast
 * exceptions} need to be dealt with due to class erasure.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public final class LambdaSafe {

	private static final Method CLASS_GET_MODULE;

	private static final Method MODULE_GET_NAME;

	static {
		CLASS_GET_MODULE = ReflectionUtils.findMethod(Class.class, "getModule");
		MODULE_GET_NAME = (CLASS_GET_MODULE != null)
				? ReflectionUtils.findMethod(CLASS_GET_MODULE.getReturnType(), "getName") : null;
	}

	private LambdaSafe() {
	}

	/**
	 * Start a call to a single callback instance, dealing with common generic type
	 * concerns and exceptions.
	 * @param callbackType the callback type (a {@link FunctionalInterface functional
	 * interface})
	 * @param callbackInstance the callback instance (may be a lambda)
	 * @param argument the primary argument passed to the callback
	 * @param additionalArguments any additional arguments passed to the callback
	 * @param <C> the callback type
	 * @param <A> the primary argument type
	 * @return a {@link Callback} instance that can be invoked.
	 */
	public static <C, A> Callback<C, A> callback(Class<C> callbackType, C callbackInstance, A argument,
			Object... additionalArguments) {
		Assert.notNull(callbackType, "CallbackType must not be null");
		Assert.notNull(callbackInstance, "CallbackInstance must not be null");
		return new Callback<>(callbackType, callbackInstance, argument, additionalArguments);
	}

	/**
	 * Start a call to callback instances, dealing with common generic type concerns and
	 * exceptions.
	 * @param callbackType the callback type (a {@link FunctionalInterface functional
	 * interface})
	 * @param callbackInstances the callback instances (elements may be lambdas)
	 * @param argument the primary argument passed to the callbacks
	 * @param additionalArguments any additional arguments passed to the callbacks
	 * @param <C> the callback type
	 * @param <A> the primary argument type
	 * @return a {@link Callbacks} instance that can be invoked.
	 */
	public static <C, A> Callbacks<C, A> callbacks(Class<C> callbackType, Collection<? extends C> callbackInstances,
			A argument, Object... additionalArguments) {
		Assert.notNull(callbackType, "CallbackType must not be null");
		Assert.notNull(callbackInstances, "CallbackInstances must not be null");
		return new Callbacks<>(callbackType, callbackInstances, argument, additionalArguments);
	}

	/**
	 * Abstract base class for lambda safe callbacks.
	 *
	 * @param <C> the callback type
	 * @param <A> the primary argument type
	 * @param <SELF> the self class reference
	 */
	protected abstract static class LambdaSafeCallback<C, A, SELF extends LambdaSafeCallback<C, A, SELF>> {

		private final Class<C> callbackType;

		private final A argument;

		private final Object[] additionalArguments;

		private Log logger;

		private Filter<C, A> filter = new GenericTypeFilter<>();

		LambdaSafeCallback(Class<C> callbackType, A argument, Object[] additionalArguments) {
			this.callbackType = callbackType;
			this.argument = argument;
			this.additionalArguments = additionalArguments;
			this.logger = LogFactory.getLog(callbackType);
		}

		/**
		 * Use the specified logger source to report any lambda failures.
		 * @param loggerSource the logger source to use
		 * @return this instance
		 */
		public SELF withLogger(Class<?> loggerSource) {
			return withLogger(LogFactory.getLog(loggerSource));
		}

		/**
		 * Use the specified logger to report any lambda failures.
		 * @param logger the logger to use
		 * @return this instance
		 */
		public SELF withLogger(Log logger) {
			Assert.notNull(logger, "Logger must not be null");
			this.logger = logger;
			return self();
		}

		/**
		 * Use a specific filter to determine when a callback should apply. If no explicit
		 * filter is set filter will be attempted using the generic type on the callback
		 * type.
		 * @param filter the filter to use
		 * @return this instance
		 */
		SELF withFilter(Filter<C, A> filter) {
			Assert.notNull(filter, "Filter must not be null");
			this.filter = filter;
			return self();
		}

		protected final <R> InvocationResult<R> invoke(C callbackInstance, Supplier<R> supplier) {
			if (this.filter.match(this.callbackType, callbackInstance, this.argument, this.additionalArguments)) {
				try {
					return InvocationResult.of(supplier.get());
				}
				catch (ClassCastException ex) {
					if (!isLambdaGenericProblem(ex)) {
						throw ex;
					}
					logNonMatchingType(callbackInstance, ex);
				}
			}
			return InvocationResult.noResult();
		}

		private boolean isLambdaGenericProblem(ClassCastException ex) {
			return (ex.getMessage() == null || startsWithArgumentClassName(ex.getMessage()));
		}

		private boolean startsWithArgumentClassName(String message) {
			Predicate<Object> startsWith = (argument) -> startsWithArgumentClassName(message, argument);
			return startsWith.test(this.argument) || Stream.of(this.additionalArguments).anyMatch(startsWith);
		}

		private boolean startsWithArgumentClassName(String message, Object argument) {
			if (argument == null) {
				return false;
			}
			Class<?> argumentType = argument.getClass();
			// On Java 8, the message starts with the class name: "java.lang.String cannot
			// be cast..."
			if (message.startsWith(argumentType.getName())) {
				return true;
			}
			// On Java 11, the message starts with "class ..." a.k.a. Class.toString()
			if (message.startsWith(argumentType.toString())) {
				return true;
			}
			// On Java 9, the message used to contain the module name:
			// "java.base/java.lang.String cannot be cast..."
			int moduleSeparatorIndex = message.indexOf('/');
			if (moduleSeparatorIndex != -1 && message.startsWith(argumentType.getName(), moduleSeparatorIndex + 1)) {
				return true;
			}
			if (CLASS_GET_MODULE != null) {
				Object module = ReflectionUtils.invokeMethod(CLASS_GET_MODULE, argumentType);
				Object moduleName = ReflectionUtils.invokeMethod(MODULE_GET_NAME, module);
				return message.startsWith(moduleName + "/" + argumentType.getName());
			}
			return false;
		}

		private void logNonMatchingType(C callback, ClassCastException ex) {
			if (this.logger.isDebugEnabled()) {
				Class<?> expectedType = ResolvableType.forClass(this.callbackType).resolveGeneric();
				String expectedTypeName = (expectedType != null) ? ClassUtils.getShortName(expectedType) + " type"
						: "type";
				String message = "Non-matching " + expectedTypeName + " for callback "
						+ ClassUtils.getShortName(this.callbackType) + ": " + callback;
				this.logger.debug(message, ex);
			}
		}

		@SuppressWarnings("unchecked")
		private SELF self() {
			return (SELF) this;
		}

	}

	/**
	 * Represents a single callback that can be invoked in a lambda safe way.
	 *
	 * @param <C> the callback type
	 * @param <A> the primary argument type
	 */
	public static final class Callback<C, A> extends LambdaSafeCallback<C, A, Callback<C, A>> {

		private final C callbackInstance;

		private Callback(Class<C> callbackType, C callbackInstance, A argument, Object[] additionalArguments) {
			super(callbackType, argument, additionalArguments);
			this.callbackInstance = callbackInstance;
		}

		/**
		 * Invoke the callback instance where the callback method returns void.
		 * @param invoker the invoker used to invoke the callback
		 */
		public void invoke(Consumer<C> invoker) {
			invoke(this.callbackInstance, () -> {
				invoker.accept(this.callbackInstance);
				return null;
			});
		}

		/**
		 * Invoke the callback instance where the callback method returns a result.
		 * @param invoker the invoker used to invoke the callback
		 * @param <R> the result type
		 * @return the result of the invocation (may be {@link InvocationResult#noResult}
		 * if the callback was not invoked)
		 */
		public <R> InvocationResult<R> invokeAnd(Function<C, R> invoker) {
			return invoke(this.callbackInstance, () -> invoker.apply(this.callbackInstance));
		}

	}

	/**
	 * Represents a collection of callbacks that can be invoked in a lambda safe way.
	 *
	 * @param <C> the callback type
	 * @param <A> the primary argument type
	 */
	public static final class Callbacks<C, A> extends LambdaSafeCallback<C, A, Callbacks<C, A>> {

		private final Collection<? extends C> callbackInstances;

		private Callbacks(Class<C> callbackType, Collection<? extends C> callbackInstances, A argument,
				Object[] additionalArguments) {
			super(callbackType, argument, additionalArguments);
			this.callbackInstances = callbackInstances;
		}

		/**
		 * Invoke the callback instances where the callback method returns void.
		 * @param invoker the invoker used to invoke the callback
		 */
		public void invoke(Consumer<C> invoker) {
			this.callbackInstances.forEach((callbackInstance) -> invoke(callbackInstance, () -> {
				invoker.accept(callbackInstance);
				return null;
			}));
		}

		/**
		 * Invoke the callback instances where the callback method returns a result.
		 * @param invoker the invoker used to invoke the callback
		 * @param <R> the result type
		 * @return the results of the invocation (may be an empty stream if no callbacks
		 * could be called)
		 */
		public <R> Stream<R> invokeAnd(Function<C, R> invoker) {
			Function<C, InvocationResult<R>> mapper = (callbackInstance) -> invoke(callbackInstance,
					() -> invoker.apply(callbackInstance));
			return this.callbackInstances.stream()
				.map(mapper)
				.filter(InvocationResult::hasResult)
				.map(InvocationResult::get);
		}

	}

	/**
	 * A filter that can be used to restrict when a callback is used.
	 *
	 * @param <C> the callback type
	 * @param <A> the primary argument type
	 */
	@FunctionalInterface
	interface Filter<C, A> {

		/**
		 * Determine if the given callback matches and should be invoked.
		 * @param callbackType the callback type (the functional interface)
		 * @param callbackInstance the callback instance (the implementation)
		 * @param argument the primary argument
		 * @param additionalArguments any additional arguments
		 * @return if the callback matches and should be invoked
		 */
		boolean match(Class<C> callbackType, C callbackInstance, A argument, Object[] additionalArguments);

		/**
		 * Return a {@link Filter} that allows all callbacks to be invoked.
		 * @param <C> the callback type
		 * @param <A> the primary argument type
		 * @return an "allow all" filter
		 */
		static <C, A> Filter<C, A> allowAll() {
			return (callbackType, callbackInstance, argument, additionalArguments) -> true;
		}

	}

	/**
	 * {@link Filter} that matches when the callback has a single generic and primary
	 * argument is an instance of it.
	 */
	private static class GenericTypeFilter<C, A> implements Filter<C, A> {

		@Override
		public boolean match(Class<C> callbackType, C callbackInstance, A argument, Object[] additionalArguments) {
			ResolvableType type = ResolvableType.forClass(callbackType, callbackInstance.getClass());
			if (type.getGenerics().length == 1 && type.resolveGeneric() != null) {
				return type.resolveGeneric().isInstance(argument);
			}

			return true;
		}

	}

	/**
	 * The result of a callback which may be a value, {@code null} or absent entirely if
	 * the callback wasn't suitable. Similar in design to {@link Optional} but allows for
	 * {@code null} as a valid value.
	 *
	 * @param <R> the result type
	 */
	public static final class InvocationResult<R> {

		private static final InvocationResult<?> NONE = new InvocationResult<>(null);

		private final R value;

		private InvocationResult(R value) {
			this.value = value;
		}

		/**
		 * Return true if a result in present.
		 * @return if a result is present
		 */
		public boolean hasResult() {
			return this != NONE;
		}

		/**
		 * Return the result of the invocation or {@code null} if the callback wasn't
		 * suitable.
		 * @return the result of the invocation or {@code null}
		 */
		public R get() {
			return this.value;
		}

		/**
		 * Return the result of the invocation or the given fallback if the callback
		 * wasn't suitable.
		 * @param fallback the fallback to use when there is no result
		 * @return the result of the invocation or the fallback
		 */
		public R get(R fallback) {
			return (this != NONE) ? this.value : fallback;
		}

		/**
		 * Create a new {@link InvocationResult} instance with the specified value.
		 * @param value the value (may be {@code null})
		 * @param <R> the result type
		 * @return an {@link InvocationResult}
		 */
		public static <R> InvocationResult<R> of(R value) {
			return new InvocationResult<>(value);
		}

		/**
		 * Return an {@link InvocationResult} instance representing no result.
		 * @param <R> the result type
		 * @return an {@link InvocationResult}
		 */
		@SuppressWarnings("unchecked")
		public static <R> InvocationResult<R> noResult() {
			return (InvocationResult<R>) NONE;
		}

	}

}
