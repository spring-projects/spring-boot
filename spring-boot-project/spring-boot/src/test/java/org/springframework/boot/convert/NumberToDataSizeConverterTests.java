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

import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

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
class NumberToDataSizeConverterTests {

	@ConversionServiceTest
	void convertWhenSimpleWithoutSuffixShouldReturnDataSize(ConversionService conversionService) {
		assertThat(convert(conversionService, 10)).isEqualTo(DataSize.ofBytes(10));
		assertThat(convert(conversionService, +10)).isEqualTo(DataSize.ofBytes(10));
		assertThat(convert(conversionService, -10)).isEqualTo(DataSize.ofBytes(-10));
	}

	@ConversionServiceTest
	void convertWhenSimpleWithoutSuffixButWithAnnotationShouldReturnDataSize(ConversionService conversionService) {
		assertThat(convert(conversionService, 10, DataUnit.KILOBYTES)).isEqualTo(DataSize.ofKilobytes(10));
		assertThat(convert(conversionService, +10, DataUnit.KILOBYTES)).isEqualTo(DataSize.ofKilobytes(10));
		assertThat(convert(conversionService, -10, DataUnit.KILOBYTES)).isEqualTo(DataSize.ofKilobytes(-10));
	}

	private DataSize convert(ConversionService conversionService, Integer source) {
		return conversionService.convert(source, DataSize.class);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private DataSize convert(ConversionService conversionService, Integer source, DataUnit defaultUnit) {
		TypeDescriptor targetType = mock(TypeDescriptor.class);
		if (defaultUnit != null) {
			DataSizeUnit unitAnnotation = AnnotationUtils
					.synthesizeAnnotation(Collections.singletonMap("value", defaultUnit), DataSizeUnit.class, null);
			given(targetType.getAnnotation(DataSizeUnit.class)).willReturn(unitAnnotation);
		}
		given(targetType.getType()).willReturn((Class) DataSize.class);
		return (DataSize) conversionService.convert(source, TypeDescriptor.forObject(source), targetType);
	}

	static Stream<? extends Arguments> conversionServices() {
		return ConversionServiceArguments.with(new NumberToDataSizeConverter());
	}

}
