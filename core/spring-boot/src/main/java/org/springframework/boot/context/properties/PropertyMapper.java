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

package org.springframework.boot.context.properties;

import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.function.SingletonSupplier;

/**
 * Utility that can be used to map values from a supplied source to a destination.
 * Primarily intended to be help when mapping from
 * {@link ConfigurationProperties @ConfigurationProperties} to third-party classes.
 * <p>
 * Can filter values based on predicates and adapt values if needed. For example:
 * <pre class="code">
 * PropertyMapper map = PropertyMapper.get();
 * map.from(source::getName)
 *   .to(destination::setName);
 * map.from(source::getTimeout)
 *   .when(this::thisYear)
 *   .asInt(Duration::getSeconds)
 *   .to(destination::setTimeoutSecs);
 * map.from(source::isEnabled)
 *   .whenFalse().
 *   .toCall(destination::disable);
 * </pre>
 * <p>
 * Mappings can ultimately be applied to a {@link Source#to(Consumer) setter}, trigger a
 * {@link Source#toCall(Runnable) method call} or create a
 * {@link Source#toInstance(Function) new instance}.
 * <p>
 * By default {@code null} values and any {@link NullPointerException} thrown from the
 * supplier are filtered and will not be applied to consumers. If you want to apply nulls,
 * you can use {@link Source#always()}.
 *
 * @author Phillip Webb
 * @author Artsiom Yudovin
 * @author Chris Bono
 * @author Moritz Halbritter
 * @since 2.0.0
 */
public final class PropertyMapper {

	private static final PropertyMapper INSTANCE = new PropertyMapper(null, null);

	private final @Nullable PropertyMapper parent;

	private final @Nullable SourceOperator sourceOperator;

	private PropertyMapper(@Nullable PropertyMapper parent, @Nullable SourceOperator sourceOperator) {
		this.parent = parent;
		this.sourceOperator = sourceOperator;
	}

	/**
	 * Return a new {@link PropertyMapper} instance that applies the given
	 * {@link SourceOperator} to every source.
	 * @param operator the source operator to apply
	 * @return a new property mapper instance
	 */
	public PropertyMapper alwaysApplying(SourceOperator operator) {
		Assert.notNull(operator, "'operator' must not be null");
		return new PropertyMapper(this, operator);
	}

	/**
	 * Return a new {@link Source} from the specified value that can be used to perform
	 * the mapping.
	 * @param <T> the source type
	 * @param value the value
	 * @return a {@link Source} that can be used to complete the mapping
	 */
	public <T> Source<T> from(@Nullable T value) {
		return from(() -> value);
	}

	/**
	 * Return a new {@link Source} from the specified value supplier that can be used to
	 * perform the mapping.
	 * @param <T> the source type
	 * @param supplier the value supplier
	 * @return a {@link Source} that can be used to complete the mapping
	 * @see #from(Object)
	 */
	public <T> Source<T> from(Supplier<? extends @Nullable T> supplier) {
		Assert.notNull(supplier, "'supplier' must not be null");
		Source<T> source = getSource(supplier);
		if (this.sourceOperator != null) {
			source = this.sourceOperator.apply(source);
		}
		return source;
	}

	private <T> Source<T> getSource(Supplier<? extends @Nullable T> supplier) {
		if (this.parent != null) {
			return this.parent.from(supplier);
		}
		return new Source<>(SingletonSupplier.of(supplier), (value) -> true);
	}

	/**
	 * Return the property mapper.
	 * @return the property mapper
	 */
	public static PropertyMapper get() {
		return INSTANCE;
	}

	/**
	 * An operation that can be applied to a {@link Source}.
	 */
	@FunctionalInterface
	public interface SourceOperator {

		/**
		 * Apply the operation to the given source.
		 * @param <T> the source type
		 * @param source the source to operate on
		 * @return the updated source
		 */
		<T> Source<T> apply(Source<T> source);

	}

