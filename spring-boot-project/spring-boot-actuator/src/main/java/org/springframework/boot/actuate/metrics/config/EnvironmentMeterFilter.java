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

package org.springframework.boot.actuate.metrics.config;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;

import io.micrometer.core.instrument.config.PropertyMeterFilter;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.Environment;


/**
 * Meter filter constructed from properties.
 *
 * @author Jon Schneider
 */
public class EnvironmentMeterFilter extends PropertyMeterFilter {
	private final Environment environment;
	private final DefaultConversionService conversionService = new DefaultConversionService();

	public EnvironmentMeterFilter(Environment environment) {
		this.environment = environment;
	}

	@Override
	public <V> V get(String k, Class<V> vClass) {
		if (this.conversionService.canConvert(String.class, vClass)) {
			Object val = this.environment.getProperty("spring.metrics.filter." + k);
			try {
				return this.conversionService.convert(val, vClass);
			}
			catch (ConversionFailedException e) {
				if (Duration.class.equals(vClass)) {
					Duration d = simpleParse(k);
					if (d != null) {
						// noinspection unchecked
						return (V) d;
					}
				}

				throw new IllegalArgumentException("Invalid configuration for '" + k
						+ "' value '" + val + "' as " + vClass, e);
			}
		}
		return null;
	}

	private static Duration simpleParse(String rawTime) {
		if (rawTime == null || rawTime.isEmpty())
			return null;
		if (!Character.isDigit(rawTime.charAt(0)))
			return null;

		String time = rawTime.toLowerCase();
		return tryParse(time, "ns", Duration::ofNanos)
				.orElseGet(() -> tryParse(time, "ms", Duration::ofMillis).orElseGet(
						() -> tryParse(time, "s", Duration::ofSeconds).orElseGet(
								() -> tryParse(time, "m", Duration::ofMinutes).orElseGet(
										() -> tryParse(time, "h", Duration::ofHours)
												.orElseGet(() -> tryParse(time, "d",
														Duration::ofDays)
																.orElse(null))))));
	}

	private static Optional<Duration> tryParse(String time, String unit,
			Function<Long, Duration> toDuration) {
		if (time.endsWith(unit)) {
			String trim = time.substring(0, time.lastIndexOf(unit)).trim();
			try {
				return Optional.of(toDuration.apply(Long.parseLong(trim)));
			}
			catch (NumberFormatException ignore) {
				return Optional.empty();
			}
		}
		return Optional.empty();
	}
}
