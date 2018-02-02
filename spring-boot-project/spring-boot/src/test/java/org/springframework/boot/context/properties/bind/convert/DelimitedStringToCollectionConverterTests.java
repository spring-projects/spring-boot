/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.properties.bind.convert;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter.ConvertiblePair;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DelimitedStringToCollectionConverter}.
 *
 * @author Phillip Webb
 */
public class DelimitedStringToCollectionConverterTests {

	private DefaultFormattingConversionService service;

	private DelimitedStringToCollectionConverter converter;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Before
	public void setup() {
		this.service = new DefaultFormattingConversionService(null, false);
		this.converter = new DelimitedStringToCollectionConverter(this.service);
		this.service.addConverter(this.converter);
		DefaultFormattingConversionService.addDefaultFormatters(this.service);
	}

	@Test
	public void createWhenConversionServiceIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ConversionService must not be null");
		new DelimitedStringToCollectionConverter(null);
	}

	@Test
	public void getConvertiblePairShouldReturnStringCollectionPair() {
		Set<ConvertiblePair> types = this.converter.getConvertibleTypes();
		assertThat(types).hasSize(1);
		ConvertiblePair pair = types.iterator().next();
		assertThat(pair.getSourceType()).isEqualTo(String.class);
		assertThat(pair.getTargetType()).isEqualTo(Collection.class);
	}

	@Test
	public void matchesWhenTargetIsNotAnnotatedShouldReturnFalse() {
		TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
		TypeDescriptor targetType = TypeDescriptor
				.nested(ReflectionUtils.findField(Values.class, "noAnnotation"), 0);
		assertThat(this.converter.matches(sourceType, targetType)).isFalse();
	}

	@Test
	public void matchesWhenHasAnnotationAndNoElementTypeShouldReturnTrue() {
		TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
		TypeDescriptor targetType = TypeDescriptor
				.nested(ReflectionUtils.findField(Values.class, "noElementType"), 0);
		assertThat(this.converter.matches(sourceType, targetType)).isTrue();
	}

	@Test
	public void matchesWhenHasAnnotationAndConvertibleElementTypeShouldReturnTrue() {
		TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
		TypeDescriptor targetType = TypeDescriptor.nested(
				ReflectionUtils.findField(Values.class, "convertibleElementType"), 0);
		assertThat(this.converter.matches(sourceType, targetType)).isTrue();
	}

	@Test
	public void matchesWhenHasAnnotationAndNonConvertibleElementTypeShouldReturnFalse() {
		TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
		TypeDescriptor targetType = TypeDescriptor.nested(
				ReflectionUtils.findField(Values.class, "nonConvertibleElementType"), 0);
		assertThat(this.converter.matches(sourceType, targetType)).isFalse();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void convertWhenHasNoElementTypeShouldReturnTrimmedString() {
		TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
		TypeDescriptor targetType = TypeDescriptor
				.nested(ReflectionUtils.findField(Values.class, "noElementType"), 0);
		List<String> converted = (List<String>) this.converter.convert(" a |  b| c  ",
				sourceType, targetType);
		assertThat(converted).containsExactly("a", "b", "c");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void convertWhenHasConvertibleElementTypeShouldReturnConvertedType() {
		TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
		TypeDescriptor targetType = TypeDescriptor.nested(
				ReflectionUtils.findField(Values.class, "convertibleElementType"), 0);
		List<Integer> converted = (List<Integer>) this.converter.convert(" 1 |  2| 3  ",
				sourceType, targetType);
		assertThat(converted).containsExactly(1, 2, 3);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void convertWhenHasDelimiterOfNoneShouldReturnTrimmedStringElement() {
		TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
		TypeDescriptor targetType = TypeDescriptor
				.nested(ReflectionUtils.findField(Values.class, "delimiterNone"), 0);
		List<String> converted = (List<String>) this.converter.convert("a,b,c",
				sourceType, targetType);
		assertThat(converted).containsExactly("a,b,c");
	}

	@Test
	public void convertWhenHasCollectionObjectTypeShouldUseCollectionObjectType() {
		TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
		TypeDescriptor targetType = TypeDescriptor
				.nested(ReflectionUtils.findField(Values.class, "specificType"), 0);
		Object converted = this.converter.convert("a*b", sourceType, targetType);
		assertThat(converted).isInstanceOf(MyCustomList.class);
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
