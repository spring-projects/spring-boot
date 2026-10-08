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

import java.util.List;

/**
 * A declaration in the source code that is being processed, such as a type, a method or a
 * field.
 *
 * @author Areg Iazychian
 * @since 4.2.0
 */
public interface Declaration {

	/**
	 * Return the simple name of this declaration.
	 * @return the simple name
	 */
	String getName();

	/**
	 * Return the type that encloses this declaration.
	 * @return the enclosing type or {@code null} if this declaration is not enclosed by a
	 * type
	 */
	TypeDeclaration getEnclosingType();

	/**
	 * Return the annotations that are directly present on this declaration.
	 * @return the annotations
	 */
	List<? extends AnnotationReference> getAnnotations();

	/**
	 * Return whether this declaration has been deprecated by the language.
	 * @return if the declaration is deprecated
	 */
	boolean isDeprecated();

	/**
	 * Return the text of the documentation comment of this declaration.
	 * @return the documentation comment or {@code null}
	 */
	String getDocComment();

}
