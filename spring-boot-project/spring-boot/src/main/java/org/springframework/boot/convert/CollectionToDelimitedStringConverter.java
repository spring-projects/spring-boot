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
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

/**
 * Converts a Collection to a delimited String.
 *
 * @author Phillip Webb
 */
final class CollectionToDelimitedStringConverter implements ConditionalGenericConverter {

	private final ConversionService conversionService;

	CollectionToDelimitedStringConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Collection.class, String.class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		TypeDescriptor sourceElementType = sourceType.getElementTypeDescriptor();
		if (targetType == null || sourceElementType == null) {
			return true;
		}
		return this.conversionService.canConvert(sourceElementType, targetType)
				|| sourceElementType.getType().isAssignableFrom(targetType.getType());
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		Collection<?> sourceCollection = (Collection<?>) source;
		return convert(sourceCollection, sourceType, targetType);
	}

	private Object convert(Collection<?> source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source.isEmpty()) {
			return "";
		}
		return source.stream().map((element) -> convertElement(element, sourceType, targetType))
				.collect(Collectors.joining(getDelimiter(sourceType)));
	}

	private CharSequence getDelimiter(TypeDescriptor sourceType) {
		Delimiter annotation = sourceType.getAnnotation(Delimiter.class);
		return (annotation != null) ? annotation.value() : ",";
	}

	private String convertElement(Object element, TypeDescriptor sourceType, TypeDescriptor targetType) {
		return String.valueOf(
				this.conversionService.convert(element, sourceType.elementTypeDescriptor(element), targetType));
	}

}
