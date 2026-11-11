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
 * Tests for {@link DurationToNumberConverter}.
 *
 * @author Phillip Webb
 * @author Raunak
 */
class DurationToNumberConverterTests {

	@ConversionServiceTest
	void convertWithoutStyleShouldReturnMs(ConversionService conversionService) {
		Long converted = conversionService.convert(Duration.ofSeconds(1), Long.class);
		assertThat(converted).isEqualTo(1000);
	}

	@ConversionServiceTest
	void convertWithFormatShouldUseIgnoreFormat(ConversionService conversionService) {
		Integer converted = (Integer) conversionService.convert(Duration.ofSeconds(1),
				MockDurationTypeDescriptor.get(null, DurationStyle.ISO8601), TypeDescriptor.valueOf(Integer.class));
		assertThat(converted).isEqualTo(1000);
	}

	@ConversionServiceTest
	void convertWithFormatAndUnitShouldUseFormatAndUnit(ConversionService conversionService) {
		Byte converted = (Byte) conversionService.convert(Duration.ofSeconds(1),
				MockDurationTypeDescriptor.get(ChronoUnit.SECONDS, null), TypeDescriptor.valueOf(Byte.class));
		assertThat(converted).isEqualTo((byte) 1);
	}

	@ConversionServiceTest
	void convertWithMicrosUnitShouldSupportFullLongRange(ConversionService conversionService) {
		for (long micros : new long[] { Long.MIN_VALUE, Long.MAX_VALUE }) {
			Duration duration = Duration.of(micros, ChronoUnit.MICROS);
			Long converted = (Long) conversionService.convert(duration,
					MockDurationTypeDescriptor.get(ChronoUnit.MICROS, null), TypeDescriptor.valueOf(Long.class));
			assertThat(converted).isEqualTo(micros);
		}
	}

	static Stream<? extends Arguments> conversionServices() {
		return ConversionServiceArguments.with(new DurationToNumberConverter());
	}

}
