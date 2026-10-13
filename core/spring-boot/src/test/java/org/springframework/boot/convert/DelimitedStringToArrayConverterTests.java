/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.convert;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
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
		Field field = ReflectionUtils.findField(Values.class, "noAnnotation");
		assertThat(field).isNotNull();
		TypeDescriptor targetType = TypeDescriptor.nested(field, 0);
		assertThat(targetType).isNotNull();
		assertThat(new DelimitedStringToArrayConverter(conversionService).matches(sourceType, targetType)).isTrue();
	}

	@ConversionServiceTest
	void matchesWhenHasAnnotationAndNonConvertibleElementTypeShouldReturnFalse(ConversionService conversionService) {
		TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
		Field field = ReflectionUtils.findField(Values.class, "nonConvertibleElementType");
		assertThat(field).isNotNull();
		TypeDescriptor targetType = TypeDescriptor.nested(field, 0);
		assertThat(targetType).isNotNull();
		assertThat(new DelimitedStringToArrayConverter(conversionService).matches(sourceType, targetType)).isFalse();
	}

	@ConversionServiceTest
	void convertWhenHasDelimiterOfNoneShouldReturnWholeString(ConversionService conversionService) {
		TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
		Field field = ReflectionUtils.findField(Values.class, "delimiterNone");
		assertThat(field).isNotNull();
		TypeDescriptor targetType = TypeDescriptor.nested(field, 0);
		assertThat(targetType).isNotNull();
		String[] converted = (String[]) conversionService.convert("a,b,c", sourceType, targetType);
		assertThat(converted).containsExactly("a,b,c");
	}

	@Test
	void matchesWhenHasAnnotationAndConvertibleElementTypeShouldReturnTrue() {
		TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
		Field field = ReflectionUtils.findField(Values.class, "convertibleElementType");
		assertThat(field).isNotNull();
		TypeDescriptor targetType = TypeDescriptor.nested(field, 0);
		assertThat(targetType).isNotNull();
		assertThat(
				new DelimitedStringToArrayConverter(new ApplicationConversionService()).matches(sourceType, targetType))
			.isTrue();
	}

	@Test
	void convertWhenHasConvertibleElementTypeShouldReturnConvertedType() {
		TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
		Field field = ReflectionUtils.findField(Values.class, "convertibleElementType");
		assertThat(field).isNotNull();
		TypeDescriptor targetType = TypeDescriptor.nested(field, 0);
		assertThat(targetType).isNotNull();
		Integer[] converted = (Integer[]) new ApplicationConversionService().convert(" 1 |  2| 3  ", sourceType,
				targetType);
		assertThat(converted).containsExactly(1, 2, 3);
	}

	static Stream<? extends Arguments> conversionServices() {
		return ConversionServiceArguments
			.with((service) -> service.addConverter(new DelimitedStringToArrayConverter(service)));
	}

	static class Values {

		@Nullable List<String> noAnnotation;

		@Delimiter("|")
		Integer @Nullable [] convertibleElementType;

		@Delimiter("|")
		NonConvertible @Nullable [] nonConvertibleElementType;

		@Delimiter(Delimiter.NONE)
		String @Nullable [] delimiterNone;

	}

	static class NonConvertible {

	}

	static class MyCustomList<E> extends LinkedList<E> {

	}

}
