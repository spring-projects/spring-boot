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

package org.springframework.boot.logging.structured;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.json.JsonWriter.PairExtractor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Helper that can be used to add JSON pairs from context data (typically the logger MDC)
 * in the correct location (or drop them altogether).
 *
 * @author Phillip Webb
 * @since 3.5.0
 */
public class ContextPairs {

	private final boolean include;

	private final String prefix;

	ContextPairs(boolean include, String prefix) {
		this.include = include;
		this.prefix = (prefix != null) ? prefix : "";
	}

	/**
	 * Add pairs using flat naming.
	 * @param <T> the item type
	 * @param delimeter the delimiter used if there is a prefix
	 * @param pairs callback to add all the pairs
	 * @return a {@link BiConsumer} for use with the {@link JsonWriter}
	 */
	public <T> BiConsumer<T, BiConsumer<String, Object>> flat(String delimeter, Consumer<Pairs<T>> pairs) {
		return flat(joinWith(delimeter), pairs);
	}

	/**
	 * Add pairs using flat naming.
	 * @param <T> the item type
	 * @param joiner the function used to join the prefix and name
	 * @param pairs callback to add all the pairs
	 * @return a {@link BiConsumer} for use with the {@link JsonWriter}
	 */
	public <T> BiConsumer<T, BiConsumer<String, Object>> flat(BinaryOperator<String> joiner, Consumer<Pairs<T>> pairs) {
		return (!this.include) ? none() : new Pairs<>(joiner, pairs)::flat;
	}

	/**
	 * Add pairs using nested naming (for example as used in ECS).
	 * @param <T> the item type
	 * @param pairs callback to add all the pairs
	 * @return a {@link BiConsumer} for use with the {@link JsonWriter}
	 */
	public <T> BiConsumer<T, BiConsumer<String, Object>> nested(Consumer<Pairs<T>> pairs) {
		return (!this.include) ? none() : new Pairs<>(joinWith("."), pairs)::nested;
	}

	private <T, V> BiConsumer<T, BiConsumer<String, V>> none() {
		return (item, pairs) -> {
		};
	}

	private BinaryOperator<String> joinWith(String delimeter) {
		return (prefix, name) -> {
			StringBuilder joined = new StringBuilder(prefix.length() + delimeter.length() + name.length());
			joined.append(prefix);
			if (!prefix.isEmpty() && !prefix.endsWith(delimeter) && !name.startsWith(delimeter)) {
				joined.append(delimeter);
			}
			joined.append(name);
			return joined.toString();
		};
	}

	/**
	 * Callback used to add pairs.
	 *
	 * @param <T> the item type
	 */
	public class Pairs<T> {

		private final BinaryOperator<String> joiner;

		private final List<BiConsumer<T, BiConsumer<String, ?>>> addedPairs;

		Pairs(BinaryOperator<String> joiner, Consumer<Pairs<T>> pairs) {
			this.joiner = joiner;
			this.addedPairs = new ArrayList<>();
			pairs.accept(this);
		}

		/**
		 * Add pairs from map entries.
		 * @param <K> the map key type
		 * @param <V> the map value type
		 * @param extractor the extractor used to provide the map
		 */
		public <K, V> void addMapEntries(Function<T, Map<String, V>> extractor) {
			add(extractor.andThen(Map::entrySet), Map.Entry::getKey, Map.Entry::getValue);
		}

		/**
		 * Add pairs from an iterable.
		 * @param elementsExtractor the extractor used to provide the iterable
		 * @param pairExtractor the extractor used to provide the name and value
		 * @param <E> the element type
		 * @param <V> the value type
		 */
		public <E, V> void add(Function<T, Iterable<E>> elementsExtractor, PairExtractor<E> pairExtractor) {
			add(elementsExtractor, pairExtractor::getName, pairExtractor::getValue);
		}

		/**
		 * Add pairs from an iterable.
		 * @param elementsExtractor the extractor used to provide the iterable
		 * @param <E> the element type
		 * @param <V> the value type
		 * @param nameExtractor the extractor used to provide the name
		 * @param valueExtractor the extractor used to provide the value
		 */
		public <E, V> void add(Function<T, Iterable<E>> elementsExtractor, Function<E, String> nameExtractor,
				Function<E, V> valueExtractor) {
			add((item, pairs) -> {
				Iterable<E> elements = elementsExtractor.apply(item);
				if (elements != null) {
					elements.forEach((element) -> {
						String name = nameExtractor.apply(element);
						V value = valueExtractor.apply(element);
						pairs.accept(name, value);
					});
				}
			});
		}

		/**
		 * Add pairs using the given callback.
		 * @param <V> the value type
		 * @param pairs callback provided with the item and consumer that can be called to
		 * actually add the pairs
		 */
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public <V> void add(BiConsumer<T, BiConsumer<String, V>> pairs) {
			this.addedPairs.add((BiConsumer) pairs);
		}

		void flat(T item, BiConsumer<String, Object> pairs) {
			this.addedPairs.forEach((action) -> action.accept(item, joining(pairs)));
		}

		@SuppressWarnings("unchecked")
		void nested(T item, BiConsumer<String, Object> pairs) {
			LinkedHashMap<String, Object> result = new LinkedHashMap<>();
			this.addedPairs.forEach((addedPair) -> {
				addedPair.accept(item, joining((name, value) -> {
					List<String> nameParts = List.of(name.split("\\."));
					Map<String, Object> destination = result;
					for (int i = 0; i < nameParts.size() - 1; i++) {
						Object existing = destination.computeIfAbsent(nameParts.get(i), (key) -> new LinkedHashMap<>());
						if (!(existing instanceof Map)) {
							String common = nameParts.subList(0, i + 1).stream().collect(Collectors.joining("."));
							throw new IllegalStateException(
									"Duplicate nested pairs added under '%s'".formatted(common));
						}
						destination = (Map<String, Object>) existing;
					}
					Object previous = destination.put(nameParts.get(nameParts.size() - 1), value);
					Assert.state(previous == null, () -> "Duplicate nested pairs added under '%s'".formatted(name));
				}));
			});
			result.forEach(pairs::accept);
		}

		private <V> BiConsumer<String, V> joining(BiConsumer<String, V> pairs) {
			return (name, value) -> {
				name = this.joiner.apply(ContextPairs.this.prefix, (name != null) ? name : "");
				if (StringUtils.hasLength(name)) {
					pairs.accept(name, value);
				}
			};
		}

	}

}
