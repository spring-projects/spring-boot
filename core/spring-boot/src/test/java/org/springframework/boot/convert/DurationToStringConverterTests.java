/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.convert;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DurationToStringConverter}.
 *
 * @author Phillip Webb
 */
class DurationToStringConverterTests {

	@ConversionServiceTest
	void convertWithoutStyleShouldReturnIso8601(ConversionService conversionService) {
		String converted = conversionService.convert(Duration.ofSeconds(1), String.class);
		assertThat(converted).isEqualTo("PT1S");
	}

	@ConversionServiceTest
	void convertWithFormatShouldUseFormatAndMs(ConversionService conversionService) {
		String converted = (String) conversionService.convert(Duration.ofSeconds(1),
				MockDurationTypeDescriptor.get(null, DurationStyle.SIMPLE), TypeDescriptor.valueOf(String.class));
		assertThat(converted).isEqualTo("1000ms");
	}

	@ConversionServiceTest
	void convertWithFormatAndUnitShouldUseFormatAndUnit(ConversionService conversionService) {
		String converted = (String) conversionService.convert(Duration.ofSeconds(1),
				MockDurationTypeDescriptor.get(ChronoUnit.SECONDS, DurationStyle.SIMPLE),
				TypeDescriptor.valueOf(String.class));
		assertThat(converted).isEqualTo("1s");
	}

	static Stream<? extends Arguments> conversionServices() {
		return ConversionServiceArguments.with(new DurationToStringConverter());
	}

}
