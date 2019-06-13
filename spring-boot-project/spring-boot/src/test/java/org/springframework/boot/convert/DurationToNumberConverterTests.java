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

	static Stream<? extends Arguments> conversionServices() {
		return ConversionServiceArguments.with(new DurationToNumberConverter());
	}

}
