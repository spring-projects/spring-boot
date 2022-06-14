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

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link StringToDataSizeConverter}.
 *
 * @author Stephane Nicoll
 */
class StringToDataSizeConverterTests {

	@ConversionServiceTest
	void convertWhenSimpleBytesShouldReturnDataSize(ConversionService conversionService) {
		assertThat(convert(conversionService, "10B")).isEqualTo(DataSize.ofBytes(10));
		assertThat(convert(conversionService, "+10B")).isEqualTo(DataSize.ofBytes(10));
		assertThat(convert(conversionService, "-10B")).isEqualTo(DataSize.ofBytes(-10));
	}

	@ConversionServiceTest
	void convertWhenSimpleKilobytesShouldReturnDataSize(ConversionService conversionService) {
		assertThat(convert(conversionService, "10KB")).isEqualTo(DataSize.ofKilobytes(10));
		assertThat(convert(conversionService, "+10KB")).isEqualTo(DataSize.ofKilobytes(10));
		assertThat(convert(conversionService, "-10KB")).isEqualTo(DataSize.ofKilobytes(-10));
	}

	@ConversionServiceTest
	void convertWhenSimpleMegabytesShouldReturnDataSize(ConversionService conversionService) {
		assertThat(convert(conversionService, "10MB")).isEqualTo(DataSize.ofMegabytes(10));
		assertThat(convert(conversionService, "+10MB")).isEqualTo(DataSize.ofMegabytes(10));
		assertThat(convert(conversionService, "-10MB")).isEqualTo(DataSize.ofMegabytes(-10));
	}

	@ConversionServiceTest
	void convertWhenSimpleGigabytesShouldReturnDataSize(ConversionService conversionService) {
		assertThat(convert(conversionService, "10GB")).isEqualTo(DataSize.ofGigabytes(10));
		assertThat(convert(conversionService, "+10GB")).isEqualTo(DataSize.ofGigabytes(10));
		assertThat(convert(conversionService, "-10GB")).isEqualTo(DataSize.ofGigabytes(-10));
	}

	@ConversionServiceTest
	void convertWhenSimpleTerabytesShouldReturnDataSize(ConversionService conversionService) {
		assertThat(convert(conversionService, "10TB")).isEqualTo(DataSize.ofTerabytes(10));
		assertThat(convert(conversionService, "+10TB")).isEqualTo(DataSize.ofTerabytes(10));
		assertThat(convert(conversionService, "-10TB")).isEqualTo(DataSize.ofTerabytes(-10));
	}

	@ConversionServiceTest
	void convertWhenSimpleWithoutSuffixShouldReturnDataSize(ConversionService conversionService) {
		assertThat(convert(conversionService, "10")).isEqualTo(DataSize.ofBytes(10));
		assertThat(convert(conversionService, "+10")).isEqualTo(DataSize.ofBytes(10));
		assertThat(convert(conversionService, "-10")).isEqualTo(DataSize.ofBytes(-10));
	}

	@ConversionServiceTest
	void convertWhenSimpleWithoutSuffixButWithAnnotationShouldReturnDataSize(ConversionService conversionService) {
		assertThat(convert(conversionService, "10", DataUnit.KILOBYTES)).isEqualTo(DataSize.ofKilobytes(10));
		assertThat(convert(conversionService, "+10", DataUnit.KILOBYTES)).isEqualTo(DataSize.ofKilobytes(10));
		assertThat(convert(conversionService, "-10", DataUnit.KILOBYTES)).isEqualTo(DataSize.ofKilobytes(-10));
	}

	@ConversionServiceTest
	void convertWhenBadFormatShouldThrowException(ConversionService conversionService) {
		assertThatExceptionOfType(ConversionFailedException.class).isThrownBy(() -> convert(conversionService, "10WB"))
				.havingCause().isInstanceOf(IllegalArgumentException.class)
				.withMessageContaining("'10WB' is not a valid data size");
	}

	@ConversionServiceTest
	void convertWhenEmptyShouldReturnNull(ConversionService conversionService) {
		assertThat(convert(conversionService, "")).isNull();
	}

	private DataSize convert(ConversionService conversionService, String source) {
		return conversionService.convert(source, DataSize.class);
	}

	private DataSize convert(ConversionService conversionService, String source, DataUnit unit) {
		return (DataSize) conversionService.convert(source, TypeDescriptor.forObject(source),
				MockDataSizeTypeDescriptor.get(unit));
	}

	static Stream<? extends Arguments> conversionServices() {
		return ConversionServiceArguments.with(new StringToDataSizeConverter());
	}

}
