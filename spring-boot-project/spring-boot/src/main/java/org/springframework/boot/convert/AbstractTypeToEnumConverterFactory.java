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
 * Abstract base class for converting from a type to a {@link java.lang.Enum}.
 *
 * @param <T> the source type
 * @author Phillip Webb
 * @author Madhura Bhave
 */
@SuppressWarnings("rawtypes")
abstract class AbstractTypeToEnumConverterFactory<T> implements ConverterFactory<T, Enum<?>> {

	private static Map<String, List<String>> ALIASES;

	static {
		MultiValueMap<String, String> aliases = new LinkedMultiValueMap<>();
		aliases.add("true", "on");
		aliases.add("false", "off");
		ALIASES = Collections.unmodifiableMap(aliases);
	}

	@Override
	public <E extends Enum<?>> Converter<T, E> getConverter(Class<E> targetType) {
		Class<?> enumType = targetType;
		while (enumType != null && !enumType.isEnum()) {
			enumType = enumType.getSuperclass();
		}
		Assert.notNull(enumType, () -> "The target type " + targetType.getName() + " does not refer to an enum");
		return getTypeToEnumConverter(targetType);
	}

	abstract <E extends Enum> Converter<T, E> getTypeToEnumConverter(Class<E> targetType);

	@SuppressWarnings("unchecked")
	<E extends Enum> E findEnum(String source, Class<E> enumType) {
		Map<String, E> candidates = new LinkedHashMap<>();
		for (E candidate : (Set<E>) EnumSet.allOf(enumType)) {
			candidates.put(getCanonicalName(candidate.name()), candidate);
		}
		String name = getCanonicalName(source);
		E result = candidates.get(name);
		if (result != null) {
			return result;
		}
		for (String alias : ALIASES.getOrDefault(name, Collections.emptyList())) {
			result = candidates.get(alias);
			if (result != null) {
				return result;
			}
		}
		throw new IllegalArgumentException("No enum constant " + enumType.getCanonicalName() + "." + source);

	}

	private String getCanonicalName(String name) {
		StringBuilder canonicalName = new StringBuilder(name.length());
		name.chars().filter(Character::isLetterOrDigit).map(Character::toLowerCase)
				.forEach((c) -> canonicalName.append((char) c));
		return canonicalName.toString();
	}

}
