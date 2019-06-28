/*
 * Copyright 2012-2019 the original author or authors.
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MockConfigurationPropertySource;
import org.springframework.format.annotation.DateTimeFormat;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ValueObjectBinder}.
 *
 * @author Madhura Bhave
 */
class ValueObjectBinderTests {

	private final List<ConfigurationPropertySource> sources = new ArrayList<>();

	private final Binder binder = new Binder(this.sources);

	@Test
	void bindToClassShouldCreateBoundBean() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.int-value", "12");
		source.put("foo.long-value", "34");
		source.put("foo.boolean-value", "true");
		source.put("foo.string-value", "foo");
		source.put("foo.enum-value", "foo-bar");
		this.sources.add(source);
		ExampleValueBean bean = this.binder.bind("foo", Bindable.of(ExampleValueBean.class)).get();
		assertThat(bean.getIntValue()).isEqualTo(12);
		assertThat(bean.getLongValue()).isEqualTo(34);
		assertThat(bean.isBooleanValue()).isTrue();
		assertThat(bean.getStringValue()).isEqualTo("foo");
		assertThat(bean.getEnumValue()).isEqualTo(ExampleEnum.FOO_BAR);
	}

	@Test
	void bindToClassWhenHasNoPrefixShouldCreateBoundBean() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("int-value", "12");
		source.put("long-value", "34");
		source.put("boolean-value", "true");
		source.put("string-value", "foo");
		source.put("enum-value", "foo-bar");
		this.sources.add(source);
		ExampleValueBean bean = this.binder.bind(ConfigurationPropertyName.of(""), Bindable.of(ExampleValueBean.class))
				.get();
		assertThat(bean.getIntValue()).isEqualTo(12);
		assertThat(bean.getLongValue()).isEqualTo(34);
		assertThat(bean.isBooleanValue()).isTrue();
		assertThat(bean.getStringValue()).isEqualTo("foo");
		assertThat(bean.getEnumValue()).isEqualTo(ExampleEnum.FOO_BAR);
	}

	@Test
	void bindToAbstractClassWithShouldNotBind() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.name", "test");
		this.sources.add(source);
		boolean bound = this.binder.bind("foo", Bindable.of(ExampleAbstractBean.class)).isBound();
		assertThat(bound).isFalse();
	}

	@Test
	void bindToClassWithMultipleConstructorsShouldNotBind() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.int-value", "12");
		this.sources.add(source);
		boolean bound = this.binder.bind("foo", Bindable.of(MultipleConstructorsBean.class)).isBound();
		assertThat(bound).isFalse();
	}

	@Test
	void bindToClassWithOnlyDefaultConstructorShouldNotBind() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.int-value", "12");
		this.sources.add(source);
		boolean bound = this.binder.bind("foo", Bindable.of(DefaultConstructorBean.class)).isBound();
		assertThat(bound).isFalse();
	}

	@Test
	void bindToClassShouldBindNested() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.value-bean.int-value", "123");
		source.put("foo.value-bean.long-value", "34");
		source.put("foo.value-bean.string-value", "foo");
		this.sources.add(source);
		ExampleNestedBean bean = this.binder.bind("foo", Bindable.of(ExampleNestedBean.class)).get();
		assertThat(bean.getValueBean().getIntValue()).isEqualTo(123);
		assertThat(bean.getValueBean().getLongValue()).isEqualTo(34);
		assertThat(bean.getValueBean().isBooleanValue()).isFalse();
		assertThat(bean.getValueBean().getStringValue()).isEqualTo("foo");
		assertThat(bean.getValueBean().getEnumValue()).isNull();
	}

	@Test
	void bindToClassWithNoValueForPrimitiveShouldUseDefault() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.string-value", "foo");
		this.sources.add(source);
		ExampleValueBean bean = this.binder.bind("foo", Bindable.of(ExampleValueBean.class)).get();
		assertThat(bean.getIntValue()).isEqualTo(0);
		assertThat(bean.getLongValue()).isEqualTo(0);
		assertThat(bean.isBooleanValue()).isEqualTo(false);
		assertThat(bean.getStringValue()).isEqualTo("foo");
	}

	@Test
	void bindToClassWithNoValueAndDefaultValueShouldNotBind() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.string-value", "foo");
		this.sources.add(source);
		assertThat(this.binder.bind("foo", Bindable.of(ExampleDefaultValueBean.class)).isBound()).isFalse();
	}

	@Test
	void bindToClassWhenNoParameterBoundShouldReturnNull() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		this.sources.add(source.nonIterable());
		BindResult<ExampleFailingConstructorBean> result = this.binder.bind("foo",
				Bindable.of(ExampleFailingConstructorBean.class));
		assertThat(result.isBound()).isFalse();
	}

	@Test
	void bindWithAnnotations() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.date", "2014-04-01");
		this.sources.add(source);
		ConverterAnnotatedExampleBean bean = this.binder.bind("foo", Bindable.of(ConverterAnnotatedExampleBean.class))
				.get();
		assertThat(bean.getDate().toString()).isEqualTo("2014-04-01");
	}

	@Test
	void bindWithAnnotationsAndDefaultValue() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.bar", "hello");
		this.sources.add(source);
		ConverterAnnotatedExampleBean bean = this.binder.bind("foo", Bindable.of(ConverterAnnotatedExampleBean.class))
				.get();
		assertThat(bean.getDate().toString()).isEqualTo("2019-05-10");
	}

	@Test
	void createShouldReturnCreatedValue() {
		ExampleValueBean value = this.binder.bindOrCreate("foo", Bindable.of(ExampleValueBean.class));
		assertThat(value.getIntValue()).isEqualTo(0);
		assertThat(value.getLongValue()).isEqualTo(0);
		assertThat(value.isBooleanValue()).isEqualTo(false);
		assertThat(value.getStringValue()).isNull();
		assertThat(value.getEnumValue()).isNull();
	}

	@Test
	void createWithNestedShouldReturnCreatedValue() {
		ExampleNestedBean value = this.binder.bindOrCreate("foo", Bindable.of(ExampleNestedBean.class));
		assertThat(value.getValueBean()).isEqualTo(null);
	}

	@Test
	void createWithDefaultValuesShouldReturnCreatedWithDefaultValues() {
		ExampleDefaultValueBean value = this.binder.bindOrCreate("foo", Bindable.of(ExampleDefaultValueBean.class));
		assertThat(value.getIntValue()).isEqualTo(5);
		assertThat(value.getStringsList()).containsOnly("a", "b", "c");
		assertThat(value.getCustomList()).containsOnly("x,y,z");
	}

	@Test
	void createWithDefaultValuesAndAnnotationsShouldReturnCreatedWithDefaultValues() {
		ConverterAnnotatedExampleBean bean = this.binder.bindOrCreate("foo",
				Bindable.of(ConverterAnnotatedExampleBean.class));
		assertThat(bean.getDate().toString()).isEqualTo("2019-05-10");
	}

	public static class ExampleValueBean {

		private final int intValue;

		private final long longValue;

		private final boolean booleanValue;

		private final String stringValue;

		private final ExampleEnum enumValue;

		ExampleValueBean(int intValue, long longValue, boolean booleanValue, String stringValue,
				ExampleEnum enumValue) {
			this.intValue = intValue;
			this.longValue = longValue;
			this.booleanValue = booleanValue;
			this.stringValue = stringValue;
			this.enumValue = enumValue;
		}

		public int getIntValue() {
			return this.intValue;
		}

		public long getLongValue() {
			return this.longValue;
		}

		public boolean isBooleanValue() {
			return this.booleanValue;
		}

		public String getStringValue() {
			return this.stringValue;
		}

		public ExampleEnum getEnumValue() {
			return this.enumValue;
		}

	}

	public enum ExampleEnum {

		FOO_BAR,

		BAR_BAZ

	}

	@SuppressWarnings("unused")
	public static class MultipleConstructorsBean {

		MultipleConstructorsBean(int intValue) {
			this(intValue, 23L, "hello");
		}

		MultipleConstructorsBean(int intValue, long longValue, String stringValue) {
		}

	}

	public abstract static class ExampleAbstractBean {

		private final String name;

		ExampleAbstractBean(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

	}

	public static class DefaultConstructorBean {

		DefaultConstructorBean() {
		}

	}

	public static class ExampleNestedBean {

		private final ExampleValueBean valueBean;

		ExampleNestedBean(ExampleValueBean valueBean) {
			this.valueBean = valueBean;
		}

		public ExampleValueBean getValueBean() {
			return this.valueBean;
		}

	}

	public static class ExampleDefaultValueBean {

		private final int intValue;

		private final List<String> stringsList;

		private final List<String> customList;

		ExampleDefaultValueBean(@DefaultValue("5") int intValue,
				@DefaultValue({ "a", "b", "c" }) List<String> stringsList,
				@DefaultValue("x,y,z") List<String> customList) {
			this.intValue = intValue;
			this.stringsList = stringsList;
			this.customList = customList;
		}

		public int getIntValue() {
			return this.intValue;
		}

		public List<String> getStringsList() {
			return this.stringsList;
		}

		public List<String> getCustomList() {
			return this.customList;
		}

	}

	public static class ExampleFailingConstructorBean {

		private final String name;

		private final Object value;

		ExampleFailingConstructorBean(String name, String value) {
			Objects.requireNonNull(name, "'name' must be not null.");
			Objects.requireNonNull(value, "'value' must be not null.");
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return this.name;
		}

		public Object getValue() {
			return this.value;
		}

	}

	public static class ConverterAnnotatedExampleBean {

		private final LocalDate date;

		private final String bar;

		ConverterAnnotatedExampleBean(
				@DefaultValue("2019-05-10") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, String bar) {
			this.date = date;
			this.bar = bar;
		}

		public LocalDate getDate() {
			return this.date;
		}

		public String getBar() {
			return this.bar;
		}

	}

}