	/**
	 * A source that is in the process of being mapped.
	 *
	 * @param <T> the source type
	 */
	public static final class Source<T> {

		private final Supplier<? extends @Nullable T> supplier;

		private final Predicate<T> predicate;

		private Source(Supplier<? extends @Nullable T> supplier, Predicate<T> predicate) {
			Assert.notNull(predicate, "'predicate' must not be null");
			this.supplier = supplier;
			this.predicate = predicate;
		}

		/**
		 * Return a source that will use the given supplier to obtain a fallback value to
		 * use in place of {@code null}.
		 * @param fallback the fallback supplier
		 * @return a new {@link Source} instance
		 * @since 4.0.0
		 */
		public Source<T> orFrom(Supplier<? extends @Nullable T> fallback) {
			Assert.notNull(fallback, "'fallback' must not be null");
			Supplier<@Nullable T> supplier = () -> {
				T value = getValue();
				return (value != null) ? value : fallback.get();
			};
			return new Source<>(supplier, this.predicate);
		}

		/**
		 * Return an adapted version of the source with {@link Integer} type.
		 * @param <R> the resulting type
		 * @param adapter an adapter to convert the current value to a number.
		 * @return a new adapted source instance
		 */
		public <R extends Number> Source<Integer> asInt(Adapter<? super T, ? extends R> adapter) {
			return as(adapter).as(Number::intValue);
		}

		/**
		 * Return an adapted version of the source changed through the given adapter
		 * function.
		 * @param <R> the resulting type
		 * @param adapter the adapter to apply
		 * @return a new adapted source instance
		 */
		public <R> Source<R> as(Adapter<? super T, ? extends R> adapter) {
			Assert.notNull(adapter, "'adapter' must not be null");
			Supplier<@Nullable R> supplier = () -> {
				T value = getValue();
				return (value != null && this.predicate.test(value)) ? adapter.adapt(value) : null;
			};
			Predicate<R> predicate = (adaptedValue) -> {
				T value = getValue();
				return value != null && this.predicate.test(value);
			};
			return new Source<>(supplier, predicate);
		}

		/**
		 * Return a filtered version of the source that will only map values that are
		 * {@code true}.
		 * @return a new filtered source instance
		 */
		public Source<T> whenTrue() {
			return when(Boolean.TRUE::equals);
		}

		/**
		 * Return a filtered version of the source that will only map values that are
		 * {@code false}.
		 * @return a new filtered source instance
		 */
		public Source<T> whenFalse() {
			return when(Boolean.FALSE::equals);
		}

		/**
		 * Return a filtered version of the source that will only map values that have a
		 * {@code toString()} containing actual text.
		 * @return a new filtered source instance
		 */
		public Source<T> whenHasText() {
			return when((value) -> StringUtils.hasText(value.toString()));
		}

		/**
		 * Return a filtered version of the source that will only map values equal to the
		 * specified {@code object}.
		 * @param object the object to match
		 * @return a new filtered source instance
		 */
		public Source<T> whenEqualTo(@Nullable Object object) {
			return when((value) -> value.equals(object));
		}

		/**
		 * Return a filtered version of the source that will only map values that are an
		 * instance of the given type.
		 * @param <R> the target type
		 * @param target the target type to match
		 * @return a new filtered source instance
		 */
		public <R extends T> Source<R> whenInstanceOf(Class<R> target) {
			Assert.notNull(target, "'target' must not be null");
			return when(target::isInstance).as(target::cast);
		}

		/**
		 * Return a filtered version of the source that won't map values that match the
		 * given predicate.
		 * @param predicate the predicate used to filter values
		 * @return a new filtered source instance
		 */
		public Source<T> whenNot(Predicate<T> predicate) {
			Assert.notNull(predicate, "'predicate' must not be null");
			return when(predicate.negate());
		}

