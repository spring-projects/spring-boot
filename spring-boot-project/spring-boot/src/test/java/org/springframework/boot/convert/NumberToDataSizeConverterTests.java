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

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link NumberToDataSizeConverter}.
 *
 * @author Stephane Nicoll
 */
@RunWith(Parameterized.class)
public class NumberToDataSizeConverterTests {

	private final ConversionService conversionService;

	public NumberToDataSizeConverterTests(String name,
			ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Test
	public void convertWhenSimpleWithoutSuffixShouldReturnDataSize() {
		assertThat(convert(10)).isEqualTo(DataSize.ofBytes(10));
		assertThat(convert(+10)).isEqualTo(DataSize.ofBytes(10));
		assertThat(convert(-10)).isEqualTo(DataSize.ofBytes(-10));
	}

	@Test
	public void convertWhenSimpleWithoutSuffixButWithAnnotationShouldReturnDataSize() {
		assertThat(convert(10, DataUnit.KILOBYTES)).isEqualTo(DataSize.ofKilobytes(10));
		assertThat(convert(+10, DataUnit.KILOBYTES)).isEqualTo(DataSize.ofKilobytes(10));
		assertThat(convert(-10, DataUnit.KILOBYTES)).isEqualTo(DataSize.ofKilobytes(-10));
	}

	private DataSize convert(Integer source) {
		return this.conversionService.convert(source, DataSize.class);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private DataSize convert(Integer source, DataUnit defaultUnit) {
		TypeDescriptor targetType = mock(TypeDescriptor.class);
		if (defaultUnit != null) {
			DataSizeUnit unitAnnotation = AnnotationUtils.synthesizeAnnotation(
					Collections.singletonMap("value", defaultUnit), DataSizeUnit.class,
					null);
			given(targetType.getAnnotation(DataSizeUnit.class))
					.willReturn(unitAnnotation);
		}
		given(targetType.getType()).willReturn((Class) DataSize.class);
		return (DataSize) this.conversionService.convert(source,
				TypeDescriptor.forObject(source), targetType);
	}

	@Parameterized.Parameters(name = "{0}")
	public static Iterable<Object[]> conversionServices() {
		return new ConversionServiceParameters(new NumberToDataSizeConverter());
	}

}
