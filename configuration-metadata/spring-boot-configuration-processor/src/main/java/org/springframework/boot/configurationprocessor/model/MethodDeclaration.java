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
 * A {@link Declaration} of a method or of a constructor.
 *
 * @author Areg Iazychian
 * @since 4.2.0
 */
public interface MethodDeclaration extends Declaration {

	/**
	 * Return the parameters of this method.
	 * @return the parameters
	 */
	List<? extends VariableDeclaration> getParameters();

	/**
	 * Return the type that this method returns.
	 * @return the return type
	 */
	TypeReference getReturnType();

	/**
	 * Return the signature of this method, made of its name and of the types of its
	 * parameters, for instance {@code setName(java.lang.String)}.
	 * @return the signature
	 */
	String getSignature();

	/**
	 * Return whether this method is public.
	 * @return if the method is public
	 */
	boolean isPublic();

	/**
	 * Return whether this method is private.
	 * @return if the method is private
	 */
	boolean isPrivate();

	/**
	 * Return whether this method is abstract.
	 * @return if the method is abstract
	 */
	boolean isAbstract();

	/**
	 * Return whether this method is static.
	 * @return if the method is static
	 */
	boolean isStatic();

}
