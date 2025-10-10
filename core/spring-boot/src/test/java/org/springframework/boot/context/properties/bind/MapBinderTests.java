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

package org.springframework.boot.context.properties.bind;

import java.net.InetAddress;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.boot.context.properties.bind.BinderTests.ExampleEnum;
import org.springframework.boot.context.properties.bind.BinderTests.JavaBean;
import org.springframework.boot.context.properties.bind.MapBinderTests.CustomMapWithoutDefaultCtor.CustomMap;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MockConfigurationPropertySource;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MapBinder}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class MapBinderTests {

	private static final Bindable<Map<String, String>> STRING_STRING_MAP = Bindable.mapOf(String.class, String.class);

	private static final Bindable<Map<String, Integer>> STRING_INTEGER_MAP = Bindable.mapOf(String.class,
			Integer.class);

	private static final Bindable<Map<Integer, Integer>> INTEGER_INTEGER_MAP = Bindable.mapOf(Integer.class,
			Integer.class);

	private static final Bindable<Map<String, Object>> STRING_OBJECT_MAP = Bindable.mapOf(String.class, Object.class);

	private static final Bindable<Map<String, String[]>> STRING_ARRAY_MAP = Bindable.mapOf(String.class,
			String[].class);

	private static final Bindable<EnumMap<ExampleEnum, String>> EXAMPLE_ENUM_STRING_ENUM_MAP = Bindable
		.of(ResolvableType.forClassWithGenerics(EnumMap.class, ExampleEnum.class, String.class));

	private final List<ConfigurationPropertySource> sources = new ArrayList<>();

	private final Binder binder = new Binder(this.sources);

	@Test
	void bindToMapShouldReturnPopulatedMap() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.bar", "1");
		source.put("foo.[baz]", "2");
		source.put("foo[BiNg]", "3");
		this.sources.add(source);
		Map<String, String> result = this.binder.bind("foo", STRING_STRING_MAP).get();
		assertThat(result).hasSize(3);
		assertThat(result).containsEntry("bar", "1");
		assertThat(result).containsEntry("baz", "2");
		assertThat(result).containsEntry("BiNg", "3");
	}

	@Test
	@SuppressWarnings("unchecked")
	void bindToMapWithEmptyPrefix() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.bar", "1");
		this.sources.add(source);
		Map<String, Object> result = this.binder.bind("", STRING_OBJECT_MAP).get();
		assertThat((Map<String, Object>) result.get("foo")).containsEntry("bar", "1");
	}

	@Test
	void bindToMapShouldConvertMapValue() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.bar", "1");
		source.put("foo.[baz]", "2");
		source.put("foo[BiNg]", "3");
		source.put("faf.bar", "x");
		this.sources.add(source);
		Map<String, Integer> result = this.binder.bind("foo", STRING_INTEGER_MAP).get();
		assertThat(result).hasSize(3);
		assertThat(result).containsEntry("bar", 1);
		assertThat(result).containsEntry("baz", 2);
		assertThat(result).containsEntry("BiNg", 3);
	}

	@Test
	void bindToMapShouldBindToMapValue() {
		ResolvableType type = ResolvableType.forClassWithGenerics(Map.class, ResolvableType.forClass(String.class),
				STRING_INTEGER_MAP.getType());
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.bar.baz", "1");
		source.put("foo.bar.bin", "2");
		source.put("foo.far.baz", "3");
		source.put("foo.far.bin", "4");
		source.put("faf.far.bin", "x");
		this.sources.add(source);
		Map<String, Map<String, Integer>> result = this.binder
			.bind("foo", Bindable.<Map<String, Map<String, Integer>>>of(type))
			.get();
		assertThat(result).hasSize(2);
		assertThat(result.get("bar")).containsEntry("baz", 1).containsEntry("bin", 2);
		assertThat(result.get("far")).containsEntry("baz", 3).containsEntry("bin", 4);
	}

	@Test
	void bindToMapShouldBindNestedMapValue() {
		ResolvableType nestedType = ResolvableType.forClassWithGenerics(Map.class,
				ResolvableType.forClass(String.class), STRING_INTEGER_MAP.getType());
		ResolvableType type = ResolvableType.forClassWithGenerics(Map.class, ResolvableType.forClass(String.class),
				nestedType);
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.nested.bar.baz", "1");
		source.put("foo.nested.bar.bin", "2");
		source.put("foo.nested.far.baz", "3");
		source.put("foo.nested.far.bin", "4");
		source.put("faf.nested.far.bin", "x");
		this.sources.add(source);
		Bindable<Map<String, Map<String, Map<String, Integer>>>> target = Bindable.of(type);
		Map<String, Map<String, Map<String, Integer>>> result = this.binder.bind("foo", target).get();
		Map<String, Map<String, Integer>> nested = result.get("nested");
		assertThat(nested).hasSize(2);
		assertThat(nested.get("bar")).containsEntry("baz", 1).containsEntry("bin", 2);
		assertThat(nested.get("far")).containsEntry("baz", 3).containsEntry("bin", 4);
	}

	@Test
	@SuppressWarnings("unchecked")
	void bindToMapWhenMapValueIsObjectShouldBindNestedMapValue() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.nested.bar.baz", "1");
		source.put("foo.nested.bar.bin", "2");
		source.put("foo.nested.far.baz", "3");
		source.put("foo.nested.far.bin", "4");
		source.put("faf.nested.far.bin", "x");
		this.sources.add(source);
		Map<String, Object> result = this.binder.bind("foo", Bindable.mapOf(String.class, Object.class)).get();
		Map<String, Object> nested = (Map<String, Object>) result.get("nested");
		assertThat(nested).hasSize(2);
		Map<String, Object> bar = (Map<String, Object>) nested.get("bar");
		assertThat(bar).containsEntry("baz", "1").containsEntry("bin", "2");
		Map<String, Object> far = (Map<String, Object>) nested.get("far");
		assertThat(far).containsEntry("baz", "3").containsEntry("bin", "4");
	}

	@Test
	void bindToMapWhenMapValueIsObjectAndNoRootShouldBindNestedMapValue() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("commit.id", "abcdefg");
		source.put("branch", "master");
		source.put("foo", "bar");
		this.sources.add(source);
		Map<String, Object> result = this.binder.bind("", Bindable.mapOf(String.class, Object.class)).get();
		assertThat(result).containsEntry("commit", Collections.singletonMap("id", "abcdefg"));
		assertThat(result).containsEntry("branch", "master");
		assertThat(result).containsEntry("foo", "bar");
	}

	@Test
	void bindToMapWhenEmptyRootNameShouldBindMap() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("bar.baz", "1");
		source.put("bar.bin", "2");
		this.sources.add(source);
		Map<String, Integer> result = this.binder.bind("", STRING_INTEGER_MAP).get();
		assertThat(result).hasSize(2);
		assertThat(result).containsEntry("bar.baz", 1).containsEntry("bar.bin", 2);
	}

	@Test
	void bindToMapWhenMultipleCandidateShouldBindFirst() {
		MockConfigurationPropertySource source1 = new MockConfigurationPropertySource();
		source1.put("foo.bar", "1");
		source1.put("foo.baz", "2");
		this.sources.add(source1);
		MockConfigurationPropertySource source2 = new MockConfigurationPropertySource();
		source2.put("foo.baz", "3");
		source2.put("foo.bin", "4");
		this.sources.add(source2);
		Map<String, Integer> result = this.binder.bind("foo", STRING_INTEGER_MAP).get();
		assertThat(result).hasSize(3);
		assertThat(result).containsEntry("bar", 1);
		assertThat(result).containsEntry("baz", 2);
		assertThat(result).containsEntry("bin", 4);
	}

	@Test
	void bindToMapWhenMultipleInSameSourceCandidateShouldBindFirst() {
		Map<String, Object> map = new HashMap<>();
		map.put("foo.bar", "1");
		map.put("foo.b-az", "2");
		map.put("foo.ba-z", "3");
		map.put("foo.bin", "4");
		MapConfigurationPropertySource propertySource = new MapConfigurationPropertySource(map);
		this.sources.add(propertySource);
		Map<String, Integer> result = this.binder.bind("foo", STRING_INTEGER_MAP).get();
		assertThat(result).hasSize(4);
		assertThat(result).containsEntry("bar", 1);
		assertThat(result).containsEntry("b-az", 2);
		assertThat(result).containsEntry("ba-z", 3);
		assertThat(result).containsEntry("bin", 4);
	}

	@Test
	void bindToMapWhenHasExistingMapShouldReplaceOnlyNewContents() {
		this.sources.add(new MockConfigurationPropertySource("foo.bar", "1"));
		Map<String, Integer> existing = new HashMap<>();
		existing.put("bar", 1000);
		existing.put("baz", 1001);
		Bindable<Map<String, Integer>> target = STRING_INTEGER_MAP.withExistingValue(existing);
		Map<String, Integer> result = this.binder.bind("foo", target).get();
		assertThat(result).isExactlyInstanceOf(HashMap.class);
		assertThat(result).hasSize(2);
		assertThat(result).containsEntry("bar", 1);
		assertThat(result).containsEntry("baz", 1001);
	}

	@Test
	void bindToMapShouldRespectMapType() {
		this.sources.add(new MockConfigurationPropertySource("foo.bar", "1"));
		ResolvableType type = ResolvableType.forClassWithGenerics(HashMap.class, String.class, Integer.class);
		Object defaultMap = this.binder.bind("foo", STRING_INTEGER_MAP).get();
		Object customMap = this.binder.bind("foo", Bindable.of(type)).get();
		assertThat(customMap).isExactlyInstanceOf(HashMap.class).isNotInstanceOf(defaultMap.getClass());
	}

	@Test
	void bindToMapWhenNoValueShouldReturnUnbound() {
		this.sources.add(new MockConfigurationPropertySource("faf.bar", "1"));
		BindResult<Map<String, Integer>> result = this.binder.bind("foo", STRING_INTEGER_MAP);
		assertThat(result.isBound()).isFalse();
	}

	@Test
	void bindToMapShouldConvertKey() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo[0]", "1");
		source.put("foo[1]", "2");
		source.put("foo[9]", "3");
		this.sources.add(source);
		Map<Integer, Integer> result = this.binder.bind("foo", INTEGER_INTEGER_MAP).get();
		assertThat(result).hasSize(3);
		assertThat(result).containsEntry(0, 1);
		assertThat(result).containsEntry(1, 2);
		assertThat(result).containsEntry(9, 3);
	}

	@Test
	void bindToMapShouldBeGreedyForStrings() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.aaa.bbb.ccc", "b");
		source.put("foo.bbb.ccc.ddd", "a");
		source.put("foo.ccc.ddd.eee", "r");
		this.sources.add(source);
		Map<String, String> result = this.binder.bind("foo", STRING_STRING_MAP).get();
		assertThat(result).hasSize(3);
		assertThat(result).containsEntry("aaa.bbb.ccc", "b");
		assertThat(result).containsEntry("bbb.ccc.ddd", "a");
		assertThat(result).containsEntry("ccc.ddd.eee", "r");
	}

	@Test
	void bindToMapShouldBeGreedyForScalars() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.aaa.bbb.ccc", "foo-bar");
		source.put("foo.bbb.ccc.ddd", "BAR_BAZ");
		source.put("foo.ccc.ddd.eee", "bazboo");
		this.sources.add(source);
		Map<String, ExampleEnum> result = this.binder.bind("foo", Bindable.mapOf(String.class, ExampleEnum.class))
			.get();
		assertThat(result).hasSize(3);
		assertThat(result).containsEntry("aaa.bbb.ccc", ExampleEnum.FOO_BAR);
		assertThat(result).containsEntry("bbb.ccc.ddd", ExampleEnum.BAR_BAZ);
		assertThat(result).containsEntry("ccc.ddd.eee", ExampleEnum.BAZ_BOO);
	}

	@Test
	void bindToMapWithPlaceholdersShouldBeGreedyForScalars() {
		StandardEnvironment environment = new StandardEnvironment();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(environment, "foo=boo");
		MockConfigurationPropertySource source = new MockConfigurationPropertySource("foo.aaa.bbb.ccc", "baz-${foo}");
		this.sources.add(source);
		Binder binder = new Binder(this.sources, new PropertySourcesPlaceholdersResolver(environment));
		Map<String, ExampleEnum> result = binder.bind("foo", Bindable.mapOf(String.class, ExampleEnum.class)).get();
		assertThat(result).containsEntry("aaa.bbb.ccc", ExampleEnum.BAZ_BOO);
	}

	@Test
	void bindToMapWithNoPropertiesShouldReturnUnbound() {
		BindResult<Map<String, ExampleEnum>> result = this.binder.bind("foo",
				Bindable.mapOf(String.class, ExampleEnum.class));
		assertThat(result.isBound()).isFalse();
	}

	@Test
	void bindToMapShouldTriggerOnSuccess() {
		this.sources.add(new MockConfigurationPropertySource("foo.bar", "1", "line1"));
		BindHandler handler = mockBindHandler();
		Bindable<Map<String, Integer>> target = STRING_INTEGER_MAP;
		this.binder.bind("foo", target, handler);
		InOrder ordered = inOrder(handler);
		ordered.verify(handler)
			.onSuccess(eq(ConfigurationPropertyName.of("foo.bar")), eq(Bindable.of(Integer.class)), any(), eq(1));
		ordered.verify(handler).onSuccess(eq(ConfigurationPropertyName.of("foo")), eq(target), any(), isA(Map.class));
	}

	@Test
	void bindToMapStringArrayShouldTriggerOnSuccess() {
		this.sources.add(new MockConfigurationPropertySource("foo.bar", "a,b,c", "line1"));
		BindHandler handler = mockBindHandler();
		Bindable<Map<String, String[]>> target = STRING_ARRAY_MAP;
		this.binder.bind("foo", target, handler);
		InOrder ordered = inOrder(handler);
		ordered.verify(handler)
			.onSuccess(eq(ConfigurationPropertyName.of("foo.bar")), eq(Bindable.of(String[].class)), any(),
					assertArg((array) -> assertThat((String[]) array).containsExactly("a", "b", "c")));
		ordered.verify(handler).onSuccess(eq(ConfigurationPropertyName.of("foo")), eq(target), any(), isA(Map.class));
	}

	@Test
	void bindToMapNonScalarCollectionShouldPopulateMap() {
		Bindable<List<JavaBean>> valueType = Bindable.listOf(JavaBean.class);
		Bindable<Map<String, List<JavaBean>>> target = getMapBindable(String.class, valueType.getType());
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.bar[0].value", "a");
		source.put("foo.bar[1].value", "b");
		source.put("foo.bar[2].value", "c");
		this.sources.add(source);
		Map<String, List<JavaBean>> map = this.binder.bind("foo", target).get();
		List<JavaBean> bar = map.get("bar");
		assertThat(bar).isNotNull();
		List<String> values = bar.stream().map(JavaBean::getValue).toList();
		assertThat(values).containsExactly("a", "b", "c");

	}

	@Test
	void bindToPropertiesShouldBeEquivalentToMapOfStringString() {
		this.sources.add(new MockConfigurationPropertySource("foo.bar.baz", "1", "line1"));
		Bindable<Properties> target = Bindable.of(Properties.class);
		Properties properties = this.binder.bind("foo", target).get();
		assertThat(properties.getProperty("bar.baz")).isEqualTo("1");
	}

	@Test
	void bindToMapShouldNotTreatClassWithStringConstructorAsScalar() {
		this.sources.add(new MockConfigurationPropertySource("foo.bar.pattern", "1", "line1"));
		Bindable<Map<String, Foo>> target = Bindable.mapOf(String.class, Foo.class);
		Map<String, Foo> map = this.binder.bind("foo", target).get();
		Foo bar = map.get("bar");
		assertThat(bar).isNotNull();
		assertThat(bar.getPattern()).isEqualTo("1");
	}

	@Test
	void bindToMapStringArrayWithDotKeysShouldPreserveDot() {
		MockConfigurationPropertySource mockSource = new MockConfigurationPropertySource();
		mockSource.put("foo.bar.baz[0]", "a");
		mockSource.put("foo.bar.baz[1]", "b");
		mockSource.put("foo.bar.baz[2]", "c");
		this.sources.add(mockSource);
		Map<String, String[]> map = this.binder.bind("foo", STRING_ARRAY_MAP).get();
		assertThat(map.get("bar.baz")).containsExactly("a", "b", "c");
	}

	@Test
	void bindToMapStringArrayWithDotKeysAndCommaSeparatedShouldPreserveDot() {
		MockConfigurationPropertySource mockSource = new MockConfigurationPropertySource();
		mockSource.put("foo.bar.baz", "a,b,c");
		this.sources.add(mockSource);
		Map<String, String[]> map = this.binder.bind("foo", STRING_ARRAY_MAP).get();
		assertThat(map.get("bar.baz")).containsExactly("a", "b", "c");
	}

	@Test
	void bindToMapStringCollectionWithDotKeysShouldPreserveDot() {
		Bindable<List<String>> valueType = Bindable.listOf(String.class);
		Bindable<Map<String, List<String>>> target = getMapBindable(String.class, valueType.getType());
		MockConfigurationPropertySource mockSource = new MockConfigurationPropertySource();
		mockSource.put("foo.bar.baz[0]", "a");
		mockSource.put("foo.bar.baz[1]", "b");
		mockSource.put("foo.bar.baz[2]", "c");
		this.sources.add(mockSource);
		Map<String, List<String>> map = this.binder.bind("foo", target).get();
		List<String> values = map.get("bar.baz");
		assertThat(values).containsExactly("a", "b", "c");
	}

	@Test
	void bindToMapNonScalarCollectionWithDotKeysShouldBind() {
		Bindable<List<JavaBean>> valueType = Bindable.listOf(JavaBean.class);
		Bindable<Map<String, List<JavaBean>>> target = getMapBindable(String.class, valueType.getType());
		MockConfigurationPropertySource mockSource = new MockConfigurationPropertySource();
		mockSource.put("foo.bar.baz[0].value", "a");
		mockSource.put("foo.bar.baz[1].value", "b");
		mockSource.put("foo.bar.baz[2].value", "c");
		this.sources.add(mockSource);
		Map<String, List<JavaBean>> map = this.binder.bind("foo", target).get();
		List<JavaBean> barBaz = map.get("bar.baz");
		assertThat(barBaz).isNotNull();
		List<String> values = barBaz.stream().map(JavaBean::getValue).toList();
		assertThat(values).containsExactly("a", "b", "c");
	}

	@Test
	void bindToListOfMaps() {
		Bindable<List<Integer>> listBindable = Bindable.listOf(Integer.class);
		Bindable<Map<String, List<Integer>>> mapBindable = getMapBindable(String.class, listBindable.getType());
		Bindable<List<Map<String, List<Integer>>>> target = getListBindable(mapBindable.getType());
		MockConfigurationPropertySource mockSource = new MockConfigurationPropertySource();
		mockSource.put("foo[0].a", "1,2,3");
		mockSource.put("foo[1].b", "4,5,6");
		this.sources.add(mockSource);
		List<Map<String, List<Integer>>> list = this.binder.bind("foo", target).get();
		assertThat(list.get(0).get("a")).containsExactly(1, 2, 3);
		assertThat(list.get(1).get("b")).containsExactly(4, 5, 6);
	}

	@Test
	void bindToMapWithNumberKeyAndCommaSeparated() {
		Bindable<List<String>> listBindable = Bindable.listOf(String.class);
		Bindable<Map<Integer, List<String>>> target = getMapBindable(Integer.class, listBindable.getType());
		MockConfigurationPropertySource mockSource = new MockConfigurationPropertySource();
		mockSource.put("foo[0]", "a,b,c");
		mockSource.put("foo[1]", "e,f,g");
		this.sources.add(mockSource);
		Map<Integer, List<String>> map = this.binder.bind("foo", target).get();
		assertThat(map.get(0)).containsExactly("a", "b", "c");
		assertThat(map.get(1)).containsExactly("e", "f", "g");
	}

	@Test
	void bindToMapWithNumberKeyAndIndexed() {
		Bindable<List<Integer>> listBindable = Bindable.listOf(Integer.class);
		Bindable<Map<Integer, List<Integer>>> target = getMapBindable(Integer.class, listBindable.getType());
		MockConfigurationPropertySource mockSource = new MockConfigurationPropertySource();
		mockSource.put("foo[0][0]", "8");
		mockSource.put("foo[0][1]", "9");
		this.sources.add(mockSource);
		Map<Integer, List<Integer>> map = this.binder.bind("foo", target).get();
		assertThat(map.get(0)).containsExactly(8, 9);
	}

	@Test
	void bindingWithSquareBracketMap() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.[x [B] y]", "[ball]");
		this.sources.add(source);
		Map<String, String> map = this.binder.bind("foo", STRING_STRING_MAP).get();
		assertThat(map).containsEntry("x [B] y", "[ball]");
	}

	@Test
	void nestedMapsShouldNotBindToNull() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.value", "one");
		source.put("foo.foos.foo1.value", "two");
		source.put("foo.foos.foo2.value", "three");
		this.sources.add(source);
		BindResult<NestableFoo> foo = this.binder.bind("foo", NestableFoo.class);
		assertThat(foo.get().getValue()).isNotNull();
		NestableFoo foo1 = foo.get().getFoos().get("foo1");
		assertThat(foo1).isNotNull();
		assertThat(foo1.getValue()).isEqualTo("two");
		NestableFoo foo2 = foo.get().getFoos().get("foo2");
		assertThat(foo2).isNotNull();
		assertThat(foo2.getValue()).isEqualTo("three");
	}

	@Test
	void bindToMapWithCustomConverter() {
		DefaultConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(new MapConverter());
		Binder binder = new Binder(this.sources, null, conversionService, null);
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo", "a,b");
		this.sources.add(source);
		Map<String, String> map = binder.bind("foo", STRING_STRING_MAP).get();
		assertThat(map).containsKey("a");
		assertThat(map).containsKey("b");
	}

	@Test
	void bindToMapWithCustomConverterAndChildElements() {
		// gh-11892
		DefaultConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(new MapConverter());
		Binder binder = new Binder(this.sources, null, conversionService, null);
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo", "boom");
		source.put("foo.a", "a");
		source.put("foo.b", "b");
		this.sources.add(source);
		Map<String, String> map = binder.bind("foo", STRING_STRING_MAP).get();
		assertThat(map).containsEntry("a", "a");
		assertThat(map).containsEntry("b", "b");
	}

	@Test
	void bindToMapWithNoConverterForValue() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo", "a,b");
		this.sources.add(source);
		assertThatExceptionOfType(BindException.class).isThrownBy(() -> this.binder.bind("foo", STRING_STRING_MAP));
	}

	@Test
	@SuppressWarnings("rawtypes")
	void bindToMapWithPropertyEditorForKey() {
		// gh-12166
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.[java.lang.RuntimeException]", "bar");
		this.sources.add(source);
		Map<Class, String> map = this.binder.bind("foo", Bindable.mapOf(Class.class, String.class)).get();
		assertThat(map).containsExactly(entry(RuntimeException.class, "bar"));
	}

	@Test
	@SuppressWarnings("rawtypes")
	void bindToMapWithPropertyEditorForValue() {
		// gh-12166
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.bar", "java.lang.RuntimeException");
		this.sources.add(source);
		Map<String, Class> map = this.binder.bind("foo", Bindable.mapOf(String.class, Class.class)).get();
		assertThat(map).containsExactly(entry("bar", RuntimeException.class));
	}

	@Test
	void bindToMapWithNoDefaultConstructor() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.items.a", "b");
		this.sources.add(source);
		ExampleCustomNoDefaultConstructorBean result = this.binder
			.bind("foo", ExampleCustomNoDefaultConstructorBean.class)
			.get();
		assertThat(result.getItems()).containsOnly(entry("foo", "bar"), entry("a", "b"));
	}

	@Test
	void bindToMapWithDefaultConstructor() {
		// gh-12322
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.items.a", "b");
		this.sources.add(source);
		ExampleCustomWithDefaultConstructorBean result = this.binder
			.bind("foo", ExampleCustomWithDefaultConstructorBean.class)
			.get();
		assertThat(result.getItems()).containsExactly(entry("a", "b"));
	}

	@Test
	void bindToImmutableMapShouldReturnPopulatedMap() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.values.c", "d");
		source.put("foo.values.e", "f");
		this.sources.add(source);
		Map<String, String> result = this.binder
			.bind("foo.values", STRING_STRING_MAP.withExistingValue(Collections.singletonMap("a", "b")))
			.get();
		assertThat(result).hasSize(3);
		assertThat(result).containsExactly(entry("a", "b"), entry("c", "d"), entry("e", "f"));
	}

	@Test
	void bindToBeanWithExceptionInGetterForExistingValue() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.values.a", "b");
		this.sources.add(source);
		BeanWithGetterException result = this.binder.bind("foo", Bindable.of(BeanWithGetterException.class)).get();
		assertThat(result.getValues()).containsExactly(entry("a", "b"));
	}

	@Test
	void bindToMapWithWildcardShouldConvertToTheRightType() {
		// gh-18767
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.addresses.localhost[0]", "127.0.0.1");
		source.put("foo.addresses.localhost[1]", "127.0.0.2");
		this.sources.add(source);
		MapWithWildcardProperties result = this.binder.bind("foo", Bindable.of(MapWithWildcardProperties.class)).get();
		Map<String, ? extends List<? extends InetAddress>> addresses = result.getAddresses();
		assertThat(addresses).isNotNull();
		List<? extends InetAddress> localhost = addresses.get("localhost");
		assertThat(localhost).isNotNull();
		assertThat(localhost.stream().map(InetAddress::getHostAddress)).containsExactly("127.0.0.1", "127.0.0.2");
	}

	@Test
	void bindToMapWithPlaceholdersShouldResolve() {
		DefaultConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(new MapConverter());
		StandardEnvironment environment = new StandardEnvironment();
		Binder binder = new Binder(this.sources, new PropertySourcesPlaceholdersResolver(environment),
				conversionService, null, null);
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(environment, "bar=bc");
		this.sources.add(new MockConfigurationPropertySource("foo", "a${bar},${bar}d"));
		Map<String, String> map = binder.bind("foo", STRING_STRING_MAP).get();
		assertThat(map).containsKey("abc");
		assertThat(map).containsKey("bcd");
	}

	@Test
	void bindToCustomMapWithoutCtorAndConverterShouldResolve() {
		DefaultConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(new CustomMapConverter());
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.custom-map", "value");
		this.sources.add(source);
		Binder binder = new Binder(this.sources, null, conversionService, null);
		CustomMapWithoutDefaultCtor result = binder.bind("foo", Bindable.of(CustomMapWithoutDefaultCtor.class)).get();
		assertThat(result.getCustomMap().getSource()).isEqualTo("value");
	}

	@Test
	void bindToEnumMapShouldBind() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("props.foo-bar", "value");
		this.sources.add(source);
		Binder binder = new Binder(this.sources, null, null, null);
		EnumMap<ExampleEnum, String> result = binder.bind("props", EXAMPLE_ENUM_STRING_ENUM_MAP).get();
		assertThat(result).hasSize(1).containsEntry(ExampleEnum.FOO_BAR, "value");
	}

	private <K, V> Bindable<Map<K, V>> getMapBindable(Class<K> keyGeneric, ResolvableType valueType) {
		ResolvableType keyType = ResolvableType.forClass(keyGeneric);
		return Bindable.of(ResolvableType.forClassWithGenerics(Map.class, keyType, valueType));
	}

	private <T> Bindable<List<T>> getListBindable(ResolvableType type) {
		return Bindable.of(ResolvableType.forClassWithGenerics(List.class, type));
	}

	private BindHandler mockBindHandler() {
		BindHandler handler = mock(BindHandler.class);
		given(handler.onStart(any(), any(), any())).willAnswer(InvocationArgument.index(1));
		given(handler.onCreate(any(), any(), any(), any())).willAnswer(InvocationArgument.index(3));
		given(handler.onSuccess(any(), any(), any(), any())).willAnswer(InvocationArgument.index(3));
		return handler;
	}

	static class Foo {

		private @Nullable String pattern;

		Foo() {
		}

		Foo(@Nullable String pattern) {
			this.pattern = pattern;
		}

		@Nullable String getPattern() {
			return this.pattern;
		}

		void setPattern(@Nullable String pattern) {
			this.pattern = pattern;
		}

	}

	static class NestableFoo {

		private final Map<String, NestableFoo> foos = new LinkedHashMap<>();

		private @Nullable String value;

		Map<String, NestableFoo> getFoos() {
			return this.foos;
		}

		@Nullable String getValue() {
			return this.value;
		}

		void setValue(@Nullable String value) {
			this.value = value;
		}

	}

	static class MapConverter implements Converter<String, Map<String, String>> {

		@Override
		public Map<String, String> convert(String s) {
			return StringUtils.commaDelimitedListToSet(s).stream().collect(Collectors.toMap((k) -> k, (k) -> ""));
		}

	}

	static class ExampleCustomNoDefaultConstructorBean {

		private MyCustomNoDefaultConstructorMap items = new MyCustomNoDefaultConstructorMap(
				Collections.singletonMap("foo", "bar"));

		MyCustomNoDefaultConstructorMap getItems() {
			return this.items;
		}

		void setItems(MyCustomNoDefaultConstructorMap items) {
			this.items = items;
		}

	}

	static class MyCustomNoDefaultConstructorMap extends HashMap<String, String> {

		MyCustomNoDefaultConstructorMap(Map<String, String> items) {
			putAll(items);
		}

	}

	static class ExampleCustomWithDefaultConstructorBean {

		private final MyCustomWithDefaultConstructorMap items = new MyCustomWithDefaultConstructorMap();

		MyCustomWithDefaultConstructorMap getItems() {
			return this.items;
		}

		void setItems(MyCustomWithDefaultConstructorMap items) {
			this.items.clear();
			this.items.putAll(items);
		}

	}

	static class MyCustomWithDefaultConstructorMap extends HashMap<String, String> {

	}

	static class BeanWithGetterException {

		private @Nullable Map<String, String> values;

		void setValues(@Nullable Map<String, String> values) {
			this.values = values;
		}

		@Nullable Map<String, String> getValues() {
			return Collections.unmodifiableMap(this.values);
		}

	}

	static class MapWithWildcardProperties {

		private @Nullable Map<String, ? extends List<? extends InetAddress>> addresses;

		@Nullable Map<String, ? extends List<? extends InetAddress>> getAddresses() {
			return this.addresses;
		}

		void setAddresses(@Nullable Map<String, ? extends List<? extends InetAddress>> addresses) {
			this.addresses = addresses;
		}

	}

	static class CustomMapWithoutDefaultCtor {

		private final CustomMap customMap;

		CustomMapWithoutDefaultCtor(CustomMap customMap) {
			this.customMap = customMap;
		}

		CustomMap getCustomMap() {
			return this.customMap;
		}

		static final class CustomMap extends AbstractMap<String, Object> {

			private final String source;

			CustomMap(String source) {
				this.source = source;
			}

			@Override
			public Set<Entry<String, Object>> entrySet() {
				return Collections.emptySet();
			}

			String getSource() {
				return this.source;
			}

		}

	}

	private static final class CustomMapConverter implements Converter<String, CustomMap> {

		@Override
		public CustomMap convert(String source) {
			return new CustomMap(source);
		}

	}

	private static final class InvocationArgument<T> implements Answer<T> {

		private final int index;

		private InvocationArgument(int index) {
			this.index = index;
		}

		@Override
		public T answer(InvocationOnMock invocation) throws Throwable {
			return invocation.getArgument(this.index);
		}

		private static <T> InvocationArgument<T> index(int index) {
			return new InvocationArgument<>(index);
		}

	}

}
