/*
 * Copyright 2012-2017 the original author or authors.
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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link Converter} for {@link String} to {@link Duration}. Support
 * {@link Duration#parse(CharSequence)} as well a more readable {@code 10s} form.
 *
 * @author Phillip Webb
 */
class DurationConverter implements GenericConverter {

	private static final Set<ConvertiblePair> TYPES;

	static {
		Set<ConvertiblePair> types = new LinkedHashSet<>();
		types.add(new ConvertiblePair(String.class, Duration.class));
		types.add(new ConvertiblePair(Integer.class, Duration.class));
		TYPES = Collections.unmodifiableSet(types);
	}

	private static final Pattern ISO8601 = Pattern.compile("^[\\+\\-]?P.*$");

	private static final Pattern SIMPLE = Pattern.compile("^([\\+\\-]?\\d+)([a-zA-Z]{0,2})$");

	private static final Map<String, ChronoUnit> UNITS;

	static {
		Map<String, ChronoUnit> units = new LinkedHashMap<>();
		units.put("ns", ChronoUnit.NANOS);
		units.put("ms", ChronoUnit.MILLIS);
		units.put("s", ChronoUnit.SECONDS);
		units.put("m", ChronoUnit.MINUTES);
		units.put("h", ChronoUnit.HOURS);
		units.put("d", ChronoUnit.DAYS);
		UNITS = Collections.unmodifiableMap(units);
	}

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return TYPES;
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType,
			TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		return toDuration(source.toString(),
				targetType.getAnnotation(DefaultDurationUnit.class));
	}

	private Duration toDuration(String source, DefaultDurationUnit defaultUnit) {
		try {
			if (!StringUtils.hasLength(source)) {
				return null;
			}
			if (ISO8601.matcher(source).matches()) {
				return Duration.parse(source);
			}
			Matcher matcher = SIMPLE.matcher(source);
			Assert.state(matcher.matches(), () -> "'" + source + "' is not a valid duration");
			long amount = Long.parseLong(matcher.group(1));
			ChronoUnit unit = getUnit(matcher.group(2), defaultUnit);
			return Duration.of(amount, unit);
		}
		catch (Exception ex) {
			throw new IllegalStateException("'" + source + "' is not a valid duration",
					ex);
		}
	}

	private ChronoUnit getUnit(String value, DefaultDurationUnit defaultUnit) {
		if (StringUtils.isEmpty(value)) {
			return (defaultUnit != null ? defaultUnit.value() : ChronoUnit.MILLIS);
		}
		ChronoUnit unit = UNITS.get(value.toLowerCase());
		Assert.state(unit != null, () -> "Unknown unit '" + value + "'");
		return unit;
	}

}
