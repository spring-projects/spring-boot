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

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.springframework.boot.DigitalAmount;
import org.springframework.boot.DigitalUnit;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link NumberToDigitalAmountConverter}.
 *
 * @author Dmytro Nosan
 */
@RunWith(Parameterized.class)
public class NumberToDigitalAmountConverterTests {

	private final ConversionService conversionService;

	public NumberToDigitalAmountConverterTests(String name,
			ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Test
	public void convertWhenSimpleWithoutSuffixShouldReturnDigitalAmount() {
		assertThat(convert(10)).isEqualTo(DigitalAmount.fromBytes(10));
		assertThat(convert(+10)).isEqualTo(DigitalAmount.fromBytes(10));
		assertThat(convert(-10)).isEqualTo(DigitalAmount.fromBytes(-10));
	}

	@Test
	public void convertWhenSimpleWithoutSuffixButWithAnnotationShouldReturnDigitalAmount() {
		assertThat(convert(10, DigitalUnit.MEGABYTES))
				.isEqualTo(DigitalAmount.fromMegabytes(10));
		assertThat(convert(+10, DigitalUnit.MEGABYTES))
				.isEqualTo(DigitalAmount.fromMegabytes(10));
		assertThat(convert(-10, DigitalUnit.MEGABYTES))
				.isEqualTo(DigitalAmount.fromMegabytes(-10));
	}

	private DigitalAmount convert(Integer source) {
		return this.conversionService.convert(source, DigitalAmount.class);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private DigitalAmount convert(Integer source, DigitalUnit defaultUnit) {
		TypeDescriptor targetType = mock(TypeDescriptor.class);
		if (defaultUnit != null) {
			DigitalAmountUnit unitAnnotation = AnnotationUtils.synthesizeAnnotation(
					Collections.singletonMap("value", defaultUnit),
					DigitalAmountUnit.class, null);
			given(targetType.getAnnotation(DigitalAmountUnit.class))
					.willReturn(unitAnnotation);
		}
		given(targetType.getType()).willReturn((Class) DigitalAmount.class);
		return (DigitalAmount) this.conversionService.convert(source,
				TypeDescriptor.forObject(source), targetType);
	}

	@Parameterized.Parameters(name = "{0}")
	public static Iterable<Object[]> conversionServices() {
		return new ConversionServiceParameters(new NumberToDigitalAmountConverter());
	}

}
