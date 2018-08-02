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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.springframework.boot.DigitalAmount;
import org.springframework.boot.DigitalUnit;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DigitalAmountToNumberConverter}.
 *
 * @author Dmytro Nosan
 */
@RunWith(Parameterized.class)
public class DigitalAmountToNumberConverterTests {

	private final ConversionService conversionService;

	public DigitalAmountToNumberConverterTests(String name,
			ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Test
	public void convertWithoutStyleShouldReturnBytes() {
		Long converted = this.conversionService.convert(DigitalAmount.fromKilobytes(1),
				Long.class);
		assertThat(converted).isEqualTo(1024L);
	}

	@Test
	public void convertWithFormatAndUnitShouldUseFormatAndUnit() {
		Integer converted = (Integer) this.conversionService.convert(
				DigitalAmount.fromMegabytes(1),
				MockDigitalAmountTypeDescriptor.get(DigitalUnit.KILOBYTES, null),
				TypeDescriptor.valueOf(Integer.class));
		assertThat(converted).isEqualTo(1024);
	}

	@Parameterized.Parameters(name = "{0}")
	public static Iterable<Object[]> conversionServices() {
		return new ConversionServiceParameters(new DigitalAmountToNumberConverter());
	}

}
