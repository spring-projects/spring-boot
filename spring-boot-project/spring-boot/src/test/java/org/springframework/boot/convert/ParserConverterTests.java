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

import java.text.ParseException;
import java.time.Duration;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.format.Parser;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ParserConverter}.
 *
 * @author Dmytro Nosan
 */
class ParserConverterTests {

	private final DefaultConversionService conversionService = new DefaultConversionService();

	@BeforeEach
	void addParsers() {
		this.conversionService.addConverter(new ParserConverter(new DurationParser()));
		this.conversionService
				.addConverter(new ParserConverter(((Parser<?>) new ProxyFactory(new DataSizeParser()).getProxy())));

	}

	@Test
	void convertStringToDataSize() {
		assertThat(convert("1KB", DataSize.class)).isEqualTo(DataSize.ofKilobytes(1));
		assertThat(convert("", DataSize.class)).isNull();
		assertThat(convert(null, DataSize.class)).isNull();
	}

	@Test
	void convertStringToDuration() {
		assertThat(convert("PT1S", Duration.class)).isEqualTo(Duration.ofSeconds(1));
		assertThat(convert(null, Duration.class)).isNull();
		assertThat(convert("", Duration.class)).isNull();
	}

	@Test
	void shouldFailParserGenericCanNotBeResolved() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.conversionService.addConverter(new ParserConverter((source, locale) -> "")))
				.withMessageContaining("Unable to extract the parameterized type from Parser");
	}

	@Test
	void shouldFailParserThrowsParserException() {
		this.conversionService.addConverter(new ParserConverter(new ObjectParser()));
		assertThatExceptionOfType(ConversionFailedException.class).isThrownBy(() -> convert("Text", Object.class))
				.withCauseInstanceOf(IllegalArgumentException.class)
				.withMessageContaining("Value [Text] can not be parsed");

	}

	private <T> T convert(String source, Class<T> type) {
		return type.cast(this.conversionService.convert(source, TypeDescriptor.valueOf(String.class),
				TypeDescriptor.valueOf(type)));
	}

	private static class DataSizeParser implements Parser<DataSize> {

		@Override
		public DataSize parse(String value, Locale locale) {
			return DataSize.parse(value);
		}

	}

	private static class DurationParser implements Parser<Duration> {

		@Override
		public Duration parse(String value, Locale locale) {
			return Duration.parse(value);
		}

	}

	private static class ObjectParser implements Parser<Object> {

		@Override
		public Object parse(String source, Locale locale) throws ParseException {
			throw new ParseException("", 0);
		}

	}

}
