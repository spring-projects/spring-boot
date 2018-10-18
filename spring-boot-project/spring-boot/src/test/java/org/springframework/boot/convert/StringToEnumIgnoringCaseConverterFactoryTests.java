/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.convert;

import java.util.Locale;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.core.convert.ConversionService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StringToEnumIgnoringCaseConverterFactory}.
 *
 * @author Phillip Webb
 */
@RunWith(Parameterized.class)
public class StringToEnumIgnoringCaseConverterFactoryTests {

	private final ConversionService conversionService;

	public StringToEnumIgnoringCaseConverterFactoryTests(String name,
			ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Test
	public void canConvertFromStringToEnumShouldReturnTrue() {
		assertThat(this.conversionService.canConvert(String.class, TestEnum.class))
				.isTrue();
	}

	@Test
	public void canConvertFromStringToEnumSubclassShouldReturnTrue() {
		assertThat(this.conversionService.canConvert(String.class,
				TestSubclassEnum.ONE.getClass())).isTrue();
	}

	@Test
	public void convertFromStringToEnumWhenExactMatchShouldConvertValue() {
		ConversionService service = this.conversionService;
		assertThat(service.convert("", TestEnum.class)).isNull();
		assertThat(service.convert("ONE", TestEnum.class)).isEqualTo(TestEnum.ONE);
		assertThat(service.convert("TWO", TestEnum.class)).isEqualTo(TestEnum.TWO);
		assertThat(service.convert("THREE_AND_FOUR", TestEnum.class))
				.isEqualTo(TestEnum.THREE_AND_FOUR);
	}

	@Test
	public void convertFromStringToEnumWhenFuzzyMatchShouldConvertValue() {
		ConversionService service = this.conversionService;
		assertThat(service.convert("", TestEnum.class)).isNull();
		assertThat(service.convert("one", TestEnum.class)).isEqualTo(TestEnum.ONE);
		assertThat(service.convert("tWo", TestEnum.class)).isEqualTo(TestEnum.TWO);
		assertThat(service.convert("three_and_four", TestEnum.class))
				.isEqualTo(TestEnum.THREE_AND_FOUR);
		assertThat(service.convert("threeandfour", TestEnum.class))
				.isEqualTo(TestEnum.THREE_AND_FOUR);
		assertThat(service.convert("three-and-four", TestEnum.class))
				.isEqualTo(TestEnum.THREE_AND_FOUR);
		assertThat(service.convert("threeAndFour", TestEnum.class))
				.isEqualTo(TestEnum.THREE_AND_FOUR);
	}

	@Test
	public void convertFromStringToEnumWhenUsingNonEnglishLocaleShouldConvertValue() {
		Locale defaultLocale = Locale.getDefault();
		try {
			Locale.setDefault(new Locale("tr"));
			LocaleSensitiveEnum result = this.conversionService.convert(
					"accept-case-insensitive-properties", LocaleSensitiveEnum.class);
			assertThat(result)
					.isEqualTo(LocaleSensitiveEnum.ACCEPT_CASE_INSENSITIVE_PROPERTIES);
		}
		finally {
			Locale.setDefault(defaultLocale);
		}
	}

	@Parameters(name = "{0}")
	public static Iterable<Object[]> conversionServices() {
		return new ConversionServiceParameters(
				new StringToEnumIgnoringCaseConverterFactory());
	}

	enum TestEnum {

		ONE, TWO, THREE_AND_FOUR

	}

	enum LocaleSensitiveEnum {

		ACCEPT_CASE_INSENSITIVE_PROPERTIES

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
