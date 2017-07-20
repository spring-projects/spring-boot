/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.properties.bind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.context.properties.bind.BinderTests.JavaBean;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MockConfigurationPropertySource;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for {@link CollectionBinder}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
public class CollectionBinderTests {

	private static final Bindable<List<Integer>> INTEGER_LIST = Bindable
			.listOf(Integer.class);

	private static final Bindable<List<String>> STRING_LIST = Bindable
			.listOf(String.class);

	private static final Bindable<Set<String>> STRING_SET = Bindable.setOf(String.class);

	private List<ConfigurationPropertySource> sources = new ArrayList<>();

	private Binder binder;

	@Before
	public void setup() {
		this.binder = new Binder(this.sources);
	}

	@Test
	public void bindToCollectionShouldReturnPopulatedCollection() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo[0]", "1");
		source.put("foo[1]", "2");
		source.put("foo[2]", "3");
		this.sources.add(source);
		List<Integer> result = this.binder.bind("foo", INTEGER_LIST).get();
		assertThat(result).containsExactly(1, 2, 3);
	}

	@Test
	public void bindToSetShouldReturnPopulatedCollection() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo[0]", "a");
		source.put("foo[1]", "b");
		source.put("foo[2]", "c");
		this.sources.add(source);
		Set<String> result = this.binder.bind("foo", STRING_SET).get();
		assertThat(result).containsExactly("a", "b", "c");
	}

	@Test
	public void bindToCollectionWhenNestedShouldReturnPopulatedCollection()
			throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo[0][0]", "1");
		source.put("foo[0][1]", "2");
		source.put("foo[1][0]", "3");
		source.put("foo[1][1]", "4");
		this.sources.add(source);
		Bindable<List<List<Integer>>> target = Bindable.of(
				ResolvableType.forClassWithGenerics(List.class, INTEGER_LIST.getType()));
		List<List<Integer>> result = this.binder.bind("foo", target).get();
		assertThat(result).hasSize(2);
		assertThat(result.get(0)).containsExactly(1, 2);
		assertThat(result.get(1)).containsExactly(3, 4);
	}

	@Test
	public void bindToCollectionWhenNotInOrderShouldReturnPopulatedCollection()
			throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo[1]", "2");
		source.put("foo[0]", "1");
		source.put("foo[2]", "3");
		this.sources.add(source);
		List<Integer> result = this.binder.bind("foo", INTEGER_LIST).get();
		assertThat(result).containsExactly(1, 2, 3);
	}

	@Test
	public void bindToCollectionWhenNonSequentialShouldThrowException() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo[0]", "2");
		source.put("foo[1]", "1");
		source.put("foo[3]", "3");
		this.sources.add(source);
		try {
			this.binder.bind("foo", INTEGER_LIST);
			fail("No exception thrown");
		}
		catch (BindException ex) {
			ex.printStackTrace();
			Set<ConfigurationProperty> unbound = ((UnboundConfigurationPropertiesException) ex
					.getCause()).getUnboundProperties();
			assertThat(unbound).hasSize(1);
			ConfigurationProperty property = unbound.iterator().next();
			assertThat(property.getName().toString()).isEqualTo("foo[3]");
			assertThat(property.getValue()).isEqualTo("3");
		}
	}

	@Test
	public void bindToCollectionWhenNonIterableShouldReturnPopulatedCollection()
			throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo[1]", "2");
		source.put("foo[0]", "1");
		source.put("foo[2]", "3");
		this.sources.add(source.nonIterable());
		List<Integer> result = this.binder.bind("foo", INTEGER_LIST).get();
		assertThat(result).containsExactly(1, 2, 3);
	}

	@Test
	public void bindToCollectionWhenMultipleSourceShouldOnlyUseFirst() throws Exception {
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
		List<Integer> result = this.binder.bind("foo", INTEGER_LIST).get();
		assertThat(result).containsExactly(1, 2);
	}

	@Test
	public void bindToCollectionWhenHasExistingCollectionShouldReplaceAllContents()
			throws Exception {
		this.sources.add(new MockConfigurationPropertySource("foo[0]", "1"));
		List<Integer> existing = new LinkedList<>();
		existing.add(1000);
		existing.add(1001);
		List<Integer> result = this.binder
				.bind("foo", INTEGER_LIST.withExistingValue(existing)).get();
		assertThat(result).isExactlyInstanceOf(LinkedList.class);
		assertThat(result).isSameAs(existing);
		assertThat(result).containsExactly(1);
	}

	@Test
	public void bindToCollectionWhenHasExistingCollectionButNoValueShouldReturnUnbound()
			throws Exception {
		this.sources.add(new MockConfigurationPropertySource("faf[0]", "1"));
		List<Integer> existing = new LinkedList<>();
		existing.add(1000);
		BindResult<List<Integer>> result = this.binder.bind("foo",
				INTEGER_LIST.withExistingValue(existing));
		assertThat(result.isBound()).isFalse();
	}

	@Test
	public void bindToCollectionShouldRespectCollectionType() throws Exception {
		this.sources.add(new MockConfigurationPropertySource("foo[0]", "1"));
		ResolvableType type = ResolvableType.forClassWithGenerics(LinkedList.class,
				Integer.class);
		Object defaultList = this.binder.bind("foo", INTEGER_LIST).get();
		Object customList = this.binder.bind("foo", Bindable.of(type)).get();
		assertThat(customList).isExactlyInstanceOf(LinkedList.class)
				.isNotInstanceOf(defaultList.getClass());
	}

	@Test
	public void bindToCollectionWhenNoValueShouldReturnUnbound() throws Exception {
		this.sources.add(new MockConfigurationPropertySource("faf.bar", "1"));
		BindResult<List<Integer>> result = this.binder.bind("foo", INTEGER_LIST);
		assertThat(result.isBound()).isFalse();
	}

	@Test
	public void bindToCollectionWhenCommaListShouldReturnPopulatedCollection()
			throws Exception {
		this.sources.add(new MockConfigurationPropertySource("foo", "1,2,3"));
		List<Integer> result = this.binder.bind("foo", INTEGER_LIST).get();
		assertThat(result).containsExactly(1, 2, 3);
	}

	@Test
	public void bindToCollectionWhenCommaListWithPlaceholdersShouldReturnPopulatedCollection()
			throws Exception {
		StandardEnvironment environment = new StandardEnvironment();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(environment,
				"bar=1,2,3");
		this.binder = new Binder(this.sources,
				new PropertySourcesPlaceholdersResolver(environment));
		this.sources.add(new MockConfigurationPropertySource("foo", "${bar}"));
		List<Integer> result = this.binder.bind("foo", INTEGER_LIST).get();
		assertThat(result).containsExactly(1, 2, 3);

	}

	@Test
	public void bindToCollectionWhenCommaListAndIndexedShouldOnlyUseFirst()
			throws Exception {
		MockConfigurationPropertySource source1 = new MockConfigurationPropertySource();
		source1.put("foo", "1,2");
		this.sources.add(source1);
		MockConfigurationPropertySource source2 = new MockConfigurationPropertySource();
		source2.put("foo[0]", "2");
		source2.put("foo[1]", "3");
		List<Integer> result = this.binder.bind("foo", INTEGER_LIST).get();
		assertThat(result).containsExactly(1, 2);
	}

	@Test
	public void bindToCollectionWhenIndexedAndCommaListShouldOnlyUseFirst()
			throws Exception {
		MockConfigurationPropertySource source1 = new MockConfigurationPropertySource();
		source1.put("foo[0]", "1");
		source1.put("foo[1]", "2");
		this.sources.add(source1);
		MockConfigurationPropertySource source2 = new MockConfigurationPropertySource();
		source2.put("foo", "2,3");
		List<Integer> result = this.binder.bind("foo", INTEGER_LIST).get();
		assertThat(result).containsExactly(1, 2);
	}

	@Test
	public void bindToCollectionWhenItemContainsCommasShouldReturnPopulatedCollection()
			throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo[0]", "1,2");
		source.put("foo[1]", "3");
		this.sources.add(source);
		List<String> result = this.binder.bind("foo", STRING_LIST).get();
		assertThat(result).containsExactly("1,2", "3");
	}

	@Test
	public void bindToCollectionWhenEmptyStringShouldReturnEmptyCollection()
			throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo", "");
		this.sources.add(source);
		List<String> result = this.binder.bind("foo", STRING_LIST).get();
		assertThat(result).isNotNull().isEmpty();
	}

	@Test
	public void bindToNonScalarCollectionShouldReturnPopulatedCollection()
			throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo[0].value", "a");
		source.put("foo[1].value", "b");
		source.put("foo[2].value", "c");
		this.sources.add(source);
		Bindable<List<JavaBean>> target = Bindable.listOf(JavaBean.class);
		List<JavaBean> result = this.binder.bind("foo", target).get();
		assertThat(result).hasSize(3);
		List<String> values = result.stream().map(JavaBean::getValue)
				.collect(Collectors.toList());
		assertThat(values).containsExactly("a", "b", "c");
	}

	@Test
	public void bindToImmutableCollectionShouldReturnPopulatedCollection()
			throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.values", "a,b,c");
		this.sources.add(source);
		Set<String> result = this.binder
				.bind("foo.values", STRING_SET.withExistingValue(Collections.emptySet()))
				.get();
		assertThat(result).hasSize(3);
	}

	@Test
	public void bindToCollectionShouldAlsoCallSetterIfPresent() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.items", "a,b,c");
		this.sources.add(source);
		ExampleCollectionBean result = this.binder
				.bind("foo", ExampleCollectionBean.class).get();
		assertThat(result.getItems()).hasSize(4);
		assertThat(result.getItems()).containsExactly("a", "b", "c", "d");
	}

	public static class ExampleCollectionBean {

		private List<String> items = new ArrayList<>();

		public List<String> getItems() {
			return this.items;
		}

		public void setItems(List<String> items) {
			this.items.add("d");
		}

	}

}
