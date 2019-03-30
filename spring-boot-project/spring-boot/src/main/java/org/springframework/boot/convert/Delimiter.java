/*
 * Copyright 2012-2018 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a field or method parameter should be converted to collection using the
 * specified delimiter.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER,
		ElementType.ANNOTATION_TYPE })
public @interface Delimiter {

	/**
	 * A delimiter value used to indicate that no delimiter is required and the result
	 * should be a single element containing the entire string.
	 */
	String NONE = "";

	/**
	 * The delimiter to use or {@code NONE} if the entire contents should be treated as a
	 * single element.
	 * @return the delimiter
	 */
	String value();

}
