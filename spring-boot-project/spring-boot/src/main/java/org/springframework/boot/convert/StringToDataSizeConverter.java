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

import java.util.Collections;
import java.util.Set;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.util.ObjectUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

/**
 * {@link Converter} to convert from a {@link String} to a {@link DataSize}. Supports
 * {@link DataSize#parse(CharSequence)}.
 *
 * @author Stephane Nicoll
 * @see DataSizeUnit
 */
final class StringToDataSizeConverter implements GenericConverter {

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(String.class, DataSize.class));
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (ObjectUtils.isEmpty(source)) {
			return null;
		}
		return convert(source.toString(), getDataUnit(targetType));
	}

	private DataUnit getDataUnit(TypeDescriptor targetType) {
		DataSizeUnit annotation = targetType.getAnnotation(DataSizeUnit.class);
		return (annotation != null) ? annotation.value() : null;
	}

	private DataSize convert(String source, DataUnit unit) {
		return DataSize.parse(source, unit);
	}

}
