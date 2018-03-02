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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Set;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;

/**
 * {@link Converter} to convert from a {@link Duration} to a {@link String}.
 *
 * @author Phillip Webb
 * @see DurationFormat
 * @see DurationUnit
 */
final class DurationToStringConverter implements GenericConverter {

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Duration.class, String.class));
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType,
			TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		DurationFormat format = sourceType.getAnnotation(DurationFormat.class);
		DurationUnit unit = sourceType.getAnnotation(DurationUnit.class);
		return convert((Duration) source, (format == null ? null : format.value()),
				(unit == null ? null : unit.value()));
	}

	private String convert(Duration source, DurationStyle style, ChronoUnit unit) {
		style = (style != null ? style : DurationStyle.ISO8601);
		return style.print(source, unit);
	}

}
