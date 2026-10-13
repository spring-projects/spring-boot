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

/**
 * A use of a type, for instance as the type of a field or as the return type of a method.
 *
 * @author Areg Iazychian
 * @since 4.2.0
 */
public interface TypeReference {

	/**
	 * Return whether this is the {@code void} pseudo-type.
	 * @return if the type is {@code void}
	 */
	boolean isVoid();

	/**
	 * Return whether this type is a primitive type.
	 * @return if the type is a primitive
	 */
	boolean isPrimitive();

	/**
	 * Return whether this type is a type variable.
	 * @return if the type is a type variable
	 */
	boolean isTypeVariable();

	/**
	 * Return whether this type accepts {@code null}.
	 * @return if the type is nullable
	 */
	boolean isNullable();

	/**
	 * Return whether this type is a {@link java.util.Collection} or a
	 * {@link java.util.Map}.
	 * @return if the type is a collection or a map
	 */
	boolean isCollectionOrMap();

	/**
	 * Return whether this type is the same type as the given type.
	 * @param other the type to compare with
	 * @return if the types are the same
	 */
	boolean isSameType(TypeReference other);

	/**
	 * Return the declaration of this type.
	 * @return the declaration or {@code null} if this type is not a class, interface,
	 * enum, record or annotation type
	 */
	TypeDeclaration getDeclaration();

	/**
	 * Return the type of the elements of this type if it is a
	 * {@link java.util.Collection}.
	 * @return the element type or {@code null} if this type is not a collection
	 */
	TypeReference getElementType();

	/**
	 * Return the fully qualified name of this type, including all its generic
	 * information. Primitive types are named after their wrapper and type variables are
	 * resolved against the given type.
	 * @param declaringType the type in which this type is used
	 * @return the fully qualified name of the type
	 */
	String getName(TypeDeclaration declaringType);

	/**
	 * Return this type as it is written in Java.
	 * @return the Java representation of the type
	 */
	@Override
	String toString();

}
