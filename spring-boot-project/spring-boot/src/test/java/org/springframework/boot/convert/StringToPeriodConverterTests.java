/*
 * Copyright 2012-2020 the original author or authors.
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
 * Tests for {@link StringToPeriodConverter}.
 *
 * @author Eddú Meléndez
 * @author Edson Chávez
 */
class StringToPeriodConverterTests {

	@ConversionServiceTest
	void convertWhenIso8601ShouldReturnPeriod(ConversionService conversionService) {
		assertThat(convert(conversionService, "P2Y")).isEqualTo(Period.parse("P2Y"));
		assertThat(convert(conversionService, "P3M")).isEqualTo(Period.parse("P3M"));
		assertThat(convert(conversionService, "P4W")).isEqualTo(Period.parse("P4W"));
		assertThat(convert(conversionService, "P5D")).isEqualTo(Period.parse("P5D"));
		assertThat(convert(conversionService, "P1Y2M3D")).isEqualTo(Period.parse("P1Y2M3D"));
		assertThat(convert(conversionService, "P1Y2M3W4D")).isEqualTo(Period.parse("P1Y2M3W4D"));
		assertThat(convert(conversionService, "P-1Y2M")).isEqualTo(Period.parse("P-1Y2M"));
		assertThat(convert(conversionService, "-P1Y2M")).isEqualTo(Period.parse("-P1Y2M"));
	}

	@ConversionServiceTest
	void convertWhenSimpleDaysShouldReturnPeriod(ConversionService conversionService) {
		assertThat(convert(conversionService, "10d")).isEqualTo(Period.ofDays(10));
		assertThat(convert(conversionService, "10D")).isEqualTo(Period.ofDays(10));
		assertThat(convert(conversionService, "+10d")).isEqualTo(Period.ofDays(10));
		assertThat(convert(conversionService, "-10D")).isEqualTo(Period.ofDays(-10));
	}

	@ConversionServiceTest
	void convertWhenSimpleWeeksShouldReturnPeriod(ConversionService conversionService) {
		assertThat(convert(conversionService, "10w")).isEqualTo(Period.ofWeeks(10));
		assertThat(convert(conversionService, "10W")).isEqualTo(Period.ofWeeks(10));
		assertThat(convert(conversionService, "+10w")).isEqualTo(Period.ofWeeks(10));
		assertThat(convert(conversionService, "-10W")).isEqualTo(Period.ofWeeks(-10));
	}

	@ConversionServiceTest
	void convertWhenSimpleMonthsShouldReturnPeriod(ConversionService conversionService) {
		assertThat(convert(conversionService, "10m")).isEqualTo(Period.ofMonths(10));
		assertThat(convert(conversionService, "10M")).isEqualTo(Period.ofMonths(10));
		assertThat(convert(conversionService, "+10m")).isEqualTo(Period.ofMonths(10));
		assertThat(convert(conversionService, "-10M")).isEqualTo(Period.ofMonths(-10));
	}

	@ConversionServiceTest
	void convertWhenSimpleYearsShouldReturnPeriod(ConversionService conversionService) {
		assertThat(convert(conversionService, "10y")).isEqualTo(Period.ofYears(10));
		assertThat(convert(conversionService, "10Y")).isEqualTo(Period.ofYears(10));
		assertThat(convert(conversionService, "+10y")).isEqualTo(Period.ofYears(10));
		assertThat(convert(conversionService, "-10Y")).isEqualTo(Period.ofYears(-10));
	}

	@ConversionServiceTest
	void convertWhenSimpleWithoutSuffixShouldReturnPeriod(ConversionService conversionService) {
		assertThat(convert(conversionService, "10")).isEqualTo(Period.ofDays(10));
		assertThat(convert(conversionService, "+10")).isEqualTo(Period.ofDays(10));
		assertThat(convert(conversionService, "-10")).isEqualTo(Period.ofDays(-10));
	}

	@ConversionServiceTest
	void convertWhenSimpleWithoutSuffixButWithAnnotationShouldReturnPeriod(ConversionService conversionService) {
		assertThat(convert(conversionService, "10", ChronoUnit.DAYS, null)).isEqualTo(Period.ofDays(10));
		assertThat(convert(conversionService, "+10", ChronoUnit.DAYS, null)).isEqualTo(Period.ofDays(10));
		assertThat(convert(conversionService, "-10", ChronoUnit.DAYS, null)).isEqualTo(Period.ofDays(-10));
		assertThat(convert(conversionService, "10", ChronoUnit.WEEKS, null)).isEqualTo(Period.ofWeeks(10));
		assertThat(convert(conversionService, "+10", ChronoUnit.WEEKS, null)).isEqualTo(Period.ofWeeks(10));
		assertThat(convert(conversionService, "-10", ChronoUnit.WEEKS, null)).isEqualTo(Period.ofWeeks(-10));
		assertThat(convert(conversionService, "10", ChronoUnit.MONTHS, null)).isEqualTo(Period.ofMonths(10));
		assertThat(convert(conversionService, "+10", ChronoUnit.MONTHS, null)).isEqualTo(Period.ofMonths(10));
		assertThat(convert(conversionService, "-10", ChronoUnit.MONTHS, null)).isEqualTo(Period.ofMonths(-10));
		assertThat(convert(conversionService, "10", ChronoUnit.YEARS, null)).isEqualTo(Period.ofYears(10));
		assertThat(convert(conversionService, "+10", ChronoUnit.YEARS, null)).isEqualTo(Period.ofYears(10));
		assertThat(convert(conversionService, "-10", ChronoUnit.YEARS, null)).isEqualTo(Period.ofYears(-10));
	}

	private Period convert(ConversionService conversionService, String source) {
		return conversionService.convert(source, Period.class);
	}

	private Period convert(ConversionService conversionService, String source, ChronoUnit unit, PeriodStyle style) {
		return (Period) conversionService.convert(source, TypeDescriptor.forObject(source),
				MockPeriodTypeDescriptor.get(unit, style));
	}

	static Stream<? extends Arguments> conversionServices() {
		return ConversionServiceArguments.with(new StringToPeriodConverter());
	}

}
