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

import java.util.Collection;
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
 * Tests for {@link DelimitedStringToCollectionConverter}.
 *
 * @author Phillip Webb
 */
class DelimitedStringToCollectionConverterTests {

	@ConversionServiceTest
	void canConvertFromStringToCollectionShouldReturnTrue(ConversionService conversionService) {
		assertThat(conversionService.canConvert(String.class, Collection.class)).isTrue();
	}

	@ConversionServiceTest
	void matchesWhenTargetIsNotAnnotatedShouldReturnTrue(ConversionService conversionService) {
		TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
		TypeDescriptor targetType = TypeDescriptor.nested(ReflectionUtils.findField(Values.class, "noAnnotation"), 0);
		assertThat(new DelimitedStringToCollectionConverter(conversionService).matches(sourceType, targetType))
				.isTrue();
	}

	@ConversionServiceTest
	void matchesWhenHasAnnotationAndNoElementTypeShouldReturnTrue(ConversionService conversionService) {
		TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
		TypeDescriptor targetType = TypeDescriptor.nested(ReflectionUtils.findField(Values.class, "noElementType"), 0);
		assertThat(new DelimitedStringToCollectionConverter(conversionService).matches(sourceType, targetType))
				.isTrue();
	}

	@ConversionServiceTest
	void matchesWhenHasAnnotationAndNonConvertibleElementTypeShouldReturnFalse(ConversionService conversionService) {
		TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
		TypeDescriptor targetType = TypeDescriptor
				.nested(ReflectionUtils.findField(Values.class, "nonConvertibleElementType"), 0);
		assertThat(new DelimitedStringToCollectionConverter(conversionService).matches(sourceType, targetType))
				.isFalse();
	}

	@ConversionServiceTest
	@SuppressWarnings("unchecked")
	void convertWhenHasNoElementTypeShouldReturnTrimmedString(ConversionService conversionService) {
		TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
		TypeDescriptor targetType = TypeDescriptor.nested(ReflectionUtils.findField(Values.class, "noElementType"), 0);
		Collection<String> converted = (Collection<String>) conversionService.convert(" a |  b| c  ", sourceType,
				targetType);
		assertThat(converted).containsExactly("a", "b", "c");
	}

	@ConversionServiceTest
	@SuppressWarnings("unchecked")
	void convertWhenHasDelimiterOfNoneShouldReturnWholeString(ConversionService conversionService) {
		TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
		TypeDescriptor targetType = TypeDescriptor.nested(ReflectionUtils.findField(Values.class, "delimiterNone"), 0);
		List<String> converted = (List<String>) conversionService.convert("a,b,c", sourceType, targetType);
		assertThat(converted).containsExactly("a,b,c");
	}

	@SuppressWarnings("unchecked")
	@ConversionServiceTest
	void convertWhenHasCollectionObjectTypeShouldUseCollectionObjectType(ConversionService conversionService) {
		TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
		TypeDescriptor targetType = TypeDescriptor.nested(ReflectionUtils.findField(Values.class, "specificType"), 0);
		MyCustomList<String> converted = (MyCustomList<String>) conversionService.convert("a*b", sourceType,
				targetType);
		assertThat(converted).containsExactly("a", "b");
	}

	@Test
	void matchesWhenHasAnnotationAndConvertibleElementTypeShouldReturnTrue() {
		TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
		TypeDescriptor targetType = TypeDescriptor
				.nested(ReflectionUtils.findField(Values.class, "convertibleElementType"), 0);
		assertThat(new DelimitedStringToCollectionConverter(new ApplicationConversionService()).matches(sourceType,
				targetType)).isTrue();
	}

	@Test
	@SuppressWarnings("unchecked")
	void convertWhenHasConvertibleElementTypeShouldReturnConvertedType() {
		TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
		TypeDescriptor targetType = TypeDescriptor
				.nested(ReflectionUtils.findField(Values.class, "convertibleElementType"), 0);
		List<Integer> converted = (List<Integer>) new ApplicationConversionService().convert(" 1 |  2| 3  ", sourceType,
				targetType);
		assertThat(converted).containsExactly(1, 2, 3);
	}

	static Stream<? extends Arguments> conversionServices() {
		return ConversionServiceArguments
				.with((service) -> service.addConverter(new DelimitedStringToCollectionConverter(service)));
	}

	static class Values {

		List<String> noAnnotation;

		@SuppressWarnings("rawtypes")
		@Delimiter("|")
		List noElementType;

		@Delimiter("|")
		List<Integer> convertibleElementType;

		@Delimiter("|")
		List<NonConvertible> nonConvertibleElementType;

		@Delimiter(Delimiter.NONE)
		List<String> delimiterNone;

		@Delimiter("*")
		MyCustomList<String> specificType;

	}

	static class NonConvertible {

	}

	static class MyCustomList<E> extends LinkedList<E> {

	}

}
