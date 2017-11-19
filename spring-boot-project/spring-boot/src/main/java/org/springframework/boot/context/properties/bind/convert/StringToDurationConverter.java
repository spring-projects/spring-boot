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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Assert;

/**
 * {@link Converter} for {@link String} to {@link Duration}. Support
 * {@link Duration#parse(CharSequence)} as well a more readable {@code 10s} form.
 *
 * @author Phillip Webb
 */
class StringToDurationConverter implements Converter<String, Duration> {

	private static Pattern ISO8601 = Pattern.compile("^[\\+\\-]?P.*$");

	private static Pattern SIMPLE = Pattern.compile("^([\\+\\-]?\\d+)([a-zA-Z]{1,2})$");

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
	public Duration convert(String source) {
		try {
			if (ISO8601.matcher(source).matches()) {
				return Duration.parse(source);
			}
			Matcher matcher = SIMPLE.matcher(source);
			Assert.state(matcher.matches(), "'" + source + "' is not a valid duration");
			long amount = Long.parseLong(matcher.group(1));
			ChronoUnit unit = getUnit(matcher.group(2));
			return Duration.of(amount, unit);
		}
		catch (Exception ex) {
			throw new IllegalStateException("'" + source + "' is not a valid duration",
					ex);
		}
	}

	private ChronoUnit getUnit(String value) {
		ChronoUnit unit = UNITS.get(value.toLowerCase());
		Assert.state(unit != null, "Unknown unit '" + value + "'");
		return unit;
	}

}
