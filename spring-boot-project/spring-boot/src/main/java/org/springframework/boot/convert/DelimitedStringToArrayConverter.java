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

package org.springframework.boot.convert;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Set;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Converts a {@link Delimiter delimited} String to an Array.
 *
 * @author Phillip Webb
 */
final class DelimitedStringToArrayConverter implements ConditionalGenericConverter {

	private final ConversionService conversionService;

	DelimitedStringToArrayConverter(ConversionService conversionService) {
		Assert.notNull(conversionService, "ConversionService must not be null");
		this.conversionService = conversionService;
	}

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(String.class, Object[].class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return targetType.getElementTypeDescriptor() == null || this.conversionService
				.canConvert(sourceType, targetType.getElementTypeDescriptor());
	}

	@Override
	@Nullable
	public Object convert(@Nullable Object source, TypeDescriptor sourceType,
			TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		return convert((String) source, sourceType, targetType);
	}

	private Object convert(String source, TypeDescriptor sourceType,
			TypeDescriptor targetType) {
		Delimiter delimiter = targetType.getAnnotation(Delimiter.class);
		String[] elements = getElements(source,
				(delimiter == null ? "," : delimiter.value()));
		TypeDescriptor elementDescriptor = targetType.getElementTypeDescriptor();
		Object target = Array.newInstance(elementDescriptor.getType(), elements.length);
		for (int i = 0; i < elements.length; i++) {
			String sourceElement = elements[i];
			Object targetElement = this.conversionService.convert(sourceElement.trim(),
					sourceType, elementDescriptor);
			Array.set(target, i, targetElement);
		}
		return target;
	}

	private String[] getElements(String source, String delimiter) {
		return StringUtils.delimitedListToStringArray(source,
				Delimiter.NONE.equals(delimiter) ? null : delimiter);
	}

}
