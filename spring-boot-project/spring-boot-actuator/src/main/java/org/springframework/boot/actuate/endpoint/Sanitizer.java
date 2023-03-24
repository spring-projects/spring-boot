/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Strategy that should be used by endpoint implementations to sanitize potentially
 * sensitive keys.
 *
 * @author Christian Dupuis
 * @author Toshiaki Maki
 * @author Phillip Webb
 * @author Nicolas Lejeune
 * @author Stephane Nicoll
 * @author HaiTao Zhang
 * @author Chris Bono
 * @author David Good
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class Sanitizer {

	private final List<SanitizingFunction> sanitizingFunctions = new ArrayList<>();

	/**
	 * Create a new {@link Sanitizer} instance.
	 */
	public Sanitizer() {
		this(Collections.emptyList());
	}

	/**
	 * Create a new {@link Sanitizer} instance with sanitizing functions.
	 * @param sanitizingFunctions the sanitizing functions to apply
	 * @since 2.6.0
	 */
	public Sanitizer(Iterable<SanitizingFunction> sanitizingFunctions) {
		sanitizingFunctions.forEach(this.sanitizingFunctions::add);
	}

	/**
	 * Sanitize the value from the given {@link SanitizableData} using the available
	 * {@link SanitizingFunction}s.
	 * @param data the sanitizable data
	 * @param showUnsanitized whether to show the unsanitized values or not
	 * @return the potentially updated data
	 * @since 3.0.0
	 */
	public Object sanitize(SanitizableData data, boolean showUnsanitized) {
		Object value = data.getValue();
		if (value == null) {
			return null;
		}
		if (!showUnsanitized) {
			return SanitizableData.SANITIZED_VALUE;
		}
		for (SanitizingFunction sanitizingFunction : this.sanitizingFunctions) {
			data = sanitizingFunction.apply(data);
			Object sanitizedValue = data.getValue();
			if (!value.equals(sanitizedValue)) {
				return sanitizedValue;
			}
		}
		return value;
	}

}
