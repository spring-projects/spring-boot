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

import java.util.Collections;
import java.util.Set;

import org.springframework.boot.DigitalAmount;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;

/**
 * {@link Converter} to convert from a {@link Number} to a
 * {@link org.springframework.boot.DigitalAmount}.
 *
 * @author Dmytro Nosan
 * @since 2.1.0
 * @see DigitalAmountFormat
 * @see DigitalAmountUnit
 */
final class NumberToDigitalAmountConverter implements GenericConverter {

	private final StringToDigitalAmountConverter delegate = new StringToDigitalAmountConverter();

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections
				.singleton(new ConvertiblePair(Number.class, DigitalAmount.class));
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType,
			TypeDescriptor targetType) {
		return this.delegate.convert((source != null) ? source.toString() : null,
				TypeDescriptor.valueOf(String.class), targetType);
	}

}
