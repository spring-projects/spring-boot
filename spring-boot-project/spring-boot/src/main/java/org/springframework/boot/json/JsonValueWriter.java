/*
 * Copyright 2012-2025 the original author or authors.
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.springframework.boot.json.JsonWriter.MemberPath;
import org.springframework.boot.json.JsonWriter.NameProcessor;
import org.springframework.boot.json.JsonWriter.ValueProcessor;
import org.springframework.boot.util.LambdaSafe;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.function.ThrowingConsumer;

/**
 * Internal class used by {@link JsonWriter} to handle the lower-level concerns of writing
 * JSON.
 *
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class JsonValueWriter {

	private final Appendable out;

	private MemberPath path = MemberPath.ROOT;

	private final Deque<JsonWriterFiltersAndProcessors> filtersAndProcessors = new ArrayDeque<>();

	private final Deque<ActiveSeries> activeSeries = new ArrayDeque<>();

	/**
	 * Create a new {@link JsonValueWriter} instance.
	 * @param out the {@link Appendable} used to receive the JSON output
	 */
	JsonValueWriter(Appendable out) {
		this.out = out;
	}

	void pushProcessors(JsonWriterFiltersAndProcessors jsonProcessors) {
		this.filtersAndProcessors.addLast(jsonProcessors);
	}

	void popProcessors() {
		this.filtersAndProcessors.removeLast();
	}

	/**
	 * Write a name value pair, or just a value if {@code name} is {@code null}.
	 * @param <N> the name type in the pair
	 * @param <V> the value type in the pair
	 * @param name the name of the pair or {@code null} if only the value should be
	 * written
	 * @param value the value
	 */
	<N, V> void write(N name, V value) {
		if (name != null) {
			writePair(name, value);
		}
		else {
			write(value);
		}
	}

	/**
	 * Write a value to the JSON output. The following value types are supported:
	 * <ul>
	 * <li>Any {@code null} value</li>
	 * <li>A {@link WritableJson} instance</li>
	 * <li>Any {@link Iterable} or Array (written as a JSON array)</li>
	 * <li>A {@link Map} (written as a JSON object)</li>
	 * <li>Any {@link Number}</li>
	 * <li>A {@link Boolean}</li>
	 * </ul>
	 * All other values are written as JSON strings.
	 * @param <V> the value type
	 * @param value the value to write
	 */
	<V> void write(V value) {
		value = processValue(value);
		if (value == null) {
			append("null");
		}
		else if (value instanceof WritableJson writableJson) {
			try {
				writableJson.to(this.out);
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}
		else if (value instanceof Iterable<?> iterable) {
			writeArray(iterable::forEach);
		}
		else if (ObjectUtils.isArray(value)) {
			writeArray(Arrays.asList(ObjectUtils.toObjectArray(value))::forEach);
		}
		else if (value instanceof Map<?, ?> map) {
			writeObject(map::forEach);
		}
		else if (value instanceof Number || value instanceof Boolean) {
			append(value.toString());
		}
		else {
			writeString(value);
		}
	}

	/**
	 * Start a new {@link Series} (JSON object or array).
	 * @param series the series to start
	 * @see #end(Series)
	 * @see #writePairs(Consumer)
	 * @see #writeElements(Consumer)
	 */
	void start(Series series) {
		if (series != null) {
			this.activeSeries.push(new ActiveSeries(series));
			append(series.openChar);
		}
	}

	/**
	 * End an active {@link Series} (JSON object or array).
	 * @param series the series type being ended (must match {@link #start(Series)})
	 * @see #start(Series)
	 */
	void end(Series series) {
		if (series != null) {
			this.activeSeries.pop();
			append(series.closeChar);
		}
	}

	/**
	 * Write the specified elements to a newly started {@link Series#ARRAY array series}.
	 * @param <E> the element type
	 * @param elements a callback that will be used to provide each element. Typically a
	 * {@code forEach} method reference.
	 * @see #writeElements(Consumer)
	 */
	<E> void writeArray(Consumer<Consumer<E>> elements) {
		start(Series.ARRAY);
		elements.accept(ThrowingConsumer.of(this::writeElement));
		end(Series.ARRAY);
	}

	/**
	 * Write the specified elements to an already started {@link Series#ARRAY array
	 * series}.
	 * @param <E> the element type
	 * @param elements a callback that will be used to provide each element. Typically a
	 * {@code forEach} method reference.
	 * @see #writeElements(Consumer)
	 */
	<E> void writeElements(Consumer<Consumer<E>> elements) {
		elements.accept(ThrowingConsumer.of(this::writeElement));
	}

	<E> void writeElement(E element) {
		ActiveSeries activeSeries = this.activeSeries.peek();
		Assert.state(activeSeries != null, "No series has been started");
		this.path = activeSeries.updatePath(this.path);
		activeSeries.incrementIndexAndAddCommaIfRequired();
		write(element);
		this.path = activeSeries.restorePath(this.path);
	}

	/**
	 * Write the specified pairs to a newly started {@link Series#OBJECT object series}.
	 * @param <N> the name type in the pair
	 * @param <V> the value type in the pair
	 * @param pairs a callback that will be used to provide each pair. Typically a
	 * {@code forEach} method reference.
	 * @see #writePairs(Consumer)
	 */
	<N, V> void writeObject(Consumer<BiConsumer<N, V>> pairs) {
		start(Series.OBJECT);
		pairs.accept(this::writePair);
		end(Series.OBJECT);
	}

	/**
	 * Write the specified pairs to an already started {@link Series#OBJECT object
	 * series}.
	 * @param <N> the name type in the pair
	 * @param <V> the value type in the pair
	 * @param pairs a callback that will be used to provide each pair. Typically a
	 * {@code forEach} method reference.
	 * @see #writePairs(Consumer)
	 */
	<N, V> void writePairs(Consumer<BiConsumer<N, V>> pairs) {
		pairs.accept(this::writePair);
	}

	private <N, V> void writePair(N name, V value) {
		this.path = this.path.child(name.toString());
		if (!isFilteredPath()) {
			String processedName = processName(name.toString());
			ActiveSeries activeSeries = this.activeSeries.peek();
			Assert.state(activeSeries != null, "No series has been started");
			activeSeries.incrementIndexAndAddCommaIfRequired();
			Assert.state(activeSeries.addName(processedName),
					() -> "The name '" + processedName + "' has already been written");
			writeString(processedName);
			append(":");
			write(value);
		}
		this.path = this.path.parent();
	}

	private void writeString(Object value) {
		try {
			this.out.append('"');
			String string = value.toString();
			for (int i = 0; i < string.length(); i++) {
				char ch = string.charAt(i);
				switch (ch) {
					case '"' -> this.out.append("\\\"");
					case '\\' -> this.out.append("\\\\");
					case '/' -> this.out.append("\\/");
					case '\b' -> this.out.append("\\b");
					case '\f' -> this.out.append("\\f");
					case '\n' -> this.out.append("\\n");
					case '\r' -> this.out.append("\\r");
					case '\t' -> this.out.append("\\t");
					default -> {
						if (Character.isISOControl(ch)) {
							this.out.append("\\u");
							this.out.append(String.format("%04X", (int) ch));
						}
						else {
							this.out.append(ch);
						}
					}
				}
			}
			this.out.append('"');
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private void append(String value) {
		try {
			this.out.append(value);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}

	}

	private void append(char ch) {
		try {
			this.out.append(ch);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private boolean isFilteredPath() {
		for (JsonWriterFiltersAndProcessors filtersAndProcessors : this.filtersAndProcessors) {
			for (Predicate<MemberPath> pathFilter : filtersAndProcessors.pathFilters()) {
				if (pathFilter.test(this.path)) {
					return true;
				}
			}
		}
		return false;
	}

	private String processName(String name) {
		for (JsonWriterFiltersAndProcessors filtersAndProcessors : this.filtersAndProcessors) {
			for (NameProcessor nameProcessor : filtersAndProcessors.nameProcessors()) {
				name = processName(name, nameProcessor);
			}
		}
		return name;
	}

	private String processName(String name, NameProcessor nameProcessor) {
		name = nameProcessor.processName(this.path, name);
		Assert.state(StringUtils.hasLength(name), "NameProcessor " + nameProcessor + " returned an empty result");
		return name;
	}

	private <V> V processValue(V value) {
		for (JsonWriterFiltersAndProcessors filtersAndProcessors : this.filtersAndProcessors) {
			for (ValueProcessor<?> valueProcessor : filtersAndProcessors.valueProcessors()) {
				value = processValue(value, valueProcessor);
			}
		}
		return value;
	}

	@SuppressWarnings({ "unchecked", "unchecked" })
	private <V> V processValue(V value, ValueProcessor<?> valueProcessor) {
		return (V) LambdaSafe.callback(ValueProcessor.class, valueProcessor, this.path, value)
			.invokeAnd((call) -> call.processValue(this.path, value))
			.get(value);
	}

	/**
	 * A series of items that can be written to the JSON output.
	 */
	enum Series {

		/**
		 * A JSON object series consisting of name/value pairs.
		 */
		OBJECT('{', '}'),

		/**
		 * A JSON array series consisting of elements.
		 */
		ARRAY('[', ']');

		final char openChar;

		final char closeChar;

		Series(char openChar, char closeChar) {
			this.openChar = openChar;
			this.closeChar = closeChar;
		}

	}

	/**
	 * Details of the currently active {@link Series}.
	 */
	private final class ActiveSeries {

		private final Series series;

		private int index;

		private Set<String> names = new HashSet<>();

		private ActiveSeries(Series series) {
			this.series = series;
		}

		boolean addName(String processedName) {
			return this.names.add(processedName);
		}

		MemberPath updatePath(MemberPath path) {
			return (this.series != Series.ARRAY) ? path : path.child(this.index);
		}

		MemberPath restorePath(MemberPath path) {
			return (this.series != Series.ARRAY) ? path : path.parent();
		}

		void incrementIndexAndAddCommaIfRequired() {
			if (this.index > 0) {
				append(',');
			}
			this.index++;
		}

	}

}
