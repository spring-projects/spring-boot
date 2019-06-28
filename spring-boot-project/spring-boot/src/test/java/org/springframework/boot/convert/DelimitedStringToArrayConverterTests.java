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
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DelimitedStringToArrayConverter}.
 *
 * @author Phillip Webb
 */
class DelimitedStringToArrayConverterTests {

	@ConversionServiceTest
	void canConvertFromStringToArrayShouldReturnTrue(ConversionService conversionService) {
		assertThat(conversionService.canConvert(String.class, String[].class)).isTrue();
	}

	@ConversionServiceTest
	void matchesWhenTargetIsNotAnnotatedShouldReturnTrue(ConversionService conversionService) {
		TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
		TypeDescriptor targetType = TypeDescriptor.nested(ReflectionUtils.findField(Values.class, "noAnnotation"), 0);
		assertThat(new DelimitedStringToArrayConverter(conversionService).matches(sourceType, targetType)).isTrue();
	}

	@ConversionServiceTest
	void matchesWhenHasAnnotationAndNonConvertibleElementTypeShouldReturnFalse(ConversionService conversionService) {
		TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
		TypeDescriptor targetType = TypeDescriptor
				.nested(ReflectionUtils.findField(Values.class, "nonConvertibleElementType"), 0);
		assertThat(new DelimitedStringToArrayConverter(conversionService).matches(sourceType, targetType)).isFalse();
	}

	@ConversionServiceTest
	void convertWhenHasDelimiterOfNoneShouldReturnWholeString(ConversionService conversionService) {
		TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
		TypeDescriptor targetType = TypeDescriptor.nested(ReflectionUtils.findField(Values.class, "delimiterNone"), 0);
		String[] converted = (String[]) conversionService.convert("a,b,c", sourceType, targetType);
		assertThat(converted).containsExactly("a,b,c");
	}

	@Test
	void matchesWhenHasAnnotationAndConvertibleElementTypeShouldReturnTrue() {
		TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
		TypeDescriptor targetType = TypeDescriptor
				.nested(ReflectionUtils.findField(Values.class, "convertibleElementType"), 0);
		assertThat(
				new DelimitedStringToArrayConverter(new ApplicationConversionService()).matches(sourceType, targetType))
						.isTrue();
	}

	@Test
	void convertWhenHasConvertibleElementTypeShouldReturnConvertedType() {
		TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
		TypeDescriptor targetType = TypeDescriptor
				.nested(ReflectionUtils.findField(Values.class, "convertibleElementType"), 0);
		Integer[] converted = (Integer[]) new ApplicationConversionService().convert(" 1 |  2| 3  ", sourceType,
				targetType);
		assertThat(converted).containsExactly(1, 2, 3);
	}

	static Stream<? extends Arguments> conversionServices() {
		return ConversionServiceArguments
				.with((service) -> service.addConverter(new DelimitedStringToArrayConverter(service)));
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
