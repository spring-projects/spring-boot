/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DurationToNumberConverter}.
 *
 * @author Phillip Webb
 */
@RunWith(Parameterized.class)
public class DurationToNumberConverterTests {

	private final ConversionService conversionService;

	public DurationToNumberConverterTests(String name,
			ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Test
	public void convertWithoutStyleShouldReturnMs() {
		Long converted = this.conversionService.convert(Duration.ofSeconds(1),
				Long.class);
		assertThat(converted).isEqualTo(1000);
	}

	@Test
	public void convertWithFormatShouldUseIgnoreFormat() {
		Integer converted = (Integer) this.conversionService.convert(
				Duration.ofSeconds(1),
				MockDurationTypeDescriptor.get(null, DurationStyle.ISO8601),
				TypeDescriptor.valueOf(Integer.class));
		assertThat(converted).isEqualTo(1000);
	}

	@Test
	public void convertWithFormatAndUnitShouldUseFormatAndUnit() {
		Byte converted = (Byte) this.conversionService.convert(Duration.ofSeconds(1),
				MockDurationTypeDescriptor.get(ChronoUnit.SECONDS, null),
				TypeDescriptor.valueOf(Byte.class));
		assertThat(converted).isEqualTo((byte) 1);
	}

	@Parameters(name = "{0}")
	public static Iterable<Object[]> conversionServices() {
		return new ConversionServiceParameters(new DurationToNumberConverter());
	}

}
