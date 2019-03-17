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
import org.junit.runners.Parameterized.Parameters;

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
@RunWith(Parameterized.class)
public class StringToDataSizeConverterTests {

	private final ConversionService conversionService;

	public StringToDataSizeConverterTests(String name,
			ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Test
	public void convertWhenSimpleBytesShouldReturnDataSize() {
		assertThat(convert("10B")).isEqualTo(DataSize.ofBytes(10));
		assertThat(convert("+10B")).isEqualTo(DataSize.ofBytes(10));
		assertThat(convert("-10B")).isEqualTo(DataSize.ofBytes(-10));
	}

	@Test
	public void convertWhenSimpleKilobytesShouldReturnDataSize() {
		assertThat(convert("10KB")).isEqualTo(DataSize.ofKilobytes(10));
		assertThat(convert("+10KB")).isEqualTo(DataSize.ofKilobytes(10));
		assertThat(convert("-10KB")).isEqualTo(DataSize.ofKilobytes(-10));
	}

	@Test
	public void convertWhenSimpleMegabytesShouldReturnDataSize() {
		assertThat(convert("10MB")).isEqualTo(DataSize.ofMegabytes(10));
		assertThat(convert("+10MB")).isEqualTo(DataSize.ofMegabytes(10));
		assertThat(convert("-10MB")).isEqualTo(DataSize.ofMegabytes(-10));
	}

	@Test
	public void convertWhenSimpleGigabytesShouldReturnDataSize() {
		assertThat(convert("10GB")).isEqualTo(DataSize.ofGigabytes(10));
		assertThat(convert("+10GB")).isEqualTo(DataSize.ofGigabytes(10));
		assertThat(convert("-10GB")).isEqualTo(DataSize.ofGigabytes(-10));
	}

	@Test
	public void convertWhenSimpleTerabytesShouldReturnDataSize() {
		assertThat(convert("10TB")).isEqualTo(DataSize.ofTerabytes(10));
		assertThat(convert("+10TB")).isEqualTo(DataSize.ofTerabytes(10));
		assertThat(convert("-10TB")).isEqualTo(DataSize.ofTerabytes(-10));
	}

	@Test
	public void convertWhenSimpleWithoutSuffixShouldReturnDataSize() {
		assertThat(convert("10")).isEqualTo(DataSize.ofBytes(10));
		assertThat(convert("+10")).isEqualTo(DataSize.ofBytes(10));
		assertThat(convert("-10")).isEqualTo(DataSize.ofBytes(-10));
	}

	@Test
	public void convertWhenSimpleWithoutSuffixButWithAnnotationShouldReturnDataSize() {
		assertThat(convert("10", DataUnit.KILOBYTES)).isEqualTo(DataSize.ofKilobytes(10));
		assertThat(convert("+10", DataUnit.KILOBYTES))
				.isEqualTo(DataSize.ofKilobytes(10));
		assertThat(convert("-10", DataUnit.KILOBYTES))
				.isEqualTo(DataSize.ofKilobytes(-10));
	}

	@Test
	public void convertWhenBadFormatShouldThrowException() {
		assertThatExceptionOfType(ConversionFailedException.class)
				.isThrownBy(() -> convert("10WB"))
				.withMessageContaining("'10WB' is not a valid data size");
	}

	@Test
	public void convertWhenEmptyShouldReturnNull() {
		assertThat(convert("")).isNull();
	}

	private DataSize convert(String source) {
		return this.conversionService.convert(source, DataSize.class);
	}

	private DataSize convert(String source, DataUnit unit) {
		return (DataSize) this.conversionService.convert(source,
				TypeDescriptor.forObject(source), MockDataSizeTypeDescriptor.get(unit));
	}

	@Parameters(name = "{0}")
	public static Iterable<Object[]> conversionServices() {
		return new ConversionServiceParameters(new StringToDataSizeConverter());
	}

}
