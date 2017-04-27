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

package org.springframework.boot.context.properties.bind.convert;

import org.junit.Test;

import org.springframework.core.convert.converter.Converter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StringToEnumConverterFactory}.
 *
 * @author Phillip Webb
 */
public class StringToEnumConverterFactoryTests {

	private StringToEnumConverterFactory factory = new StringToEnumConverterFactory();

	@Test
	public void getConverterShouldReturnConverter() {
		Converter<String, TestEnum> converter = this.factory.getConverter(TestEnum.class);
		assertThat(converter).isNotNull();
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void getConverterWhenEnumSubclassShouldReturnConverter() throws Exception {
		Converter<String, TestEnum> converter = this.factory
				.getConverter((Class) TestSubclassEnum.ONE.getClass());
		assertThat(converter).isNotNull();
	}

	@Test
	public void convertWhenExactMatchShouldConvertValue() throws Exception {
		Converter<String, TestEnum> converter = this.factory.getConverter(TestEnum.class);
		assertThat(converter.convert("")).isNull();
		assertThat(converter.convert("ONE")).isEqualTo(TestEnum.ONE);
		assertThat(converter.convert("TWO")).isEqualTo(TestEnum.TWO);
		assertThat(converter.convert("THREE_AND_FOUR"))
				.isEqualTo(TestEnum.THREE_AND_FOUR);
	}

	@Test
	public void convertWhenFuzzyMatchShouldConvertValue() throws Exception {
		Converter<String, TestEnum> converter = this.factory.getConverter(TestEnum.class);
		assertThat(converter.convert("")).isNull();
		assertThat(converter.convert("one")).isEqualTo(TestEnum.ONE);
		assertThat(converter.convert("tWo")).isEqualTo(TestEnum.TWO);
		assertThat(converter.convert("three_and_four"))
				.isEqualTo(TestEnum.THREE_AND_FOUR);
		assertThat(converter.convert("threeandfour")).isEqualTo(TestEnum.THREE_AND_FOUR);
		assertThat(converter.convert("three-and-four"))
				.isEqualTo(TestEnum.THREE_AND_FOUR);
		assertThat(converter.convert("threeAndFour")).isEqualTo(TestEnum.THREE_AND_FOUR);
	}

	enum TestEnum {

		ONE, TWO, THREE_AND_FOUR

	}

	enum TestSubclassEnum {

		ONE {

			@Override
			public String toString() {
				return "foo";
			}

		}

	}

}
