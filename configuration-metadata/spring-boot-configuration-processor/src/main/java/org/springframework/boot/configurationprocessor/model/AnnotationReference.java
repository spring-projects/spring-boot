/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.configurationprocessor.model;

import java.util.Map;

/**
 * A use of an annotation on a {@link Declaration}.
 *
 * @author Areg Iazychian
 * @since 4.2.0
 */
public interface AnnotationReference {

	/**
	 * Return the declaration of the annotation.
	 * @return the annotation type
	 */
	TypeDeclaration getType();

	/**
	 * Return the values of the attributes that have been explicitly set. An enum constant
	 * is represented by its name and an array by a {@link java.util.List}.
	 * @return a map of attribute names to values
	 */
	Map<String, Object> getValues();

}
