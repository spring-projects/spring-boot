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

package org.springframework.boot.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.springframework.boot.json.JsonValueWriter.Series;
import org.springframework.boot.json.JsonWriter.Member.Extractor;
import org.springframework.core.io.WritableResource;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Interface that can be used to write JSON output. Typically used to generate JSON when a
 * dependency on a fully marshalling library (such as Jackson or Gson) cannot be assumed.
 * <p>
 * For standard Java types, the {@link #standard()} factory method may be used to obtain
 * an instance of this interface. It supports {@link String}, {@link Number} and
 * {@link Boolean} as well as {@link Collection}, {@code Array}, {@link Map} and
 * {@link WritableJson} types. Typical usage would be:
 *
 * <pre class="code">
 * JsonWriter&lt;Map&lt;String,Object&gt;&gt; writer = JsonWriter.standard();
 * writer.write(Map.of("Hello", "World!"), out);
 * </pre>
 * <p>
 * More complex mappings can be created using the {@link #of(Consumer)} method with a
 * callback to configure the {@link Members JSON members} that should be written. Typical
 * usage would be:
 *
 * <pre class="code">
 * JsonWriter&lt;Person&gt; writer = JsonWriter.of((members) -&gt; {
 *     members.add("first", Person::firstName);
 *     members.add("last", Person::lastName);
 *     members.add("dob", Person::dateOfBirth)
 *         .whenNotNull()
 *         .as(DateTimeFormatter.ISO_DATE::format);
 * });
 * writer.write(person, out);
 * </pre>
 * <p>
 * The {@link #writeToString(Object)} method can be used if you want to write the JSON
 * directly to a {@link String}. To write to other types of output, the
 * {@link #write(Object)} method may be used to obtain a {@link WritableJson} instance.
 *
 * @param <T> the type being written
 * @author Phillip Webb
 * @author Moritz Halbritter
 * @since 3.4.0
 */
@FunctionalInterface
public interface JsonWriter<T> {

	/**
	 * Write the given instance to the provided {@link Appendable}.
	 * @param instance the instance to write (may be {@code null}
	 * @param out the output that should receive the JSON
	 * @throws IOException on IO error
	 */
	void write(T instance, Appendable out) throws IOException;

	/**
	 * Write the given instance to a JSON string.
	 * @param instance the instance to write (may be {@code null})
	 * @return the JSON string
	 */
	default String writeToString(T instance) {
		return write(instance).toJsonString();
	}

	/**
	 * Provide a {@link WritableJson} implementation that may be used to write the given
	 * instance to various outputs.
	 * @param instance the instance to write (may be {@code null})
	 * @return a {@link WritableJson} instance that may be used to write the JSON
	 */
	default WritableJson write(T instance) {
		return WritableJson.of((out) -> write(instance, out));
	}

	/**
	 * Return a new {@link JsonWriter} instance that appends a new line after the JSON has
	 * been written.
	 * @return a new {@link JsonWriter} instance that appends a new line after the JSON
	 */
	default JsonWriter<T> withNewLineAtEnd() {
		return withSuffix("\n");
	}

	/**
	 * Return a new {@link JsonWriter} instance that appends the given suffix after the
	 * JSON has been written.
	 * @param suffix the suffix to write, if any
	 * @return a new {@link JsonWriter} instance that appends a suffixafter the JSON
	 */
	default JsonWriter<T> withSuffix(String suffix) {
		if (!StringUtils.hasLength(suffix)) {
			return this;
		}
		return (instance, out) -> {
			write(instance, out);
			out.append(suffix);
		};
	}

	/**
	 * Factory method to return a {@link JsonWriter} for standard Java types. See
	 * {@link JsonValueWriter class-level javadoc} for details.
	 * @param <T> the type to write
	 * @return a {@link JsonWriter} instance
	 */
	static <T> JsonWriter<T> standard() {
		return of(Members::add);
	}

	/**
	 * Factory method to return a {@link JsonWriter} with specific {@link Members member
	 * mapping}. See {@link JsonValueWriter class-level javadoc} and {@link Members} for
	 * details.
	 * @param <T> the type to write
	 * @param members a consumer, which should configure the members
	 * @return a {@link JsonWriter} instance
	 * @see Members
	 */
	static <T> JsonWriter<T> of(Consumer<Members<T>> members) {
		Members<T> initializedMembers = new Members<>(members, false); // Don't inline
		return (instance, out) -> initializedMembers.write(instance, new JsonValueWriter(out));
	}

	/**
	 * JSON content that can be written out.
	 */
	@FunctionalInterface
	interface WritableJson {

		/**
		 * Write the JSON to the provided {@link Appendable}.
		 * @param out the {@link Appendable} to receive the JSON
		 * @throws IOException on IO error
		 */
		void to(Appendable out) throws IOException;

		/**
		 * Write the JSON to a {@link String}.
		 * @return the JSON string
		 */
		default String toJsonString() {
			try {
				StringBuilder stringBuilder = new StringBuilder();
				to(stringBuilder);
				return stringBuilder.toString();
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}

		/**
		 * Write the JSON to a UTF-8 encoded byte array.
		 * @return the JSON bytes
		 */
		default byte[] toByteArray() {
			return toByteArray(StandardCharsets.UTF_8);
		}

		/**
		 * Write the JSON to a byte array.
		 * @param charset the charset
		 * @return the JSON bytes
		 */
		default byte[] toByteArray(Charset charset) {
			Assert.notNull(charset, "'charset' must not be null");
			try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
				toWriter(new OutputStreamWriter(out, charset));
				return out.toByteArray();
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}

		/**
		 * Write the JSON to the provided {@link WritableResource} using
		 * {@link StandardCharsets#UTF_8 UTF8} encoding.
		 * @param out the {@link OutputStream} to receive the JSON
		 * @throws IOException on IO error
		 */
		default void toResource(WritableResource out) throws IOException {
			Assert.notNull(out, "'out' must not be null");
			try (OutputStream outputStream = out.getOutputStream()) {
				toOutputStream(outputStream);
			}
		}

		/**
		 * Write the JSON to the provided {@link WritableResource} using the given
		 * {@link Charset}.
		 * @param out the {@link OutputStream} to receive the JSON
		 * @param charset the charset to use
		 * @throws IOException on IO error
		 */
		default void toResource(WritableResource out, Charset charset) throws IOException {
			Assert.notNull(out, "'out' must not be null");
			Assert.notNull(charset, "'charset' must not be null");
			try (OutputStream outputStream = out.getOutputStream()) {
				toOutputStream(outputStream, charset);
			}
		}

		/**
		 * Write the JSON to the provided {@link OutputStream} using
		 * {@link StandardCharsets#UTF_8 UTF8} encoding. The output stream will not be
		 * closed.
		 * @param out the {@link OutputStream} to receive the JSON
		 * @throws IOException on IO error
		 * @see #toOutputStream(OutputStream, Charset)
		 */
		default void toOutputStream(OutputStream out) throws IOException {
			toOutputStream(out, StandardCharsets.UTF_8);
		}

		/**
		 * Write the JSON to the provided {@link OutputStream} using the given
		 * {@link Charset}. The output stream will not be closed.
		 * @param out the {@link OutputStream} to receive the JSON
		 * @param charset the charset to use
		 * @throws IOException on IO error
		 */
		default void toOutputStream(OutputStream out, Charset charset) throws IOException {
			Assert.notNull(out, "'out' must not be null");
			Assert.notNull(charset, "'charset' must not be null");
			toWriter(new OutputStreamWriter(out, charset));
		}

		/**
		 * Write the JSON to the provided {@link Writer}. The writer will be flushed but
		 * not closed.
		 * @param out the {@link Writer} to receive the JSON
		 * @throws IOException on IO error
		 * @see #toOutputStream(OutputStream, Charset)
		 */
		default void toWriter(Writer out) throws IOException {
			Assert.notNull(out, "'out' must not be null");
			to(out);
			out.flush();
		}

		/**
		 * Factory method used to create a {@link WritableJson} with a sensible
		 * {@link Object#toString()} that delegate to {@link WritableJson#toJsonString()}.
		 * @param writableJson the source {@link WritableJson}
		 * @return a new {@link WritableJson} with a sensible {@link Object#toString()}.
		 */
		static WritableJson of(WritableJson writableJson) {
			return new WritableJson() {

				@Override
				public void to(Appendable out) throws IOException {
					writableJson.to(out);
				}

				@Override
				public String toString() {
					return toJsonString();
				}

			};
		}

	}

	/**
	 * Callback used to configure JSON members. Individual members can be declared using
	 * the various {@code add(...)} methods. Typically, members are declared with a
	 * {@code "name"} and a {@link Function} that will extract the value from the
	 * instance. Members can also be declared using a static value or a {@link Supplier}.
	 * The {@link #add(String)} and {@link #add()} methods may be used to access the
	 * actual instance being written.
	 * <p>
	 * Members can be added without a {@code name} when a {@code Member.using(...)} method
	 * is used to complete the definition.
	 * <p>
	 * Members can filtered using {@code Member.when} methods and adapted to different
	 * types using {@link Member#as(Function) Member.as(...)}.
	 *
	 * @param <T> the type that will be written
	 */
	final class Members<T> {

		private final List<Member<?>> members = new ArrayList<>();

		private final boolean contributesPair;

		private final Series series;

		Members(Consumer<Members<T>> members, boolean contributesToExistingSeries) {
			Assert.notNull(members, "'members' must not be null");
			members.accept(this);
			Assert.state(!this.members.isEmpty(), "No members have been added");
			this.contributesPair = this.members.stream().anyMatch(Member::contributesPair);
			this.series = (this.contributesPair && !contributesToExistingSeries) ? Series.OBJECT : null;
			if (this.contributesPair || this.members.size() > 1) {
				this.members.forEach((member) -> Assert.state(member.contributesPair(),
						() -> String.format("%s does not contribute a named pair, ensure that all members have "
								+ "a name or call an appropriate 'using' method", member)));
			}
		}

		/**
		 * Add a new member with access to the instance being written.
		 * @param name the member name
		 * @return the added {@link Member} which may be configured further
		 */
		public Member<T> add(String name) {
			return add(name, (instance) -> instance);
		}

		/**
		 * Add a new member with a static value.
		 * @param <V> the value type
		 * @param name the member name
		 * @param value the member value
		 * @return the added {@link Member} which may be configured further
		 */
		public <V> Member<V> add(String name, V value) {
			return add(name, (instance) -> value);
		}

		/**
		 * Add a new member with a supplied value.
		 * @param <V> the value type
		 * @param name the member name
		 * @param supplier a supplier of the value
		 * @return the added {@link Member} which may be configured further
		 */
		public <V> Member<V> add(String name, Supplier<V> supplier) {
			Assert.notNull(supplier, "'supplier' must not be null");
			return add(name, (instance) -> supplier.get());
		}

		/**
		 * Add a new member with an extracted value.
		 * @param <V> the value type
		 * @param name the member name
		 * @param extractor a function to extract the value
		 * @return the added {@link Member} which may be configured further
		 */
		public <V> Member<V> add(String name, Function<T, V> extractor) {
			Assert.notNull(name, "'name' must not be null");
			Assert.notNull(extractor, "'extractor' must not be null");
			return addMember(name, extractor);
		}

		/**
		 * Add a new member with access to the instance being written. The member is added
		 * without a name, so one of the {@code Member.using(...)} methods must be used to
		 * complete the configuration.
		 * @return the added {@link Member} which may be configured further
		 */
		public Member<T> add() {
			return from(Function.identity());
		}

		/**
		 * Add all entries from the given {@link Map} to the JSON.
		 * @param <M> the map type
		 * @param <K> the key type
		 * @param <V> the value type
		 * @param extractor a function to extract the map
		 * @return the added {@link Member} which may be configured further
		 */
		public <M extends Map<K, V>, K, V> Member<M> addMapEntries(Function<T, M> extractor) {
			return from(extractor).usingPairs(Map::forEach);
		}

		/**
		 * Add members from a static value. One of the {@code Member.using(...)} methods
		 * must be used to complete the configuration.
		 * @param <V> the value type
		 * @param value the member value
		 * @return the added {@link Member} which may be configured further
		 */
		public <V> Member<V> from(V value) {
			return from((instance) -> value);
		}

		/**
		 * Add members from a supplied value. One of the {@code Member.using(...)} methods
		 * must be used to complete the configuration.
		 * @param <V> the value type
		 * @param supplier a supplier of the value
		 * @return the added {@link Member} which may be configured further
		 */
		public <V> Member<V> from(Supplier<V> supplier) {
			Assert.notNull(supplier, "'supplier' must not be null");
			return from((instance) -> supplier.get());
		}

		/**
		 * Add members from an extracted value. One of the {@code Member.using(...)}
		 * methods must be used to complete the configuration.
		 * @param <V> the value type
		 * @param extractor a function to extract the value
		 * @return the added {@link Member} which may be configured further
		 */
		public <V> Member<V> from(Function<T, V> extractor) {
			Assert.notNull(extractor, "'extractor' must not be null");
			return addMember(null, extractor);
		}

		private <V> Member<V> addMember(String name, Function<T, V> extractor) {
			Member<V> member = new Member<>(this.members.size(), name, Extractor.of(extractor));
			this.members.add(member);
			return member;
		}

		/**
		 * Writes the given instance using the configured {@link Member members}.
		 * @param instance the instance to write
		 * @param valueWriter the JSON value writer to use
		 */
		void write(T instance, JsonValueWriter valueWriter) {
			valueWriter.start(this.series);
			for (Member<?> member : this.members) {
				member.write(instance, valueWriter);
			}
			valueWriter.end(this.series);
		}

		/**
		 * Return if any of the members contributes a name/value pair to the JSON.
		 * @return if a name/value pair is contributed
		 */
		boolean contributesPair() {
			return this.contributesPair;
		}

	}

	/**
	 * A member that contributes JSON. Typically, a member will contribute a single
	 * name/value pair based on an extracted value. They may also contribute more complex
	 * JSON structures when configured with one of the {@code using(...)} methods.
	 * <p>
	 * The {@code when(...)} methods may be used to filter a member (omit it entirely from
	 * the JSON). The {@link #as(Function)} method can be used to adapt to a different
	 * type.
	 *
	 * @param <T> the member type
	 */
	final class Member<T> {

		private final int index;

		private final String name;

		private Extractor<T> extractor;

		private BiConsumer<T, BiConsumer<?, ?>> pairs;

		private Members<T> members;

		Member(int index, String name, Extractor<T> extractor) {
			this.index = index;
			this.name = name;
			this.extractor = extractor;
		}

		/**
		 * Only include this member when its value is not {@code null}.
		 * @return a {@link Member} which may be configured further
		 */
		public Member<T> whenNotNull() {
			return when(Objects::nonNull);
		}

		/**
		 * Only include this member when an extracted value is not {@code null}.
		 * @param extractor an function used to extract the value to test
		 * @return a {@link Member} which may be configured further
		 */
		public Member<T> whenNotNull(Function<T, ?> extractor) {
			Assert.notNull(extractor, "'extractor' must not be null");
			return when((instance) -> Objects.nonNull(extractor.apply(instance)));
		}

		/**
		 * Only include this member when it is not {@code null} and has a
		 * {@link Object#toString() toString()} that is not zero length.
		 * @return a {@link Member} which may be configured further
		 * @see StringUtils#hasLength(CharSequence)
		 */
		public Member<T> whenHasLength() {
			return when((instance) -> instance != null && StringUtils.hasLength(instance.toString()));
		}

		/**
		 * Only include this member when it is not empty (See
		 * {@link ObjectUtils#isEmpty(Object)} for details).
		 * @return a {@link Member} which may be configured further
		 */
		public Member<T> whenNotEmpty() {
			return whenNot(ObjectUtils::isEmpty);
		}

		/**
		 * Only include this member when the given predicate does not match.
		 * @param predicate the predicate to test
		 * @return a {@link Member} which may be configured further
		 */
		public Member<T> whenNot(Predicate<T> predicate) {
			Assert.notNull(predicate, "'predicate' must not be null");
			return when(predicate.negate());
		}

		/**
		 * Only include this member when the given predicate matches.
		 * @param predicate the predicate to test
		 * @return a {@link Member} which may be configured further
		 */
		public Member<T> when(Predicate<T> predicate) {
			Assert.notNull(predicate, "'predicate' must not be null");
			this.extractor = this.extractor.when(predicate);
			return this;
		}

		/**
		 * Adapt the value by applying the given {@link Function}.
		 * @param <R> the result type
		 * @param adapter a {@link Function} to adapt the value
		 * @return a {@link Member} which may be configured further
		 */
		@SuppressWarnings("unchecked")
		public <R> Member<R> as(Function<T, R> adapter) {
			Assert.notNull(adapter, "'adapter' must not be null");
			Member<R> result = (Member<R>) this;
			result.extractor = this.extractor.as(adapter);
			return result;
		}

		/**
		 * Add JSON name/value pairs by extracting values from a series of elements.
		 * Typically used with a {@link Iterable#forEach(Consumer)} call, for example:
		 *
		 * <pre class="code">
		 * members.add(Event::getTags).usingExtractedPairs(Iterable::forEach, pairExtractor);
		 * </pre>
		 * <p>
		 * When used with a named member, the pairs will be added as a new JSON value
		 * object:
		 *
		 * <pre>
		 * {
		 *   "name": {
		 *     "p1": 1,
		 *     "p2": 2
		 *   }
		 * }
		 * </pre>
		 *
		 * When used with an unnamed member the pairs will be added to the existing JSON
		 * object:
		 *
		 * <pre>
		 * {
		 *   "p1": 1,
		 *   "p2": 2
		 * }
		 * </pre>
		 * @param <E> the element type
		 * @param elements callback used to provide the elements
		 * @param extractor a {@link PairExtractor} used to extract the name/value pair
		 * @return a {@link Member} which may be configured further
		 * @see #usingExtractedPairs(BiConsumer, Function, Function)
		 * @see #usingPairs(BiConsumer)
		 */
		public <E> Member<T> usingExtractedPairs(BiConsumer<T, Consumer<E>> elements, PairExtractor<E> extractor) {
			Assert.notNull(elements, "'elements' must not be null");
			Assert.notNull(extractor, "'extractor' must not be null");
			return usingExtractedPairs(elements, extractor::getName, extractor::getValue);
		}

		/**
		 * Add JSON name/value pairs by extracting values from a series of elements.
		 * Typically used with a {@link Iterable#forEach(Consumer)} call, for example:
		 *
		 * <pre class="code">
		 * members.add(Event::getTags).usingExtractedPairs(Iterable::forEach, Tag::getName, Tag::getValue);
		 * </pre>
		 * <p>
		 * When used with a named member, the pairs will be added as a new JSON value
		 * object:
		 *
		 * <pre>
		 * {
		 *   "name": {
		 *     "p1": 1,
		 *     "p2": 2
		 *   }
		 * }
		 * </pre>
		 *
		 * When used with an unnamed member the pairs will be added to the existing JSON
		 * object:
		 *
		 * <pre>
		 * {
		 *   "p1": 1,
		 *   "p2": 2
		 * }
		 * </pre>
		 * @param <E> the element type
		 * @param <N> the name type
		 * @param <V> the value type
		 * @param elements callback used to provide the elements
		 * @param nameExtractor {@link Function} used to extract the name
		 * @param valueExtractor {@link Function} used to extract the value
		 * @return a {@link Member} which may be configured further
		 * @see #usingExtractedPairs(BiConsumer, PairExtractor)
		 * @see #usingPairs(BiConsumer)
		 */
		public <E, N, V> Member<T> usingExtractedPairs(BiConsumer<T, Consumer<E>> elements,
				Function<E, N> nameExtractor, Function<E, V> valueExtractor) {
			Assert.notNull(elements, "'elements' must not be null");
			Assert.notNull(nameExtractor, "'nameExtractor' must not be null");
			Assert.notNull(valueExtractor, "'valueExtractor' must not be null");
			return usingPairs((instance, pairsConsumer) -> elements.accept(instance, (element) -> {
				N name = nameExtractor.apply(element);
				V value = valueExtractor.apply(element);
				pairsConsumer.accept(name, value);
			}));
		}

		/**
		 * Add JSON name/value pairs. Typically used with a
		 * {@link Map#forEach(BiConsumer)} call, for example:
		 *
		 * <pre class="code">
		 * members.add(Event::getLabels).usingPairs(Map::forEach);
		 * </pre>
		 * <p>
		 * When used with a named member, the pairs will be added as a new JSON value
		 * object:
		 *
		 * <pre>
		 * {
		 *   "name": {
		 *     "p1": 1,
		 *     "p2": 2
		 *   }
		 * }
		 * </pre>
		 *
		 * When used with an unnamed member the pairs will be added to the existing JSON
		 * object:
		 *
		 * <pre>
		 * {
		 *   "p1": 1,
		 *   "p2": 2
		 * }
		 * </pre>
		 * @param <N> the name type
		 * @param <V> the value type
		 * @param pairs callback used to provide the pairs
		 * @return a {@link Member} which may be configured further
		 * @see #usingExtractedPairs(BiConsumer, PairExtractor)
		 * @see #usingPairs(BiConsumer)
		 */
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public <N, V> Member<T> usingPairs(BiConsumer<T, BiConsumer<N, V>> pairs) {
			Assert.notNull(pairs, "'pairs' must not be null");
			Assert.state(this.pairs == null, "Pairs cannot be declared multiple times");
			Assert.state(this.members == null, "Pairs cannot be declared when using members");
			this.pairs = (BiConsumer) pairs;
			return this;
		}

		/**
		 * Add JSON based on further {@link Members} configuration. For example:
		 *
		 * <pre class="code">
		 * members.add(User::getName).usingMembers((personMembers) -> {
		 *     personMembers.add("first", Name::first);
		 *     personMembers.add("last", Name::last);
		 * });
		 * </pre>
		 *
		 * <p>
		 * When used with a named member, the result will be added as a new JSON value
		 * object:
		 *
		 * <pre>
		 * {
		 *   "name": {
		 *     "first": "Jane",
		 *     "last": "Doe"
		 *   }
		 * }
		 * </pre>
		 *
		 * When used with an unnamed member the result will be added to the existing JSON
		 * object:
		 *
		 * <pre>
		 * {
		 *   "first": "John",
		 *   "last": "Doe"
		 * }
		 * </pre>
		 * @param members callback to configure the members
		 * @return a {@link Member} which may be configured further
		 * @see #usingExtractedPairs(BiConsumer, PairExtractor)
		 * @see #usingPairs(BiConsumer)
		 */
		public Member<T> usingMembers(Consumer<Members<T>> members) {
			Assert.notNull(members, "'members' must not be null");
			Assert.state(this.members == null, "Members cannot be declared multiple times");
			Assert.state(this.pairs == null, "Members cannot be declared when using pairs");
			this.members = new Members<>(members, this.name == null);
			return this;
		}

		/**
		 * Writes the given instance using details configure by this member.
		 * @param instance the instance to write
		 * @param valueWriter the JSON value writer to use
		 */
		void write(Object instance, JsonValueWriter valueWriter) {
			T extracted = this.extractor.extract(instance);
			if (Extractor.skip(extracted)) {
				return;
			}
			Object value = getValueToWrite(extracted, valueWriter);
			valueWriter.write(this.name, value);
		}

		private Object getValueToWrite(T extracted, JsonValueWriter valueWriter) {
			if (this.pairs != null) {
				return WritableJson.of((out) -> valueWriter.writePairs((pairs) -> this.pairs.accept(extracted, pairs)));
			}
			if (this.members != null) {
				return WritableJson.of((out) -> this.members.write(extracted, valueWriter));
			}
			return extracted;
		}

		/**
		 * Whether this contributes one or more name/value pairs to the JSON.
		 * @return whether a name/value pair is contributed
		 */
		boolean contributesPair() {
			return this.name != null || this.pairs != null || (this.members != null && this.members.contributesPair());
		}

		@Override
		public String toString() {
			return "Member at index " + this.index + ((this.name != null) ? "{%s}".formatted(this.name) : "");
		}

		/**
		 * Internal class used to manage member value extraction and filtering.
		 *
		 * @param <T> the member type
		 */
		@FunctionalInterface
		interface Extractor<T> {

			/**
			 * Represents a skipped value.
			 */
			Object SKIP = new Object();

			/**
			 * Extract the value from the given instance.
			 * @param instance the source instance
			 * @return the extracted value or {@link #SKIP}
			 */
			T extract(Object instance);

			/**
			 * Only extract when the given predicate matches.
			 * @param predicate the predicate to test
			 * @return a new {@link Extractor}
			 */
			default Extractor<T> when(Predicate<T> predicate) {
				return (instance) -> test(extract(instance), predicate);
			}

			@SuppressWarnings("unchecked")
			private T test(T extracted, Predicate<T> predicate) {
				return (!skip(extracted) && predicate.test(extracted)) ? extracted : (T) SKIP;
			}

			/**
			 * Adapt the extracted value.
			 * @param <R> the result type
			 * @param adapter the adapter to use
			 * @return a new {@link Extractor}
			 */
			default <R> Extractor<R> as(Function<T, R> adapter) {
				return (instance) -> apply(extract(instance), adapter);
			}

			@SuppressWarnings("unchecked")
			private <R> R apply(T extracted, Function<T, R> function) {
				if (skip(extracted)) {
					return (R) SKIP;
				}
				return (extracted != null) ? function.apply(extracted) : null;
			}

			/**
			 * Create a new {@link Extractor} based on the given {@link Function}.
			 * @param <S> the source type
			 * @param <T> the extracted type
			 * @param extractor the extractor to use
			 * @return a new {@link Extractor} instance
			 */
			@SuppressWarnings("unchecked")
			static <S, T> Extractor<T> of(Function<S, T> extractor) {
				return (instance) -> !skip(instance) ? extractor.apply((S) instance) : (T) SKIP;
			}

			/**
			 * Return if the extracted value should be skipped.
			 * @param <T> the value type
			 * @param extracted the value to test
			 * @return if the value is to be skipped
			 */
			static <T> boolean skip(T extracted) {
				return extracted == SKIP;
			}

		}

	}

	/**
	 * Interface that can be used to extract name/value pairs from an element.
	 *
	 * @param <E> the element type
	 */
	interface PairExtractor<E> {

		/**
		 * Extract the name.
		 * @param <N> the name type
		 * @param element the source element
		 * @return the extracted name
		 */
		<N> N getName(E element);

		/**
		 * Extract the name.
		 * @param <V> the value type
		 * @param element the source element
		 * @return the extracted value
		 */
		<V> V getValue(E element);

		/**
		 * Factory method to create a {@link PairExtractor} using distinct name and value
		 * extraction functions.
		 * @param <T> the element type
		 * @param nameExtractor the name extractor
		 * @param valueExtractor the value extraction
		 * @return a new {@link PairExtractor} instance
		 */
		static <T> PairExtractor<T> of(Function<T, ?> nameExtractor, Function<T, ?> valueExtractor) {
			Assert.notNull(nameExtractor, "'nameExtractor' must not be null");
			Assert.notNull(valueExtractor, "'valueExtractor' must not be null");
			return new PairExtractor<>() {

				@Override
				@SuppressWarnings("unchecked")
				public <N> N getName(T instance) {
					return (N) nameExtractor.apply(instance);
				}

				@Override
				@SuppressWarnings("unchecked")
				public <V> V getValue(T instance) {
					return (V) valueExtractor.apply(instance);
				}

			};
		}

	}

}
