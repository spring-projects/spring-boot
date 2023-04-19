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

import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PeriodToStringConverter}.
 *
 * @author Eddú Melendez
 * @author Edson Chávez
 */
class PeriodToStringConverterTests {

	@ConversionServiceTest
	void convertWithoutStyleShouldReturnIso8601(ConversionService conversionService) {
		String converted = conversionService.convert(Period.ofDays(1), String.class);
		assertThat(converted).isEqualTo(Period.ofDays(1).toString());
	}

	@ConversionServiceTest
	void convertWithFormatWhenZeroShouldUseFormatAndDays(ConversionService conversionService) {
		String converted = (String) conversionService.convert(Period.ofMonths(0),
				MockPeriodTypeDescriptor.get(null, PeriodStyle.SIMPLE), TypeDescriptor.valueOf(String.class));
		assertThat(converted).isEqualTo("0d");
	}

	@ConversionServiceTest
	void convertWithFormatShouldUseFormat(ConversionService conversionService) {
		String converted = (String) conversionService.convert(Period.of(1, 2, 3),
				MockPeriodTypeDescriptor.get(null, PeriodStyle.SIMPLE), TypeDescriptor.valueOf(String.class));
		assertThat(converted).isEqualTo("1y2m3d");
	}

	@ConversionServiceTest
	void convertWithFormatAndUnitWhenZeroShouldUseFormatAndUnit(ConversionService conversionService) {
		String converted = (String) conversionService.convert(Period.ofYears(0),
				MockPeriodTypeDescriptor.get(ChronoUnit.YEARS, PeriodStyle.SIMPLE),
				TypeDescriptor.valueOf(String.class));
		assertThat(converted).isEqualTo("0y");
	}

	@ConversionServiceTest
	void convertWithFormatAndUnitWhenNonZeroShouldUseFormatAndIgnoreUnit(ConversionService conversionService) {
		String converted = (String) conversionService.convert(Period.of(1, 0, 3),
				MockPeriodTypeDescriptor.get(ChronoUnit.YEARS, PeriodStyle.SIMPLE),
				TypeDescriptor.valueOf(String.class));
		assertThat(converted).isEqualTo("1y3d");
	}

	@ConversionServiceTest
	void convertWithWeekUnitShouldConvertToStringInDays(ConversionService conversionService) {
		String converted = (String) conversionService.convert(Period.ofWeeks(53),
				MockPeriodTypeDescriptor.get(null, PeriodStyle.SIMPLE), TypeDescriptor.valueOf(String.class));
		assertThat(converted).isEqualTo("371d");
	}

	static Stream<? extends Arguments> conversionServices() {
		return ConversionServiceArguments.with(new PeriodToStringConverter());
	}

}
