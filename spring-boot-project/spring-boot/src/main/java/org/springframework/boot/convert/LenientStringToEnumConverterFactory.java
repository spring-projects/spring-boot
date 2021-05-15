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
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Converts from a String to a {@link java.lang.Enum} with lenient conversion rules.
 * Specifically:
 * <ul>
 * <li>Uses a case insensitive search</li>
 * <li>Does not consider {@code '_'}, {@code '$'} or other special characters</li>
 * <li>Allows mapping of YAML style {@code "false"} and {@code "true"} to enums {@code ON}
 * and {@code OFF}</li>
 * </ul>
 *
 * @author Phillip Webb
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
final class LenientStringToEnumConverterFactory implements ConverterFactory<String, Enum> {

	private static Map<String, List<String>> ALIASES;
	static {
		MultiValueMap<String, String> aliases = new LinkedMultiValueMap<>();
		aliases.add("true", "on");
		aliases.add("false", "off");
		ALIASES = Collections.unmodifiableMap(aliases);
	}

	@Override
	public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
		Class<?> enumType = targetType;
		while (enumType != null && !enumType.isEnum()) {
			enumType = enumType.getSuperclass();
		}
		Assert.notNull(enumType, () -> "The target type " + targetType.getName() + " does not refer to an enum");
		return new StringToEnum(enumType);
	}

	private class StringToEnum<T extends Enum> implements Converter<String, T> {

		private final Class<T> enumType;

		StringToEnum(Class<T> enumType) {
			this.enumType = enumType;
		}

		@Override
		public T convert(String source) {
			if (source.isEmpty()) {
				return null;
			}
			source = source.trim();
			try {
				return (T) Enum.valueOf(this.enumType, source);
			}
			catch (Exception ex) {
				return findEnum(source);
			}
		}

		private T findEnum(String source) {
			Map<String, T> candidates = new LinkedHashMap<String, T>();
			for (T candidate : (Set<T>) EnumSet.allOf(this.enumType)) {
				candidates.put(getLettersAndDigits(candidate.name()), candidate);
			}
			String name = getLettersAndDigits(source);
			T result = candidates.get(name);
			if (result != null) {
				return result;
			}
			for (String alias : ALIASES.getOrDefault(name, Collections.emptyList())) {
				result = candidates.get(alias);
				if (result != null) {
					return result;
				}
			}
			throw new IllegalArgumentException("No enum constant " + this.enumType.getCanonicalName() + "." + source);
		}

		private String getLettersAndDigits(String name) {
			StringBuilder canonicalName = new StringBuilder(name.length());
			name.chars().filter(Character::isLetterOrDigit).map(Character::toLowerCase)
					.forEach((c) -> canonicalName.append((char) c));
			return canonicalName.toString();
		}

	}

}
