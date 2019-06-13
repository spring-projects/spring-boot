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

package org.springframework.boot.convert;

import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import org.springframework.core.convert.ConversionService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StringToEnumIgnoringCaseConverterFactory}.
 *
 * @author Phillip Webb
 */
class StringToEnumIgnoringCaseConverterFactoryTests {

	@ConversionServiceTest
	void canConvertFromStringToEnumShouldReturnTrue(ConversionService conversionService) {
		assertThat(conversionService.canConvert(String.class, TestEnum.class)).isTrue();
	}

	@ConversionServiceTest
	void canConvertFromStringToEnumSubclassShouldReturnTrue(ConversionService conversionService) {
		assertThat(conversionService.canConvert(String.class, TestSubclassEnum.ONE.getClass())).isTrue();
	}

	@ConversionServiceTest
	void convertFromStringToEnumWhenExactMatchShouldConvertValue(ConversionService conversionService) {
		assertThat(conversionService.convert("", TestEnum.class)).isNull();
		assertThat(conversionService.convert("ONE", TestEnum.class)).isEqualTo(TestEnum.ONE);
		assertThat(conversionService.convert("TWO", TestEnum.class)).isEqualTo(TestEnum.TWO);
		assertThat(conversionService.convert("THREE_AND_FOUR", TestEnum.class)).isEqualTo(TestEnum.THREE_AND_FOUR);
	}

	@ConversionServiceTest
	void convertFromStringToEnumWhenFuzzyMatchShouldConvertValue(ConversionService conversionService) {
		assertThat(conversionService.convert("", TestEnum.class)).isNull();
		assertThat(conversionService.convert("one", TestEnum.class)).isEqualTo(TestEnum.ONE);
		assertThat(conversionService.convert("tWo", TestEnum.class)).isEqualTo(TestEnum.TWO);
		assertThat(conversionService.convert("three_and_four", TestEnum.class)).isEqualTo(TestEnum.THREE_AND_FOUR);
		assertThat(conversionService.convert("threeandfour", TestEnum.class)).isEqualTo(TestEnum.THREE_AND_FOUR);
		assertThat(conversionService.convert("three-and-four", TestEnum.class)).isEqualTo(TestEnum.THREE_AND_FOUR);
		assertThat(conversionService.convert("threeAndFour", TestEnum.class)).isEqualTo(TestEnum.THREE_AND_FOUR);
	}

	@ConversionServiceTest
	void convertFromStringToEnumWhenUsingNonEnglishLocaleShouldConvertValue(ConversionService conversionService) {
		Locale defaultLocale = Locale.getDefault();
		try {
			Locale.setDefault(new Locale("tr"));
			LocaleSensitiveEnum result = conversionService.convert("accept-case-insensitive-properties",
					LocaleSensitiveEnum.class);
			assertThat(result).isEqualTo(LocaleSensitiveEnum.ACCEPT_CASE_INSENSITIVE_PROPERTIES);
		}
		finally {
			Locale.setDefault(defaultLocale);
		}
	}

	static Stream<? extends Arguments> conversionServices() {
		return ConversionServiceArguments
				.with((service) -> service.addConverterFactory(new StringToEnumIgnoringCaseConverterFactory()));
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
