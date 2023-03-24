/*
 * Copyright 2012-2021 the original author or authors.
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

/**
 * Function that takes a {@link SanitizableData} and applies sanitization to the value, if
 * necessary. Can be used by a {@link Sanitizer} to determine the sanitized value.
 *
 * @author Madhura Bhave
 * @since 2.6.0
 */
@FunctionalInterface
public interface SanitizingFunction {

	/**
	 * Apply the sanitizing function to the given data.
	 * @param data the data to sanitize
	 * @return the sanitized data or the original instance is no sanitization is applied
	 */
	SanitizableData apply(SanitizableData data);

}
