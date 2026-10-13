/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.convert;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import org.springframework.core.convert.ConversionService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CharArrayFormatter}.
 *
 * @author Phillip Webb
 */
class CharArrayFormatterTests {

	@ConversionServiceTest
	void convertFromCharArrayToStringShouldConvert(ConversionService conversionService) {
		char[] source = { 'b', 'o', 'o', 't' };
		String converted = conversionService.convert(source, String.class);
		assertThat(converted).isEqualTo("boot");
	}

	@ConversionServiceTest
	void convertFromStringToCharArrayShouldConvert(ConversionService conversionService) {
		String source = "boot";
		char[] converted = conversionService.convert(source, char[].class);
		assertThat(converted).containsExactly('b', 'o', 'o', 't');
	}

	static Stream<? extends Arguments> conversionServices() {
		return ConversionServiceArguments.with(new CharArrayFormatter());
	}

}
