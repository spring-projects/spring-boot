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

package org.springframework.boot.health.contributor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Base class for {@link Map} backed composites.
 *
 * @param <T> the composite type
 * @param <C> the contributor type
 * @param <E> the entry type
 * @author Phillip Webb
 * @author Guirong Hu
 */
abstract class MapCompositeTests<T, C, E> {

	@Test
	void createWhenMapIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> createWithData(null, Function.identity()))
			.withMessage("'map' must not be null");
	}

	@Test
	void createWhenValueAdapterIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> createWithData(Collections.emptyMap(), null))
			.withMessage("'valueAdapter' must not be null");
	}

	@Test
	void createWhenMapContainsNullValueThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> createWithData(Collections.singletonMap("test", null), Function.identity()))
			.withMessage("'map' must not contain null values");
	}

	@Test
	void createWhenMapContainsNullKeyThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> createWithData(Collections.singletonMap(null, "test"), Function.identity()))
			.withMessage("'map' must not contain null keys");
	}

	@Test
	void createWhenMapContainsKeyWithSlashThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> createWithData(Collections.singletonMap("test/key", "test"), Function.identity()))
			.withMessage("'map' keys must not contain a '/'");
	}

	@Test
	void streamReturnsAdaptedEntries() {
		T composite = create();
		List<E> streamed = stream(composite).toList();
		assertThat(streamed).hasSize(2);
		E one = streamed.get(0);
		E two = streamed.get(1);
		assertThat(getName(one)).isEqualTo("one");
		assertThat(getData(getContributor(one))).isEqualTo("eno");
		assertThat(getName(two)).isEqualTo("two");
		assertThat(getData(getContributor(two))).isEqualTo("owt");
	}

	@Test
	void getContributorReturnsAdaptedEntry() {
		T composite = create();
		assertThat(getContributorData(composite, "one")).isEqualTo("eno");
		assertThat(getContributorData(composite, "two")).isEqualTo("owt");
	}

	@Test
	void getContributorCallsAdaptersOnlyOnce() {
		Map<String, String> map = new LinkedHashMap<>();
		map.put("one", "one");
		map.put("two", "two");
		int callCount = map.size();
		AtomicInteger counter = new AtomicInteger(0);
		T composite = createWithData(map, (name) -> count(name, counter));
		assertThat(getContributorData(composite, "one")).isEqualTo("eno");
		assertThat(counter.get()).isEqualTo(callCount);
		assertThat(getContributorData(composite, "two")).isEqualTo("owt");
		assertThat(counter.get()).isEqualTo(callCount);
	}

	@Test
	void getContributorWhenNotInMapReturnsNull() {
		T composite = create();
		assertThat(getContributor(composite, "missing")).isNull();
	}

	private String count(CharSequence charSequence, AtomicInteger counter) {
		counter.incrementAndGet();
		return reverse(charSequence);
	}

	private String reverse(CharSequence charSequence) {
		return new StringBuilder(charSequence).reverse().toString();
	}

	private T create() {
		Map<String, String> map = new LinkedHashMap<>();
		map.put("one", "one");
		map.put("two", "two");
		return createWithData(map, this::reverse);
	}

	private T createWithData(Map<String, String> map, Function<String, String> dataAdapter) {
		return create(map, (dataAdapter != null) ? (key) -> createContributor(dataAdapter.apply(key)) : null);
	}

	private String getContributorData(T composite, String name) {
		return getData(getContributor(composite, name));
	}

	protected abstract T create(Map<String, String> map, Function<String, C> valueAdapter);

	protected abstract Stream<E> stream(T composite);

	protected abstract C getContributor(T composite, String name);

	protected abstract C createContributor(String data);

	protected abstract String getData(C contributor);

	protected abstract String getName(E entry);

	protected abstract C getContributor(E entry);

}
