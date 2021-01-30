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

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CharSequenceToObjectConverter}
 *
 * @author Phillip Webb
 */
class CharSequenceToObjectConverterTests {

	@ConversionServiceTest
	void convertWhenCanConvertViaToString(ConversionService conversionService) {
		assertThat(conversionService.convert(new StringBuilder("1"), Integer.class)).isEqualTo(1);
	}

	@ConversionServiceTest
	void convertWhenCanConvertDirectlySkipsStringConversion(ConversionService conversionService) {
		assertThat(conversionService.convert(new String("1"), Long.class)).isEqualTo(1);
		if (!ConversionServiceArguments.isApplicationConversionService(conversionService)) {
			assertThat(conversionService.convert(new StringBuilder("1"), Long.class)).isEqualTo(2);
		}
	}

	static Stream<? extends Arguments> conversionServices() {
		return ConversionServiceArguments.with((conversionService) -> {
			conversionService.addConverter(new StringToIntegerConverter());
			conversionService.addConverter(new StringToLongConverter());
			conversionService.addConverter(new CharSequenceToLongConverter());
			conversionService.addConverter(new CharSequenceToObjectConverter(conversionService));
		});
	}

	static class StringToIntegerConverter implements Converter<String, Integer> {

		@Override
		public Integer convert(String source) {
			return Integer.valueOf(source);
		}

	}

	static class StringToLongConverter implements Converter<String, Long> {

		@Override
		public Long convert(String source) {
			return Long.valueOf(source);
		}

	}

	static class CharSequenceToLongConverter implements Converter<CharSequence, Long> {

		@Override
		public Long convert(CharSequence source) {
			return Long.valueOf(source.toString()) + 1;
		}

	}

}
