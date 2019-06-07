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

import java.util.LinkedList;
import java.util.List;

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
 * Tests for {@link DelimitedStringToArrayConverter}.
 *
 * @author Phillip Webb
 */
@RunWith(Parameterized.class)
public class DelimitedStringToArrayConverterTests {

	private final ConversionService conversionService;

	public DelimitedStringToArrayConverterTests(String name, ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Test
	public void canConvertFromStringToArrayShouldReturnTrue() {
		assertThat(this.conversionService.canConvert(String.class, String[].class)).isTrue();
	}

	@Test
	public void matchesWhenTargetIsNotAnnotatedShouldReturnTrue() {
		TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
		TypeDescriptor targetType = TypeDescriptor.nested(ReflectionUtils.findField(Values.class, "noAnnotation"), 0);
		assertThat(new DelimitedStringToArrayConverter(this.conversionService).matches(sourceType, targetType))
				.isTrue();
	}

	@Test
	public void matchesWhenHasAnnotationAndConvertibleElementTypeShouldReturnTrue() {
		if (this.conversionService instanceof ApplicationConversionService) {
			TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
			TypeDescriptor targetType = TypeDescriptor
					.nested(ReflectionUtils.findField(Values.class, "convertibleElementType"), 0);
			assertThat(new DelimitedStringToArrayConverter(this.conversionService).matches(sourceType, targetType))
					.isTrue();
		}
	}

	@Test
	public void matchesWhenHasAnnotationAndNonConvertibleElementTypeShouldReturnFalse() {
		TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
		TypeDescriptor targetType = TypeDescriptor
				.nested(ReflectionUtils.findField(Values.class, "nonConvertibleElementType"), 0);
		assertThat(new DelimitedStringToArrayConverter(this.conversionService).matches(sourceType, targetType))
				.isFalse();
	}

	@Test
	public void convertWhenHasConvertibleElementTypeShouldReturnConvertedType() {
		if (this.conversionService instanceof ApplicationConversionService) {
			TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
			TypeDescriptor targetType = TypeDescriptor
					.nested(ReflectionUtils.findField(Values.class, "convertibleElementType"), 0);
			Integer[] converted = (Integer[]) this.conversionService.convert(" 1 |  2| 3  ", sourceType, targetType);
			assertThat(converted).containsExactly(1, 2, 3);
		}
	}

	@Test
	public void convertWhenHasDelimiterOfNoneShouldReturnWholeString() {
		TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
		TypeDescriptor targetType = TypeDescriptor.nested(ReflectionUtils.findField(Values.class, "delimiterNone"), 0);
		String[] converted = (String[]) this.conversionService.convert("a,b,c", sourceType, targetType);
		assertThat(converted).containsExactly("a,b,c");
	}

	@Parameters(name = "{0}")
	public static Iterable<Object[]> conversionServices() {
		return new ConversionServiceParameters(DelimitedStringToArrayConverterTests::addConverter);
	}

	private static void addConverter(FormattingConversionService service) {
		service.addConverter(new DelimitedStringToArrayConverter(service));
	}

	static class Values {

		List<String> noAnnotation;

		@Delimiter("|")
		Integer[] convertibleElementType;

		@Delimiter("|")
		NonConvertible[] nonConvertibleElementType;

		@Delimiter(Delimiter.NONE)
		String[] delimiterNone;

	}

	static class NonConvertible {

	}

	static class MyCustomList<E> extends LinkedList<E> {

	}

}
