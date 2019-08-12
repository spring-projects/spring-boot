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

import org.springframework.core.convert.converter.Converter;

/**
 * Converter to support mapping of YAML style {@code "false"} and {@code "true"} to enums
 * {@code ON} and {@code OFF}.
 *
 * @author Madhura Bhave
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
final class BooleanToEnumConverterFactory extends AbstractTypeToEnumConverterFactory<Boolean> {

	@Override
	<E extends Enum> Converter<Boolean, E> getTypeToEnumConverter(Class<E> targetType) {
		return new BooleanToEnum<>(targetType);
	}

	private class BooleanToEnum<T extends Enum> implements Converter<Boolean, T> {

		private final Class<T> enumType;

		BooleanToEnum(Class<T> enumType) {
			this.enumType = enumType;
		}

		@Override
		public T convert(Boolean source) {
			return findEnum(Boolean.toString(source), this.enumType);
		}

	}

}
