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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CollectionToDelimitedStringConverter}.
 *
 * @author Phillip Webb
 */
class CollectionToDelimitedStringConverterTests {

	@ConversionServiceTest
	void convertListToStringShouldConvert(ConversionService conversionService) {
		List<String> list = Arrays.asList("a", "b", "c");
		String converted = conversionService.convert(list, String.class);
		assertThat(converted).isEqualTo("a,b,c");
	}

	@ConversionServiceTest
	void convertWhenHasDelimiterNoneShouldConvert(ConversionService conversionService) {
		Data data = new Data();
		data.none = Arrays.asList("1", "2", "3");
		String converted = (String) conversionService.convert(data.none,
				TypeDescriptor.nested(ReflectionUtils.findField(Data.class, "none"), 0),
				TypeDescriptor.valueOf(String.class));
		assertThat(converted).isEqualTo("123");
	}

	@ConversionServiceTest
	void convertWhenHasDelimiterDashShouldConvert(ConversionService conversionService) {
		Data data = new Data();
		data.dash = Arrays.asList("1", "2", "3");
		String converted = (String) conversionService.convert(data.dash,
				TypeDescriptor.nested(ReflectionUtils.findField(Data.class, "dash"), 0),
				TypeDescriptor.valueOf(String.class));
		assertThat(converted).isEqualTo("1-2-3");
	}

	@ConversionServiceTest
	void convertShouldConvertNull(ConversionService conversionService) {
		List<String> list = null;
		String converted = conversionService.convert(list, String.class);
		assertThat(converted).isNull();
	}

	@Test
	void convertShouldConvertElements() {
		Data data = new Data();
		data.type = Arrays.asList(1, 2, 3);
		String converted = (String) new ApplicationConversionService().convert(data.type,
				TypeDescriptor.nested(ReflectionUtils.findField(Data.class, "type"), 0),
				TypeDescriptor.valueOf(String.class));
		assertThat(converted).isEqualTo("1.2.3");
	}

	static Stream<? extends Arguments> conversionServices() {
		return ConversionServiceArguments
				.with((service) -> service.addConverter(new CollectionToDelimitedStringConverter(service)));
	}

	static class Data {

		@Delimiter(Delimiter.NONE)
		List<String> none;

		@Delimiter("-")
		List<String> dash;

		@Delimiter(".")
		List<Integer> type;

	}

}
