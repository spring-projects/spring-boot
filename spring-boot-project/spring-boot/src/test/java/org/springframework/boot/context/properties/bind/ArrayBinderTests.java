/*
 * Copyright 2012-2020 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.InOrder;

import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MockConfigurationPropertySource;
import org.springframework.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ArrayBinder}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ArrayBinderTests {

	private static final Bindable<List<Integer>> INTEGER_LIST = Bindable.listOf(Integer.class);

	private static final Bindable<Integer[]> INTEGER_ARRAY = Bindable.of(Integer[].class);

	private List<ConfigurationPropertySource> sources = new ArrayList<>();

	private final Binder binder = new Binder(this.sources);

	@Test
	void bindToArrayShouldReturnArray() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo[0]", "1");
		source.put("foo[1]", "2");
		source.put("foo[2]", "3");
		this.sources.add(source);
		Integer[] result = this.binder.bind("foo", INTEGER_ARRAY).get();
		assertThat(result).containsExactly(1, 2, 3);
	}

	@Test
	void bindToCollectionShouldTriggerOnSuccess() {
		this.sources.add(new MockConfigurationPropertySource("foo[0]", "1", "line1"));
		BindHandler handler = mock(BindHandler.class, Answers.CALLS_REAL_METHODS);
		this.binder.bind("foo", INTEGER_LIST, handler);
		InOrder inOrder = inOrder(handler);
		inOrder.verify(handler).onSuccess(eq(ConfigurationPropertyName.of("foo[0]")), eq(Bindable.of(Integer.class)),
				any(), eq(1));
		inOrder.verify(handler).onSuccess(eq(ConfigurationPropertyName.of("foo")), eq(INTEGER_LIST), any(),
				isA(List.class));
	}

	@Test
	void bindToArrayShouldReturnPrimitiveArray() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo[0]", "1");
		source.put("foo[1]", "2");
		source.put("foo[2]", "3");
		this.sources.add(source);
		int[] result = this.binder.bind("foo", Bindable.of(int[].class)).get();
		assertThat(result).containsExactly(1, 2, 3);
	}

	@Test
	void bindToArrayWhenNestedShouldReturnPopulatedArray() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo[0][0]", "1");
		source.put("foo[0][1]", "2");
		source.put("foo[1][0]", "3");
		source.put("foo[1][1]", "4");
		this.sources.add(source);
		ResolvableType type = ResolvableType.forArrayComponent(INTEGER_ARRAY.getType());
		Bindable<Integer[][]> target = Bindable.of(type);
		Integer[][] result = this.binder.bind("foo", target).get();
		assertThat(result).hasDimensions(2, 2);
		assertThat(result[0]).containsExactly(1, 2);
		assertThat(result[1]).containsExactly(3, 4);
	}

	@Test
	void bindToArrayWhenNestedListShouldReturnPopulatedArray() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo[0][0]", "1");
		source.put("foo[0][1]", "2");
		source.put("foo[1][0]", "3");
		source.put("foo[1][1]", "4");
		this.sources.add(source);
		ResolvableType type = ResolvableType.forArrayComponent(INTEGER_LIST.getType());
		Bindable<List<Integer>[]> target = Bindable.of(type);
		List<Integer>[] result = this.binder.bind("foo", target).get();
		assertThat(result).hasSize(2);
		assertThat(result[0]).containsExactly(1, 2);
		assertThat(result[1]).containsExactly(3, 4);
	}

	@Test
	void bindToArrayWhenNotInOrderShouldReturnPopulatedArray() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo[1]", "2");
		source.put("foo[0]", "1");
		source.put("foo[2]", "3");
		this.sources.add(source);
		Integer[] result = this.binder.bind("foo", INTEGER_ARRAY).get();
		assertThat(result).containsExactly(1, 2, 3);
	}

	@Test
	void bindToArrayWhenNonSequentialShouldThrowException() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo[0]", "2");
		source.put("foo[1]", "1");
		source.put("foo[3]", "3");
		this.sources.add(source);
		assertThatExceptionOfType(BindException.class).isThrownBy(() -> this.binder.bind("foo", INTEGER_ARRAY))
				.satisfies((ex) -> {
					Set<ConfigurationProperty> unbound = ((UnboundConfigurationPropertiesException) ex.getCause())
							.getUnboundProperties();
					assertThat(unbound.size()).isEqualTo(1);
					ConfigurationProperty property = unbound.iterator().next();
					assertThat(property.getName().toString()).isEqualTo("foo[3]");
					assertThat(property.getValue()).isEqualTo("3");
				});
	}

	@Test
	void bindToArrayWhenNonIterableShouldReturnPopulatedArray() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo[1]", "2");
		source.put("foo[0]", "1");
		source.put("foo[2]", "3");
		this.sources.add(source.nonIterable());
		Integer[] result = this.binder.bind("foo", INTEGER_ARRAY).get();
		assertThat(result).containsExactly(1, 2, 3);
	}

	@Test
	void bindToArrayWhenMultipleSourceShouldOnlyUseFirst() {
		MockConfigurationPropertySource source1 = new MockConfigurationPropertySource();
		source1.put("bar", "baz");
		this.sources.add(source1);
		MockConfigurationPropertySource source2 = new MockConfigurationPropertySource();
		source2.put("foo[0]", "1");
		source2.put("foo[1]", "2");
		this.sources.add(source2);
		MockConfigurationPropertySource source3 = new MockConfigurationPropertySource();
		source3.put("foo[0]", "7");
		source3.put("foo[1]", "8");
		source3.put("foo[2]", "9");
		this.sources.add(source3);
		Integer[] result = this.binder.bind("foo", INTEGER_ARRAY).get();
		assertThat(result).containsExactly(1, 2);
	}

	@Test
	void bindToArrayWhenHasExistingCollectionShouldReplaceAllContents() {
		this.sources.add(new MockConfigurationPropertySource("foo[0]", "1"));
		Integer[] existing = new Integer[2];
		existing[0] = 1000;
		existing[1] = 1001;
		Integer[] result = this.binder.bind("foo", INTEGER_ARRAY.withExistingValue(existing)).get();
		assertThat(result).containsExactly(1);
	}

	@Test
	void bindToArrayWhenNoValueShouldReturnUnbound() {
		this.sources.add(new MockConfigurationPropertySource("faf.bar", "1"));
		BindResult<Integer[]> result = this.binder.bind("foo", INTEGER_ARRAY);
		assertThat(result.isBound()).isFalse();
	}

	@Test
	void bindToArrayShouldTriggerOnSuccess() {
		this.sources.add(new MockConfigurationPropertySource("foo[0]", "1", "line1"));
		BindHandler handler = mock(BindHandler.class, Answers.CALLS_REAL_METHODS);
		Bindable<Integer[]> target = INTEGER_ARRAY;
		this.binder.bind("foo", target, handler);
		InOrder inOrder = inOrder(handler);
		inOrder.verify(handler).onSuccess(eq(ConfigurationPropertyName.of("foo[0]")), eq(Bindable.of(Integer.class)),
				any(), eq(1));
		inOrder.verify(handler).onSuccess(eq(ConfigurationPropertyName.of("foo")), eq(target), any(),
				isA(Integer[].class));
	}

	@Test
	void bindToArrayWhenCommaListShouldReturnPopulatedArray() {
		this.sources.add(new MockConfigurationPropertySource("foo", "1,2,3"));
		int[] result = this.binder.bind("foo", Bindable.of(int[].class)).get();
		assertThat(result).containsExactly(1, 2, 3);
	}

	@Test
	void bindToArrayWhenCommaListAndIndexedShouldOnlyUseFirst() {
		MockConfigurationPropertySource source1 = new MockConfigurationPropertySource();
		source1.put("foo", "1,2");
		this.sources.add(source1);
		MockConfigurationPropertySource source2 = new MockConfigurationPropertySource();
		source2.put("foo[0]", "2");
		source2.put("foo[1]", "3");
		int[] result = this.binder.bind("foo", Bindable.of(int[].class)).get();
		assertThat(result).containsExactly(1, 2);
	}

	@Test
	void bindToArrayWhenIndexedAndCommaListShouldOnlyUseFirst() {
		MockConfigurationPropertySource source1 = new MockConfigurationPropertySource();
		source1.put("foo[0]", "1");
		source1.put("foo[1]", "2");
		this.sources.add(source1);
		MockConfigurationPropertySource source2 = new MockConfigurationPropertySource();
		source2.put("foo", "2,3");
		int[] result = this.binder.bind("foo", Bindable.of(int[].class)).get();
		assertThat(result).containsExactly(1, 2);
	}

	@Test
	void bindToArrayShouldBindCharArray() {
		this.sources.add(new MockConfigurationPropertySource("foo", "word"));
		char[] result = this.binder.bind("foo", Bindable.of(char[].class)).get();
		assertThat(result).containsExactly("word".toCharArray());
	}

	@Test
	void bindToArrayWhenEmptyStringShouldReturnEmptyArray() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo", "");
		this.sources.add(source);
		String[] result = this.binder.bind("foo", Bindable.of(String[].class)).get();
		assertThat(result).isNotNull().isEmpty();
	}

	@Test
	void bindToArrayWhenHasSpacesShouldTrim() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo", "1,  2,3");
		this.sources.add(source);
		String[] result = this.binder.bind("foo", Bindable.of(String[].class)).get();
		assertThat(result).containsExactly("1", "2", "3");
	}

	@Test
	void bindToArrayShouldUsePropertyEditor() {
		// gh-12166
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo[0]", "java.lang.RuntimeException");
		source.put("foo[1]", "java.lang.IllegalStateException");
		this.sources.add(source);
		assertThat(this.binder.bind("foo", Bindable.of(Class[].class)).get()).containsExactly(RuntimeException.class,
				IllegalStateException.class);
	}

	@Test
	void bindToArrayWhenStringShouldUsePropertyEditor() {
		// gh-12166
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo", "java.lang.RuntimeException,java.lang.IllegalStateException");
		this.sources.add(source);
		assertThat(this.binder.bind("foo", Bindable.of(Class[].class)).get()).containsExactly(RuntimeException.class,
				IllegalStateException.class);
	}

}
