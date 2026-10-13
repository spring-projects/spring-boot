/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.convert;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CharSequenceToObjectConverter}
 *
 * @author Phillip Webb
 */
class CharSequenceToObjectConverterTests {

	@ConversionServiceTest
	void convertWhenCanConvertViaToString(ConversionService conversionService) {
		assertThat(conversionService.convert(new StringBuilder("1"), Integer.class)).isOne();
	}

	@ConversionServiceTest
	void convertWhenCanConvertDirectlySkipsStringConversion(ConversionService conversionService) {
		assertThat(conversionService.convert(new String("1"), Long.class)).isOne();
		if (!ConversionServiceArguments.isApplicationConversionService(conversionService)) {
			assertThat(conversionService.convert(new StringBuilder("1"), Long.class)).isEqualTo(2);
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void convertWhenTargetIsList() {
		ConversionService conversionService = new ApplicationConversionService();
		StringBuilder source = new StringBuilder("1,2,3");
		TypeDescriptor sourceType = TypeDescriptor.valueOf(StringBuilder.class);
		TypeDescriptor targetType = TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(String.class));
		List<String> converted = (List<String>) conversionService.convert(source, sourceType, targetType);
		assertThat(converted).containsExactly("1", "2", "3");
	}

	@Test
	@SuppressWarnings("unchecked")
	void convertWhenTargetIsListAndNotUsingApplicationConversionService() {
		FormattingConversionService conversionService = new DefaultFormattingConversionService();
		conversionService.addConverter(new CharSequenceToObjectConverter(conversionService));
		StringBuilder source = new StringBuilder("1,2,3");
		TypeDescriptor sourceType = TypeDescriptor.valueOf(StringBuilder.class);
		TypeDescriptor targetType = TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(String.class));
		List<String> converted = (List<String>) conversionService.convert(source, sourceType, targetType);
		assertThat(converted).containsExactly("1", "2", "3");
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
			return Long.parseLong(source.toString()) + 1;
		}

	}

}
