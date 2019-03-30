/*
 * Copyright 2012-2018 the original author or authors.
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ArrayToDelimitedStringConverter}.
 *
 * @author Phillip Webb
 */
@RunWith(Parameterized.class)
public class ArrayToDelimitedStringConverterTests {

	private final ConversionService conversionService;

	public ArrayToDelimitedStringConverterTests(String name,
			ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Test
	public void convertListToStringShouldConvert() {
		String[] list = { "a", "b", "c" };
		String converted = this.conversionService.convert(list, String.class);
		assertThat(converted).isEqualTo("a,b,c");
	}

	@Test
	public void convertWhenHasDelimiterNoneShouldConvert() {
		Data data = new Data();
		data.none = new String[] { "1", "2", "3" };
		String converted = (String) this.conversionService.convert(data.none,
				TypeDescriptor.nested(ReflectionUtils.findField(Data.class, "none"), 0),
				TypeDescriptor.valueOf(String.class));
		assertThat(converted).isEqualTo("123");
	}

	@Test
	public void convertWhenHasDelimiterDashShouldConvert() {
		Data data = new Data();
		data.dash = new String[] { "1", "2", "3" };
		String converted = (String) this.conversionService.convert(data.dash,
				TypeDescriptor.nested(ReflectionUtils.findField(Data.class, "dash"), 0),
				TypeDescriptor.valueOf(String.class));
		assertThat(converted).isEqualTo("1-2-3");
	}

	@Test
	public void convertShouldConvertElements() {
		if (this.conversionService instanceof ApplicationConversionService) {
			Data data = new Data();
			data.type = new int[] { 1, 2, 3 };
			String converted = (String) this.conversionService.convert(
					data.type, TypeDescriptor
							.nested(ReflectionUtils.findField(Data.class, "type"), 0),
					TypeDescriptor.valueOf(String.class));
			assertThat(converted).isEqualTo("1.2.3");
		}
	}

	@Test
	public void convertShouldConvertNull() {
		String[] list = null;
		String converted = this.conversionService.convert(list, String.class);
		assertThat(converted).isNull();
	}

	@Parameters(name = "{0}")
	public static Iterable<Object[]> conversionServices() {
		return new ConversionServiceParameters(
				ArrayToDelimitedStringConverterTests::addConverter);
	}

	private static void addConverter(FormattingConversionService service) {
		service.addConverter(new ArrayToDelimitedStringConverter(service));
	}

	static class Data {

		@Delimiter(Delimiter.NONE)
		String[] none;

		@Delimiter("-")
		String[] dash;

		@Delimiter(".")
		int[] type;

	}

}
