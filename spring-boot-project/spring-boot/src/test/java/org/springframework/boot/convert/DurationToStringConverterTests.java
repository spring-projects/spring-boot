/*
 * Copyright 2012-2022 the original author or authors.
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
