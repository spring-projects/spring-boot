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

import org.junit.jupiter.params.provider.Arguments;
import org.springframework.core.convert.ConversionService;

import java.time.Period;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StringToPeriodConverter}.
 *
 * @author Edson Chávez
 */
public class StringToPeriodConverterTest {

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

	private Period convert(ConversionService conversionService, String source) {
		return conversionService.convert(source, Period.class);
	}

	static Stream<? extends Arguments> conversionServices() {
		return ConversionServiceArguments.with(new StringToPeriodConverter());
	}

}
