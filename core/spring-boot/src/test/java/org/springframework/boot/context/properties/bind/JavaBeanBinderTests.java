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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.JavaBeanBinder.Bean;
import org.springframework.boot.context.properties.bind.JavaBeanBinder.BeanProperty;
import org.springframework.boot.context.properties.bind.handler.IgnoreErrorsBindHandler;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MockConfigurationPropertySource;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.convert.Delimiter;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.annotation.DateTimeFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link JavaBeanBinder}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Andy Wilkinson
 * @author Lasse Wulff
 */
class JavaBeanBinderTests {

	private final List<ConfigurationPropertySource> sources = new ArrayList<>();

	private final Binder binder = new Binder(this.sources);

	@Test
	void bindToClassShouldCreateBoundBean() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.int-value", "12");
		source.put("foo.long-value", "34");
		source.put("foo.string-value", "foo");
		source.put("foo.enum-value", "foo-bar");
		this.sources.add(source);
		ExampleValueBean bean = this.binder.bind("foo", Bindable.of(ExampleValueBean.class)).get();
		assertThat(bean.getIntValue()).isEqualTo(12);
		assertThat(bean.getLongValue()).isEqualTo(34);
		assertThat(bean.getStringValue()).isEqualTo("foo");
		assertThat(bean.getEnumValue()).isEqualTo(ExampleEnum.FOO_BAR);
	}

	@Test
	void bindRenamedPropertyToClassBean() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("renamed.public", "alpha");
		this.sources.add(source);
		ExampleRenamedPropertyBean bean = this.binder.bind("renamed", Bindable.of(ExampleRenamedPropertyBean.class))
			.get();
		assertThat(bean.getExampleProperty()).isEqualTo("alpha");
	}

	@Test
	void bindToClassWhenHasNoPrefixShouldCreateBoundBean() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("int-value", "12");
		source.put("long-value", "34");
		source.put("string-value", "foo");
		source.put("enum-value", "foo-bar");
		this.sources.add(source);
		ExampleValueBean bean = this.binder.bind(ConfigurationPropertyName.of(""), Bindable.of(ExampleValueBean.class))
			.get();
		assertThat(bean.getIntValue()).isEqualTo(12);
		assertThat(bean.getLongValue()).isEqualTo(34);
		assertThat(bean.getStringValue()).isEqualTo("foo");
		assertThat(bean.getEnumValue()).isEqualTo(ExampleEnum.FOO_BAR);
	}

	@Test
	void bindToInstanceShouldBindToInstance() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.int-value", "12");
		source.put("foo.long-value", "34");
		source.put("foo.string-value", "foo");
		source.put("foo.enum-value", "foo-bar");
		this.sources.add(source);
		ExampleValueBean bean = new ExampleValueBean();
		ExampleValueBean boundBean = this.binder
			.bind("foo", Bindable.of(ExampleValueBean.class).withExistingValue(bean))
			.get();
		assertThat(boundBean).isSameAs(bean);
		assertThat(bean.getIntValue()).isEqualTo(12);
		assertThat(bean.getLongValue()).isEqualTo(34);
		assertThat(bean.getStringValue()).isEqualTo("foo");
		assertThat(bean.getEnumValue()).isEqualTo(ExampleEnum.FOO_BAR);
	}

	@Test
	void bindToInstanceWithNoPropertiesShouldReturnUnbound() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		this.sources.add(source);
		ExampleDefaultsBean bean = new ExampleDefaultsBean();
		BindResult<ExampleDefaultsBean> boundBean = this.binder.bind("foo",
				Bindable.of(ExampleDefaultsBean.class).withExistingValue(bean));
		assertThat(boundBean.isBound()).isFalse();
		assertThat(bean.getFoo()).isEqualTo(123);
		assertThat(bean.getBar()).isEqualTo(456);
	}

	@Test
	void bindToClassShouldLeaveDefaults() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.bar", "999");
		this.sources.add(source);
		ExampleDefaultsBean bean = this.binder.bind("foo", Bindable.of(ExampleDefaultsBean.class)).get();
		assertThat(bean.getFoo()).isEqualTo(123);
		assertThat(bean.getBar()).isEqualTo(999);
	}

	@Test
	void bindToExistingInstanceShouldLeaveDefaults() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.bar", "999");
		this.sources.add(source);
		ExampleDefaultsBean bean = new ExampleDefaultsBean();
		bean.setFoo(888);
		ExampleDefaultsBean boundBean = this.binder
			.bind("foo", Bindable.of(ExampleDefaultsBean.class).withExistingValue(bean))
			.get();
		assertThat(boundBean).isSameAs(bean);
		assertThat(bean.getFoo()).isEqualTo(888);
		assertThat(bean.getBar()).isEqualTo(999);
	}

	@Test
	void bindToClassShouldBindToMap() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.map.foo-bar", "1");
		source.put("foo.map.bar-baz", "2");
		this.sources.add(source);
		ExampleMapBean bean = this.binder.bind("foo", Bindable.of(ExampleMapBean.class)).get();
		assertThat(bean.getMap()).containsExactly(entry(ExampleEnum.FOO_BAR, 1), entry(ExampleEnum.BAR_BAZ, 2));
	}

	@Test
	void bindToClassShouldBindToList() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.list[0]", "foo-bar");
		source.put("foo.list[1]", "bar-baz");
		this.sources.add(source);
		ExampleListBean bean = this.binder.bind("foo", Bindable.of(ExampleListBean.class)).get();
		assertThat(bean.getList()).containsExactly(ExampleEnum.FOO_BAR, ExampleEnum.BAR_BAZ);
	}

	@Test
	void bindToListIfUnboundElementsPresentShouldThrowException() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.list[0]", "foo-bar");
		source.put("foo.list[2]", "bar-baz");
		this.sources.add(source);
		assertThatExceptionOfType(BindException.class)
			.isThrownBy(() -> this.binder.bind("foo", Bindable.of(ExampleListBean.class)))
			.withCauseInstanceOf(UnboundConfigurationPropertiesException.class);
	}

	@Test
	void bindToClassShouldBindToSet() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.set[0]", "foo-bar");
		source.put("foo.set[1]", "bar-baz");
		this.sources.add(source);
		ExampleSetBean bean = this.binder.bind("foo", Bindable.of(ExampleSetBean.class)).get();
		assertThat(bean.getSet()).containsExactly(ExampleEnum.FOO_BAR, ExampleEnum.BAR_BAZ);
	}

	@Test
	void bindToClassShouldBindToCollection() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.collection[0]", "foo-bar");
		source.put("foo.collection[1]", "bar-baz");
		this.sources.add(source);
		ExampleCollectionBean bean = this.binder.bind("foo", Bindable.of(ExampleCollectionBean.class)).get();
		assertThat(bean.getCollection()).containsExactly(ExampleEnum.FOO_BAR, ExampleEnum.BAR_BAZ);
	}

	@Test
	void bindToClassShouldBindToCollectionWithDelimiter() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.collection", "foo-bar|bar-baz");
		this.sources.add(source);
		ExampleCollectionBeanWithDelimiter bean = this.binder
			.bind("foo", Bindable.of(ExampleCollectionBeanWithDelimiter.class))
			.get();
		assertThat(bean.getCollection()).containsExactly(ExampleEnum.FOO_BAR, ExampleEnum.BAR_BAZ);
	}

	@Test
	void bindToClassWhenHasNoSetterShouldBindToMap() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.map.foo-bar", "1");
		source.put("foo.map.bar-baz", "2");
		this.sources.add(source);
		ExampleMapBeanWithoutSetter bean = this.binder.bind("foo", Bindable.of(ExampleMapBeanWithoutSetter.class))
			.get();
		assertThat(bean.getMap()).containsExactly(entry(ExampleEnum.FOO_BAR, 1), entry(ExampleEnum.BAR_BAZ, 2));
	}

	@Test
	void bindToClassWhenHasNoSetterShouldBindToList() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.list[0]", "foo-bar");
		source.put("foo.list[1]", "bar-baz");
		this.sources.add(source);
		ExampleListBeanWithoutSetter bean = this.binder.bind("foo", Bindable.of(ExampleListBeanWithoutSetter.class))
			.get();
		assertThat(bean.getList()).containsExactly(ExampleEnum.FOO_BAR, ExampleEnum.BAR_BAZ);
	}

	@Test
	void bindToClassWhenHasNoSetterShouldBindToSet() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.set[0]", "foo-bar");
		source.put("foo.set[1]", "bar-baz");
		this.sources.add(source);
		ExampleSetBeanWithoutSetter bean = this.binder.bind("foo", Bindable.of(ExampleSetBeanWithoutSetter.class))
			.get();
		assertThat(bean.getSet()).containsExactly(ExampleEnum.FOO_BAR, ExampleEnum.BAR_BAZ);
	}

	@Test
	void bindToClassWhenHasNoSetterShouldBindToCollection() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.collection[0]", "foo-bar");
		source.put("foo.collection[1]", "bar-baz");
		this.sources.add(source);
		ExampleCollectionBeanWithoutSetter bean = this.binder
			.bind("foo", Bindable.of(ExampleCollectionBeanWithoutSetter.class))
			.get();
		assertThat(bean.getCollection()).containsExactly(ExampleEnum.FOO_BAR, ExampleEnum.BAR_BAZ);
	}

	@Test
	void bindToClassShouldBindNested() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.value-bean.int-value", "123");
		source.put("foo.value-bean.string-value", "foo");
		this.sources.add(source);
		ExampleNestedBean bean = this.binder.bind("foo", Bindable.of(ExampleNestedBean.class)).get();
		ExampleValueBean valueBean = bean.getValueBean();
		assertThat(valueBean).isNotNull();
		assertThat(valueBean.getIntValue()).isEqualTo(123);
		assertThat(valueBean.getStringValue()).isEqualTo("foo");
	}

	@Test
	void bindToClassWhenIterableShouldBindNestedBasedOnInstance() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.value-bean.int-value", "123");
		source.put("foo.value-bean.string-value", "foo");
		this.sources.add(source);
		ExampleNestedBeanWithoutSetterOrType bean = this.binder
			.bind("foo", Bindable.of(ExampleNestedBeanWithoutSetterOrType.class))
			.get();
		ExampleValueBean valueBean = (ExampleValueBean) bean.getValueBean();
		assertThat(valueBean.getIntValue()).isEqualTo(123);
		assertThat(valueBean.getStringValue()).isEqualTo("foo");
	}

	@Test
	void bindToClassWhenNotIterableShouldNotBindNestedBasedOnInstance() {
		// If we can't tell that binding will happen, we don't want to randomly invoke
		// getters on the class and cause side effects
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.value-bean.int-value", "123");
		source.put("foo.value-bean.string-value", "foo");
		this.sources.add(source.nonIterable());
		BindResult<ExampleNestedBeanWithoutSetterOrType> bean = this.binder.bind("foo",
				Bindable.of(ExampleNestedBeanWithoutSetterOrType.class));
		assertThat(bean.isBound()).isFalse();
	}

	@Test
	void bindToClassWhenHasNoSetterShouldBindNested() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.value-bean.int-value", "123");
		source.put("foo.value-bean.string-value", "foo");
		this.sources.add(source);
		ExampleNestedBeanWithoutSetter bean = this.binder.bind("foo", Bindable.of(ExampleNestedBeanWithoutSetter.class))
			.get();
		assertThat(bean.getValueBean().getIntValue()).isEqualTo(123);
		assertThat(bean.getValueBean().getStringValue()).isEqualTo("foo");
	}

	@Test
	void bindToClassWhenHasNoSetterAndImmutableShouldThrowException() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.nested.foo", "bar");
		this.sources.add(source);
		assertThatExceptionOfType(BindException.class)
			.isThrownBy(() -> this.binder.bind("foo", Bindable.of(ExampleImmutableNestedBeanWithoutSetter.class)));
	}

	@Test
	void bindToInstanceWhenNoNestedShouldLeaveNestedAsNull() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("faf.value-bean.int-value", "123");
		this.sources.add(source);
		ExampleNestedBean bean = new ExampleNestedBean();
		BindResult<ExampleNestedBean> boundBean = this.binder.bind("foo",
				Bindable.of(ExampleNestedBean.class).withExistingValue(bean));
		assertThat(boundBean.isBound()).isFalse();
		assertThat(bean.getValueBean()).isNull();
	}

	@Test
	void bindToClassWithOverriddenPropertyShouldSetSubclassProperty() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.value-bean.int-value", "123");
		source.put("foo.value-bean.sub-int-value", "456");
		this.sources.add(source);
		ExampleNestedSubclassBean bean = this.binder.bind("foo", Bindable.of(ExampleNestedSubclassBean.class)).get();
		assertThat(bean.getValueBean()).isNotNull();
		assertThat(bean.getValueBean().getIntValue()).isEqualTo(123);
		assertThat(bean.getValueBean().getSubIntValue()).isEqualTo(456);
	}

	@Test
	void bindToClassWhenPropertiesMissingShouldReturnUnbound() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("faf.int-value", "12");
		this.sources.add(source);
		BindResult<ExampleValueBean> bean = this.binder.bind("foo", Bindable.of(ExampleValueBean.class));
		assertThat(bean.isBound()).isFalse();
	}

	@Test
	void bindToClassWhenNoDefaultConstructorShouldBind() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.value", "bar");
		this.sources.add(source);
		BindResult<ExampleWithNonDefaultConstructor> bean = this.binder.bind("foo",
				Bindable.of(ExampleWithNonDefaultConstructor.class));
		assertThat(bean.isBound()).isTrue();
		ExampleWithNonDefaultConstructor boundBean = bean.get();
		assertThat(boundBean.getValue()).isEqualTo("bar");
	}

	@Test
	void bindToInstanceWhenNoDefaultConstructorShouldBind() {
		Binder binder = new Binder(this.sources, null, (ConversionService) null, null, null,
				(bindable, isNestedConstructorBinding) -> null);
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.value", "bar");
		this.sources.add(source);
		ExampleWithNonDefaultConstructor bean = new ExampleWithNonDefaultConstructor("faf");
		ExampleWithNonDefaultConstructor boundBean = binder
			.bind("foo", Bindable.of(ExampleWithNonDefaultConstructor.class).withExistingValue(bean))
			.get();
		assertThat(boundBean).isSameAs(bean);
		assertThat(bean.getValue()).isEqualTo("bar");
	}

	@Test
	void bindToClassShouldBindHierarchy() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.int-value", "123");
		source.put("foo.long-value", "456");
		this.sources.add(source);
		ExampleSubclassBean bean = this.binder.bind("foo", Bindable.of(ExampleSubclassBean.class)).get();
		assertThat(bean.getIntValue()).isEqualTo(123);
		assertThat(bean.getLongValue()).isEqualTo(456);
	}

	@Test
	void bindToClassWhenPropertyCannotBeConvertedShouldThrowException() {
		this.sources.add(new MockConfigurationPropertySource("foo.int-value", "foo"));
		assertThatExceptionOfType(BindException.class)
			.isThrownBy(() -> this.binder.bind("foo", Bindable.of(ExampleValueBean.class)));
	}

	@Test
	void bindToClassWhenPropertyCannotBeConvertedAndIgnoreErrorsShouldNotSetValue() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.int-value", "12");
		source.put("foo.long-value", "bang");
		source.put("foo.string-value", "foo");
		source.put("foo.enum-value", "foo-bar");
		this.sources.add(source);
		IgnoreErrorsBindHandler handler = new IgnoreErrorsBindHandler();
		ExampleValueBean bean = this.binder.bind("foo", Bindable.of(ExampleValueBean.class), handler).get();
		assertThat(bean.getIntValue()).isEqualTo(12);
		assertThat(bean.getLongValue()).isZero();
		assertThat(bean.getStringValue()).isEqualTo("foo");
		assertThat(bean.getEnumValue()).isEqualTo(ExampleEnum.FOO_BAR);
	}

	@Test
	void bindToClassWhenMismatchedGetSetShouldBind() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.value", "123");
		this.sources.add(source);
		ExampleMismatchBean bean = this.binder.bind("foo", Bindable.of(ExampleMismatchBean.class)).get();
		assertThat(bean.getValue()).isEqualTo("123");
	}

	@Test
	void bindToClassShouldNotInvokeExtraMethods() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource("foo.value", "123");
		this.sources.add(source.nonIterable());
		ExampleWithThrowingGetters bean = this.binder.bind("foo", Bindable.of(ExampleWithThrowingGetters.class)).get();
		assertThat(bean.getValue()).isEqualTo(123);
	}

	@Test
	void bindToClassWithSelfReferenceShouldBind() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.value", "123");
		this.sources.add(source);
		ExampleWithSelfReference bean = this.binder.bind("foo", Bindable.of(ExampleWithSelfReference.class)).get();
		assertThat(bean.getValue()).isEqualTo(123);
	}

	@Test
	void bindToInstanceWithExistingValueShouldReturnUnbound() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		this.sources.add(source);
		ExampleNestedBean existingValue = new ExampleNestedBean();
		ExampleValueBean valueBean = new ExampleValueBean();
		existingValue.setValueBean(valueBean);
		BindResult<ExampleNestedBean> result = this.binder.bind("foo",
				Bindable.of(ExampleNestedBean.class).withExistingValue(existingValue));
		assertThat(result.isBound()).isFalse();
	}

	@Test
	void bindWithAnnotations() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.date", "2014-04-01");
		this.sources.add(source);
		ConverterAnnotatedExampleBean bean = this.binder.bind("foo", Bindable.of(ConverterAnnotatedExampleBean.class))
			.get();
		assertThat(bean.getDate()).hasToString("2014-04-01");
	}

	@Test
	void bindWhenValueIsConvertedWithPropertyEditorShouldBind() {
		// gh-12166
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.value", "java.lang.RuntimeException");
		this.sources.add(source);
		ExampleWithPropertyEditorType bean = this.binder.bind("foo", Bindable.of(ExampleWithPropertyEditorType.class))
			.get();
		assertThat(bean.getValue()).isEqualTo(RuntimeException.class);
	}

	@Test
	void bindToClassShouldIgnoreInvalidAccessors() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.name", "something");
		this.sources.add(source);
		ExampleWithInvalidAccessors bean = this.binder.bind("foo", Bindable.of(ExampleWithInvalidAccessors.class))
			.get();
		assertThat(bean.getName()).isEqualTo("something");
	}

	@Test
	void bindToClassShouldIgnoreStaticAccessors() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.name", "invalid");
		source.put("foo.counter", "42");
		this.sources.add(source);
		ExampleWithStaticAccessors bean = this.binder.bind("foo", Bindable.of(ExampleWithStaticAccessors.class)).get();
		assertThat(ExampleWithStaticAccessors.name).isNull();
		assertThat(bean.getCounter()).isEqualTo(42);
	}

	@Test
	void bindToClassShouldCacheWithGenerics() {
		// gh-16821
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.integers[a].value", "1");
		source.put("foo.booleans[b].value", "true");
		this.sources.add(source);
		ExampleWithGenericMap bean = this.binder.bind("foo", Bindable.of(ExampleWithGenericMap.class)).get();
		GenericValue<Integer> a = bean.getIntegers().get("a");
		assertThat(a).isNotNull();
		assertThat(a.getValue()).isOne();
		GenericValue<Boolean> b = bean.getBooleans().get("b");
		assertThat(b).isNotNull();
		assertThat(b.getValue()).isTrue();
	}

	@Test
	void bindToClassWithOverloadedSetterShouldUseSetterThatMatchesField() {
		// gh-16206
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.property", "some string");
		this.sources.add(source);
		PropertyWithOverloadedSetter bean = this.binder.bind("foo", Bindable.of(PropertyWithOverloadedSetter.class))
			.get();
		assertThat(bean.getProperty()).isEqualTo("some string");
	}

	@Test
	void beanPropertiesPreferMatchingType() {
		// gh-16206
		ResolvableType type = ResolvableType.forClass(PropertyWithOverloadedSetter.class);
		Class<?> resolvedType = type.resolve();
		assertThat(resolvedType).isNotNull();
		Bean<PropertyWithOverloadedSetter> bean = new Bean<>(type, resolvedType) {

			@Override
			protected void addProperties(Method[] declaredMethods, Field[] declaredFields) {
				// We override here because we need a specific order of the declared
				// methods and the JVM doesn't give us one
				int intSetter = -1;
				int stringSetter = -1;
				for (int i = 0; i < declaredMethods.length; i++) {
					Method method = declaredMethods[i];
					if (method.getName().equals("setProperty")) {
						if (method.getParameters()[0].getType().equals(int.class)) {
							intSetter = i;
						}
						else {
							stringSetter = i;
						}
					}
				}
				if (intSetter > stringSetter) {
					Method method = declaredMethods[intSetter];
					declaredMethods[intSetter] = declaredMethods[stringSetter];
					declaredMethods[stringSetter] = method;
				}
				super.addProperties(declaredMethods, declaredFields);
			}

		};
		BeanProperty property = bean.getProperties().get("property");
		assertThat(property).isNotNull();
		PropertyWithOverloadedSetter target = new PropertyWithOverloadedSetter();
		property.setValue(() -> target, "some string");
	}

	@Test
	void bindOrCreateWithNestedShouldReturnCreatedValue() {
		NestedJavaBean result = this.binder.bindOrCreate("foo", Bindable.of(NestedJavaBean.class));
		assertThat(result.getNested().getBar()).isEqualTo(456);
	}

	@Test
	void bindWhenHasPackagePrivateSetterShouldBind() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.property", "test");
		this.sources.add(source);
		PackagePrivateSetterBean bean = this.binder.bind("foo", Bindable.of(PackagePrivateSetterBean.class)).get();
		assertThat(bean.getProperty()).isEqualTo("test");
	}

	@Test
	void bindUsesConsistentPropertyOrder() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.gamma", "0");
		source.put("foo.alpha", "0");
		source.put("foo.beta", "0");
		this.sources.add(source);
		PropertyOrderBean bean = this.binder.bind("foo", Bindable.of(PropertyOrderBean.class)).get();
		assertThat(bean.getAlpha()).isZero();
		assertThat(bean.getBeta()).isOne();
		assertThat(bean.getGamma()).isEqualTo(2);
	}

	@Test // gh-23007
	void bindWhenBeanWithGetSetIsMethodsFoundUsesGetterThatMatchesSetter() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("test.names", "spring,boot");
		this.sources.add(source);
		JavaBeanWithGetSetIs bean = this.binder.bind("test", Bindable.of(JavaBeanWithGetSetIs.class)).get();
		assertThat(bean.getNames()).containsExactly("spring", "boot");
	}

	@Test // gh-23007
	void bindWhenBeanWithGetIsMethodsFoundDoesNotUseIsGetter() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("test.names", "spring,boot");
		this.sources.add(source);
		JavaBeanWithGetIs bean = this.binder.bind("test", Bindable.of(JavaBeanWithGetIs.class)).get();
		assertThat(bean.getNames()).containsExactly("spring", "boot");
	}

	@Test // gh-33105
	void bindWhenHasBridgeMethods() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("test.value", "spring-boot");
		this.sources.add(source);
		ApplicationConversionService conversionService = new ApplicationConversionService();
		conversionService.addConverter(String.class, BridgeType.class, BridgeType::new);
		Binder binder = new Binder(this.sources, null, conversionService);
		BridgeMethods bean = binder.bind("test", Bindable.of(BridgeMethods.class)).get();
		assertThat(bean.getValue()).hasToString("spring-boot");
	}

	static class ExampleValueBean {

		private int intValue;

		private long longValue;

		private @Nullable String stringValue;

		private @Nullable ExampleEnum enumValue;

		int getIntValue() {
			return this.intValue;
		}

		void setIntValue(int intValue) {
			this.intValue = intValue;
		}

		long getLongValue() {
			return this.longValue;
		}

		void setLongValue(long longValue) {
			this.longValue = longValue;
		}

		@Nullable String getStringValue() {
			return this.stringValue;
		}

		void setStringValue(@Nullable String stringValue) {
			this.stringValue = stringValue;
		}

		@Nullable ExampleEnum getEnumValue() {
			return this.enumValue;
		}

		void setEnumValue(@Nullable ExampleEnum enumValue) {
			this.enumValue = enumValue;
		}

	}

	static class ExampleRenamedPropertyBean {

		@Name("public")
		private @Nullable String exampleProperty;

		@Nullable String getExampleProperty() {
			return this.exampleProperty;
		}

		void setExampleProperty(@Nullable String exampleProperty) {
			this.exampleProperty = exampleProperty;
		}

	}

	static class ExampleDefaultsBean {

		private int foo = 123;

		private int bar = 456;

		int getFoo() {
			return this.foo;
		}

		void setFoo(int foo) {
			this.foo = foo;
		}

		int getBar() {
			return this.bar;
		}

		void setBar(int bar) {
			this.bar = bar;
		}

	}

	static class ExampleMapBean {

		private @Nullable Map<ExampleEnum, Integer> map;

		@Nullable Map<ExampleEnum, Integer> getMap() {
			return this.map;
		}

		void setMap(@Nullable Map<ExampleEnum, Integer> map) {
			this.map = map;
		}

	}

	static class ExampleListBean {

		private @Nullable List<ExampleEnum> list;

		@Nullable List<ExampleEnum> getList() {
			return this.list;
		}

		void setList(@Nullable List<ExampleEnum> list) {
			this.list = list;
		}

	}

	static class ExampleSetBean {

		private @Nullable Set<ExampleEnum> set;

		@Nullable Set<ExampleEnum> getSet() {
			return this.set;
		}

		void setSet(@Nullable Set<ExampleEnum> set) {
			this.set = set;
		}

	}

	static class ExampleCollectionBean {

		private @Nullable Collection<ExampleEnum> collection;

		@Nullable Collection<ExampleEnum> getCollection() {
			return this.collection;
		}

		void setCollection(@Nullable Collection<ExampleEnum> collection) {
			this.collection = collection;
		}

	}

	static class ExampleMapBeanWithoutSetter {

		private final Map<ExampleEnum, Integer> map = new LinkedHashMap<>();

		Map<ExampleEnum, Integer> getMap() {
			return this.map;
		}

	}

	static class ExampleListBeanWithoutSetter {

		private final List<ExampleEnum> list = new ArrayList<>();

		List<ExampleEnum> getList() {
			return this.list;
		}

	}

	static class ExampleSetBeanWithoutSetter {

		private final Set<ExampleEnum> set = new LinkedHashSet<>();

		Set<ExampleEnum> getSet() {
			return this.set;
		}

	}

	static class ExampleCollectionBeanWithoutSetter {

		private final Collection<ExampleEnum> collection = new ArrayList<>();

		Collection<ExampleEnum> getCollection() {
			return this.collection;
		}

	}

	static class ExampleCollectionBeanWithDelimiter {

		@Delimiter("|")
		private @Nullable Collection<ExampleEnum> collection;

		@Nullable Collection<ExampleEnum> getCollection() {
			return this.collection;
		}

		void setCollection(@Nullable Collection<ExampleEnum> collection) {
			this.collection = collection;
		}

	}

	static class ExampleNestedBean {

		private @Nullable ExampleValueBean valueBean;

		@Nullable ExampleValueBean getValueBean() {
			return this.valueBean;
		}

		void setValueBean(@Nullable ExampleValueBean valueBean) {
			this.valueBean = valueBean;
		}

	}

	static class ExampleNestedBeanWithoutSetter {

		private final ExampleValueBean valueBean = new ExampleValueBean();

		ExampleValueBean getValueBean() {
			return this.valueBean;
		}

	}

	static class ExampleNestedBeanWithoutSetterOrType {

		private final ExampleValueBean valueBean = new ExampleValueBean();

		Object getValueBean() {
			return this.valueBean;
		}

	}

	static class ExampleImmutableNestedBeanWithoutSetter {

		private final NestedImmutable nested = new NestedImmutable();

		NestedImmutable getNested() {
			return this.nested;
		}

		static class NestedImmutable {

			String getFoo() {
				return "foo";
			}

		}

	}

	static class ExampleNestedSubclassBean extends ExampleNestedBean {

		private @Nullable ExampleValueSubclassBean valueBean;

		@Override
		@Nullable ExampleValueSubclassBean getValueBean() {
			return this.valueBean;
		}

		void setValueBean(@Nullable ExampleValueSubclassBean valueBean) {
			this.valueBean = valueBean;
		}

		static class ExampleValueSubclassBean extends ExampleValueBean {

			private int subIntValue;

			int getSubIntValue() {
				return this.subIntValue;
			}

			void setSubIntValue(int intValue) {
				this.subIntValue = intValue;
			}

		}

	}

	static class ExampleWithNonDefaultConstructor {

		private @Nullable String value;

		ExampleWithNonDefaultConstructor(@Nullable String value) {
			this.value = value;
		}

		@Nullable String getValue() {
			return this.value;
		}

		void setValue(@Nullable String value) {
			this.value = value;
		}

	}

	static class ExampleSuperClassBean {

		private int intValue;

		int getIntValue() {
			return this.intValue;
		}

		void setIntValue(int intValue) {
			this.intValue = intValue;
		}

	}

	static class ExampleSubclassBean extends ExampleSuperClassBean {

		private long longValue;

		long getLongValue() {
			return this.longValue;
		}

		void setLongValue(long longValue) {
			this.longValue = longValue;
		}

	}

	static class ExampleMismatchBean {

		private int value;

		String getValue() {
			return String.valueOf(this.value);
		}

		void setValue(int value) {
			this.value = value;
		}

	}

	static class ExampleWithThrowingGetters {

		private int value;

		int getValue() {
			return this.value;
		}

		void setValue(int value) {
			this.value = value;
		}

		List<String> getNames() {
			throw new RuntimeException();
		}

		ExampleValueBean getNested() {
			throw new RuntimeException();
		}

	}

	static class ExampleWithSelfReference {

		private int value;

		private @Nullable ExampleWithSelfReference self;

		int getValue() {
			return this.value;
		}

		void setValue(int value) {
			this.value = value;
		}

		@Nullable ExampleWithSelfReference getSelf() {
			return this.self;
		}

		void setSelf(@Nullable ExampleWithSelfReference self) {
			this.self = self;
		}

	}

	static class ExampleWithInvalidAccessors {

		private @Nullable String name;

		@Nullable String getName() {
			return this.name;
		}

		void setName(@Nullable String name) {
			this.name = name;
		}

		String get() {
			throw new IllegalArgumentException("should not be invoked");
		}

		boolean is() {
			throw new IllegalArgumentException("should not be invoked");
		}

	}

	static class ExampleWithStaticAccessors {

		private static @Nullable String name;

		private int counter;

		static @Nullable String getName() {
			return name;
		}

		static void setName(@Nullable String name) {
			ExampleWithStaticAccessors.name = name;
		}

		int getCounter() {
			return this.counter;
		}

		void setCounter(int counter) {
			this.counter = counter;
		}

	}

	public enum ExampleEnum {

		FOO_BAR,

		BAR_BAZ

	}

	static class ConverterAnnotatedExampleBean {

		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
		private @Nullable LocalDate date;

		@Nullable LocalDate getDate() {
			return this.date;
		}

		void setDate(@Nullable LocalDate date) {
			this.date = date;
		}

	}

	static class ExampleWithPropertyEditorType {

		private @Nullable Class<? extends Throwable> value;

		@Nullable Class<? extends Throwable> getValue() {
			return this.value;
		}

		void setValue(@Nullable Class<? extends Throwable> value) {
			this.value = value;
		}

	}

	static class ExampleWithGenericMap {

		private final Map<String, GenericValue<Integer>> integers = new LinkedHashMap<>();

		private final Map<String, GenericValue<Boolean>> booleans = new LinkedHashMap<>();

		Map<String, GenericValue<Integer>> getIntegers() {
			return this.integers;
		}

		Map<String, GenericValue<Boolean>> getBooleans() {
			return this.booleans;
		}

	}

	static class GenericValue<T> {

		private @Nullable T value;

		@Nullable T getValue() {
			return this.value;
		}

		void setValue(@Nullable T value) {
			this.value = value;
		}

	}

	static class PropertyWithOverloadedSetter {

		private @Nullable String property;

		void setProperty(int property) {
			this.property = String.valueOf(property);
		}

		void setProperty(@Nullable String property) {
			this.property = property;
		}

		@Nullable String getProperty() {
			return this.property;
		}

	}

	static class NestedJavaBean {

		private ExampleDefaultsBean nested = new ExampleDefaultsBean();

		ExampleDefaultsBean getNested() {
			return this.nested;
		}

		void setNested(ExampleDefaultsBean nested) {
			this.nested = nested;
		}

	}

	static class PackagePrivateSetterBean {

		private @Nullable String property;

		@Nullable String getProperty() {
			return this.property;
		}

		void setProperty(@Nullable String property) {
			this.property = property;
		}

	}

	static class JavaBeanWithGetSetIs {

		private List<String> names = new ArrayList<>();

		List<String> getNames() {
			return this.names;
		}

		void setNames(List<String> names) {
			this.names = names;
		}

		boolean isNames() {
			return !this.names.isEmpty();
		}

	}

	static class JavaBeanWithGetIs {

		private final List<String> names = new ArrayList<>();

		boolean isNames() {
			return !this.names.isEmpty();
		}

		List<String> getNames() {
			return this.names;
		}

	}

	static class PropertyOrderBean {

		static AtomicInteger atomic = new AtomicInteger();

		private int alpha;

		private int beta;

		private int gamma;

		int getAlpha() {
			return this.alpha;
		}

		void setAlpha(int alpha) {
			this.alpha = alpha + atomic.getAndIncrement();
		}

		int getBeta() {
			return this.beta;
		}

		void setBeta(int beta) {
			this.beta = beta + atomic.getAndIncrement();
		}

		int getGamma() {
			return this.gamma;
		}

		void setGamma(int gamma) {
			this.gamma = gamma + atomic.getAndIncrement();
		}

	}

	static class BridgeMethodsBase<T extends BridgeBaseType> {

		private @Nullable T value;

		@Nullable T getValue() {
			return this.value;
		}

		void setValue(@Nullable T value) {
			this.value = value;
		}

	}

	static class BridgeMethods extends BridgeMethodsBase<BridgeType> {

		@Override
		@Nullable BridgeType getValue() {
			return super.getValue();
		}

	}

	static class BridgeBaseType {

	}

	static class BridgeType extends BridgeBaseType {

		private final String value;

		BridgeType(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return this.value;
		}

	}

}
