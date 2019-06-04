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

import java.time.Duration;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.format.Printer;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link PrinterConverter}.
 *
 * @author Dmytro Nosan
 */
class PrinterConverterTests {

	private final DefaultConversionService conversionService = new DefaultConversionService();

	@BeforeEach
	void addPrinters() {
		this.conversionService.addConverter(new PrinterConverter(new DurationPrinter()));
		this.conversionService
				.addConverter(new PrinterConverter(((Printer<?>) new ProxyFactory(new DataSizePrinter()).getProxy())));

	}

	@Test
	void convertDataSizeToString() {
		assertThat(convert(DataSize.ofKilobytes(1), DataSize.class)).isEqualTo("1024B");
		assertThat(convert(null, DataSize.class)).isEmpty();
	}

	@Test
	void convertDurationToString() {
		assertThat(convert(Duration.ofSeconds(1), Duration.class)).isEqualTo("PT1S");
		assertThat(convert(null, Duration.class)).isEmpty();
	}

	@Test
	void shouldFailPrinterGenericCanNotBeResolved() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.conversionService.addConverter(new PrinterConverter((source, locale) -> "")))
				.withMessageContaining("Unable to extract the parameterized type from Printer");
	}

	private <T> String convert(T source, Class<T> type) {
		return (String) this.conversionService.convert(source, TypeDescriptor.valueOf(type),
				TypeDescriptor.valueOf(String.class));
	}

	private static class DataSizePrinter implements Printer<DataSize> {

		@Override
		public String print(DataSize dataSize, Locale locale) {
			return dataSize.toString();
		}

	}

	private static class DurationPrinter implements Printer<Duration> {

		@Override
		public String print(Duration duration, Locale locale) {
			return duration.toString();
		}

	}

}
