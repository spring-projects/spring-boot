/*
 * Copyright 2012-2020 the original author or authors.
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

import java.util.Collections;
import java.util.Set;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

/**
 * {@link ConditionalGenericConverter} to convert {@link CharSequence} type by delegating
 * to existing {@link String} converters.
 *
 * @author Phillip Webb
 */
class CharSequenceToObjectConverter implements ConditionalGenericConverter {

	private static final TypeDescriptor STRING = TypeDescriptor.valueOf(String.class);

	private static final Set<ConvertiblePair> TYPES;

	private final ThreadLocal<Boolean> disable = new ThreadLocal<>();

	static {
		TYPES = Collections.singleton(new ConvertiblePair(CharSequence.class, Object.class));
	}

	private final ConversionService conversionService;

	CharSequenceToObjectConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return TYPES;
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (sourceType.getType() == String.class || this.disable.get() == Boolean.TRUE) {
			return false;
		}
		this.disable.set(Boolean.TRUE);
		try {
			return !this.conversionService.canConvert(sourceType, targetType)
					&& this.conversionService.canConvert(STRING, targetType);
		}
		finally {
			this.disable.set(null);
		}
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		return this.conversionService.convert(source.toString(), STRING, targetType);
	}

}
