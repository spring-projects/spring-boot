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
 * Tests for {@link DurationToStringConverter}.
 *
 * @author Edson Chávez
 */
class PeriodToStringConverterTests {

	@ConversionServiceTest
	void convertWithoutStyleShouldReturnIso8601(ConversionService conversionService) {
		String converted = conversionService.convert(Period.ofDays(1), String.class);
		assertThat(converted).isEqualTo(Period.ofDays(1).toString());
	}

	static Stream<? extends Arguments> conversionServices() throws Exception {
		return ConversionServiceArguments.with(new PeriodToStringConverter());
	}

}
