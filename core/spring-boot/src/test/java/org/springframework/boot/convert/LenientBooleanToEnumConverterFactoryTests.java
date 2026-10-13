/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.convert;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import org.springframework.core.convert.ConversionService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LenientBooleanToEnumConverterFactory}.
 *
 * @author Madhura Bhave
 */
class LenientBooleanToEnumConverterFactoryTests {

	@ConversionServiceTest
	void convertFromBooleanToEnumWhenShouldConvertValue(ConversionService conversionService) {
		assertThat(conversionService.convert(true, TestOnOffEnum.class)).isEqualTo(TestOnOffEnum.ON);
		assertThat(conversionService.convert(false, TestOnOffEnum.class)).isEqualTo(TestOnOffEnum.OFF);
		assertThat(conversionService.convert(true, TestTrueFalseEnum.class)).isEqualTo(TestTrueFalseEnum.TRUE);
		assertThat(conversionService.convert(false, TestTrueFalseEnum.class)).isEqualTo(TestTrueFalseEnum.FALSE);
	}

	static Stream<? extends Arguments> conversionServices() {
		return ConversionServiceArguments
			.with((service) -> service.addConverterFactory(new LenientBooleanToEnumConverterFactory()));
	}

	enum TestOnOffEnum {

		ON, OFF

	}

	enum TestTrueFalseEnum {

		ONE, TWO, TRUE, FALSE, ON, OFF

	}

}
