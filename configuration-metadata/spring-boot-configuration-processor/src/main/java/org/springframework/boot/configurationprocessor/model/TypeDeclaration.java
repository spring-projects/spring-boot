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
import java.util.Map;

/**
 * A {@link Declaration} of a class, interface, enum, record or annotation type.
 *
 * @author Areg Iazychian
 * @since 4.2.0
 */
public interface TypeDeclaration extends Declaration {

	/**
	 * Return the fully qualified name of this type, suitable for a call to
	 * {@link Class#forName(String)}.
	 * @return the fully qualified name
	 */
	String getQualifiedName();

	/**
	 * Return the superclass of this type.
	 * @return the superclass or {@code null} if this type has none
	 */
	TypeDeclaration getSuperclass();

	/**
	 * Return the type that this declaration defines.
	 * @return the declared type
	 */
	TypeReference asType();

	/**
	 * Return whether this type is an enum.
	 * @return if the type is an enum
	 */
	boolean isEnum();

	/**
	 * Return whether this type is a record.
	 * @return if the type is a record
	 */
	boolean isRecord();

	/**
	 * Return whether the source of this type is available. The documentation of a type
	 * that has no source cannot be determined.
	 * @return if the source of the type is available
	 */
	boolean hasSource();

	/**
	 * Return the constructors that this type declares.
	 * @return the constructors
	 */
	List<? extends MethodDeclaration> getConstructors();

	/**
	 * Return the methods that this type declares, as they are exposed to Java. Inherited
	 * methods are not included.
	 * @return the methods
	 */
	List<? extends MethodDeclaration> getMethods();

	/**
	 * Return the fields that this type declares. Inherited fields are not included.
	 * @return the fields
	 */
	List<? extends VariableDeclaration> getFields();

	/**
	 * Return the record components that this type declares.
	 * @return the record components
	 */
	List<? extends VariableDeclaration> getRecordComponents();

	/**
	 * Return the values that the fields declared by this type are initialized with.
	 * @return a map of field names to values, with no entry for fields whose value cannot
	 * be determined
	 */
	Map<String, Object> getFieldValues();

}
