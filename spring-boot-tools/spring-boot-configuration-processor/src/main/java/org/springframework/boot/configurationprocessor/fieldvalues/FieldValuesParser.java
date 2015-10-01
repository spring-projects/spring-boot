/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.configurationprocessor.fieldvalues;

import java.util.Collections;
import java.util.Map;

import javax.lang.model.element.TypeElement;

import org.springframework.boot.configurationprocessor.fieldvalues.javac.JavaCompilerFieldValuesParser;

/**
 * Parser which can be used to obtain the field values from an {@link TypeElement}.
 *
 * @author Phillip Webb
 * @since 1.1.2
 * @see JavaCompilerFieldValuesParser
 */
public interface FieldValuesParser {

	/**
	 * Implementation of {@link FieldValuesParser} that always returns an empty result.
	 */
	FieldValuesParser NONE = new FieldValuesParser() {

		@Override
		public Map<String, Object> getFieldValues(TypeElement element) {
			return Collections.emptyMap();
		}

	};

	/**
	 * Return the field values for the given element.
	 * @param element the element to inspect
	 * @return a map of field names to values.
	 * @throws Exception if the values cannot be extracted
	 */
	Map<String, Object> getFieldValues(TypeElement element) throws Exception;

}