		/**
		 * Return a filtered version of the source that won't map values that don't match
		 * the given predicate.
		 * @param predicate the predicate used to filter values
		 * @return a new filtered source instance
		 */
		public Source<T> when(Predicate<T> predicate) {
			Assert.notNull(predicate, "'predicate' must not be null");
			return new Source<>(this.supplier, this.predicate.and(predicate));
		}

		/**
		 * Complete the mapping by passing any non-filtered value to the specified
		 * consumer. The method is designed to be used with mutable objects.
		 * @param consumer the consumer that should accept the value if it's not been
		 * filtered
		 */
		public void to(Consumer<? super T> consumer) {
			Assert.notNull(consumer, "'consumer' must not be null");
			T value = getValue();
			if (value != null && test(value)) {
				consumer.accept(value);
			}
		}

		/**
		 * Complete the mapping for any non-filtered value by applying the given function
		 * to an existing instance and returning a new one. For filtered values, the
		 * {@code instance} parameter is returned unchanged. The method is designed to be
		 * used with immutable objects.
		 * @param <R> the result type
		 * @param instance the current instance
		 * @param mapper the mapping function
		 * @return a new mapped instance or the original instance
		 * @since 3.0.0
		 */
		public <R> R to(R instance, BiFunction<R, ? super T, R> mapper) {
			Assert.notNull(instance, "'instance' must not be null");
			Assert.notNull(mapper, "'mapper' must not be null");
			T value = getValue();
			if (value != null && test(value)) {
				return mapper.apply(instance, value);
			}
			return instance;
		}

		/**
		 * Complete the mapping by creating a new instance from the non-filtered value.
		 * @param <R> the resulting type
		 * @param factory the factory used to create the instance
		 * @return the instance
		 * @throws NoSuchElementException if the value has been filtered
		 */
		public <R> R toInstance(Function<? super T, R> factory) {
			Assert.notNull(factory, "'factory' must not be null");
			T value = getValue();
			if (value != null && test(value)) {
				return factory.apply(value);
			}
			throw new NoSuchElementException("No value present");
		}

		/**
		 * Complete the mapping by calling the specified method when the value has not
		 * been filtered.
		 * @param runnable the method to call if the value has not been filtered
		 */
		public void toCall(Runnable runnable) {
			Assert.notNull(runnable, "'runnable' must not be null");
			T value = getValue();
			if (value != null && test(value)) {
				runnable.run();
			}
		}

		/**
		 * Return a version of this source that can be used to always complete mappings,
		 * even if values are {@code null}.
		 * @return a new {@link Always} instance
		 * @since 4.0.0
		 */
		public Always<T> always() {
			Supplier<@Nullable T> getValue = this::getValue;
			return new Always<>(getValue, this::test);
		}

		private @Nullable T getValue() {
			try {
				return this.supplier.get();
			}
			catch (NullPointerException ex) {
				return null;
			}
		}

		private boolean test(T value) {
			Assert.state(value != null, "'value' must not be null");
			return this.predicate.test(value);
		}

		/**
		 * Adapter used to adapt a value and possibly return a {@code null} result.
		 *
		 * @param <T> the source type
		 * @param <R> the result type
		 * @since 4.0.0
		 */
		@FunctionalInterface
		public interface Adapter<T, R> {

			/**
			 * Adapt the given value.
			 * @param value the value to adapt
			 * @return an adapted value or {@code null}
			 */
			@Nullable R adapt(T value);

		}

		/**
		 * Allow source mapping to complete using methods that accept nulls.
		 *
		 * @param <T> the source type
		 * @since 4.0.0
		 */
		public static class Always<T> {

			private final Supplier<@Nullable T> supplier;

			private final Predicate<T> predicate;

			Always(Supplier<@Nullable T> supplier, Predicate<T> predicate) {
				this.supplier = supplier;
				this.predicate = predicate;
			}

