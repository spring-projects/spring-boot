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
 * Tests for {@link LenientStringToEnumConverterFactory}.
 *
 * @author Phillip Webb
 */
class LenientStringToEnumConverterFactoryTests {

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

	@ConversionServiceTest
	void convertFromStringToEnumWhenYamlBooleanShouldConvertValue(ConversionService conversionService) {
		assertThat(conversionService.convert("one", TestOnOffEnum.class)).isEqualTo(TestOnOffEnum.ONE);
		assertThat(conversionService.convert("two", TestOnOffEnum.class)).isEqualTo(TestOnOffEnum.TWO);
		assertThat(conversionService.convert("true", TestOnOffEnum.class)).isEqualTo(TestOnOffEnum.ON);
		assertThat(conversionService.convert("false", TestOnOffEnum.class)).isEqualTo(TestOnOffEnum.OFF);
		assertThat(conversionService.convert("TRUE", TestOnOffEnum.class)).isEqualTo(TestOnOffEnum.ON);
		assertThat(conversionService.convert("FALSE", TestOnOffEnum.class)).isEqualTo(TestOnOffEnum.OFF);
		assertThat(conversionService.convert("fA_lsE", TestOnOffEnum.class)).isEqualTo(TestOnOffEnum.OFF);
		assertThat(conversionService.convert("one", TestTrueFalseEnum.class)).isEqualTo(TestTrueFalseEnum.ONE);
		assertThat(conversionService.convert("two", TestTrueFalseEnum.class)).isEqualTo(TestTrueFalseEnum.TWO);
		assertThat(conversionService.convert("true", TestTrueFalseEnum.class)).isEqualTo(TestTrueFalseEnum.TRUE);
		assertThat(conversionService.convert("false", TestTrueFalseEnum.class)).isEqualTo(TestTrueFalseEnum.FALSE);
		assertThat(conversionService.convert("TRUE", TestTrueFalseEnum.class)).isEqualTo(TestTrueFalseEnum.TRUE);
		assertThat(conversionService.convert("FALSE", TestTrueFalseEnum.class)).isEqualTo(TestTrueFalseEnum.FALSE);
	}

	static Stream<? extends Arguments> conversionServices() {
		return ConversionServiceArguments
				.with((service) -> service.addConverterFactory(new LenientStringToEnumConverterFactory()));
	}

	enum TestEnum {

		ONE, TWO, THREE_AND_FOUR

	}

	enum TestOnOffEnum {

		ONE, TWO, ON, OFF

	}

	enum TestTrueFalseEnum {

		ONE, TWO, TRUE, FALSE, ON, OFF

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