			/**
			 * Return an adapted version of the source changed through the given adapter
			 * function.
			 * @param <R> the resulting type
			 * @param adapter the adapter to apply
			 * @return a new adapted source instance
			 */
			public <R> Always<R> as(Adapter<? super T, ? extends R> adapter) {
				Assert.notNull(adapter, "'adapter' must not be null");
				Supplier<@Nullable R> supplier = () -> {
					T value = getValue();
					return (value == null || test(value)) ? adapter.adapt(value) : null;
				};
				Predicate<R> predicate = (adaptedValue) -> {
					T value = getValue();
					return value == null || test(value);
				};
				return new Always<>(supplier, predicate);
			}

			/**
			 * Complete the mapping by passing any non-filtered value to the specified
			 * consumer. The method is designed to be used with mutable objects.
			 * @param consumer the consumer that should accept the value if it's not been
			 * filtered
			 */
			public void to(Consumer<@Nullable ? super T> consumer) {
				Assert.notNull(consumer, "'consumer' must not be null");
				T value = getValue();
				if (value == null || test(value)) {
					consumer.accept(value);
				}
			}

			/**
			 * Complete the mapping for any non-filtered value by applying the given
			 * function to an existing instance and returning a new one. For filtered
			 * values, the {@code instance} parameter is returned unchanged. The method is
			 * designed to be used with immutable objects.
			 * @param <R> the result type
			 * @param instance the current instance
			 * @param mapper the mapping function
			 * @return a new mapped instance or the original instance
			 */
			public <R> R to(R instance, Mapper<R, ? super T> mapper) {
				Assert.notNull(instance, "'instance' must not be null");
				Assert.notNull(mapper, "'mapper' must not be null");
				T value = getValue();
				if (value == null || test(value)) {
					return mapper.map(instance, value);
				}
				return instance;
			}

			/**
			 * Complete the mapping by creating a new instance from the non-filtered
			 * value.
			 * @param <R> the resulting type
			 * @param factory the factory used to create the instance
			 * @return the instance
			 * @throws NoSuchElementException if the value has been filtered
			 */
			public <R> R toInstance(Factory<? super T, ? extends R> factory) {
				Assert.notNull(factory, "'factory' must not be null");
				T value = getValue();
				if (value == null || test(value)) {
					return factory.create(value);
				}
				throw new NoSuchElementException("No value present");
			}

			/**
			 * Complete the mapping by calling the specified method when the value has not
			 * been filtered.
			 * @param runnable the method to call if the value has not been filtered
			 */
			public void toCall(Runnable runnable) {
				Assert.notNull(runnable, "'runnable' must not be null");
				T value = getValue();
				if (value == null || test(value)) {
					runnable.run();
				}
			}

			private @Nullable T getValue() {
				return this.supplier.get();
			}

			private boolean test(T value) {
				Assert.state(value != null, "'value' must not be null");
				return this.predicate.test(value);
			}

			/**
			 * Adapter that support nullable values.
			 *
			 * @param <T> the source type
			 * @param <R> the result type
			 */
			@FunctionalInterface
			public interface Adapter<T, R> {

				/**
				 * Adapt the given value.
				 * @param value the value to adapt
				 * @return an adapted value or {@code null}
				 */
				@Nullable R adapt(@Nullable T value);

			}

			/**
			 * Factory that supports nullable values.
			 *
			 * @param <T> the source type
			 * @param <R> the result type
			 */
			@FunctionalInterface
			public interface Factory<T, R extends @Nullable Object> {

				/**
				 * Create a new instance for the given nullable value.
				 * @param value the value used to create the instance (may be
				 * {@code null})
				 * @return the resulting instance
				 */
				R create(@Nullable T value);

			}

			/**
			 * Mapper that supports nullable values.
			 *
			 * @param <T> the source type
			 * @param <R> the result type
			 */
			@FunctionalInterface
			public interface Mapper<R extends @Nullable Object, T> {

				/**
				 * Map a existing instance for the given nullable value.
				 * @param instance the existing instance
				 * @param value the value to map (may be {@code null})
				 * @return the resulting mapped instance
				 */
				R map(R instance, @Nullable T value);

			}

		}

	}

}
