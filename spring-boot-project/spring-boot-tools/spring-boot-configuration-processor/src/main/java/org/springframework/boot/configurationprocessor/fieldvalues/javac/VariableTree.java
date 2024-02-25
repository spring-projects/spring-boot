/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.configurationprocessor.fieldvalues.javac;

import java.util.Collections;
import java.util.Set;

import javax.lang.model.element.Modifier;

/**
 * Reflection based access to {@code com.sun.source.tree.VariableTree}.
 *
 * @author Phillip Webb
 */
class VariableTree extends ReflectionWrapper {

	/**
	 * Constructs a new VariableTree object with the specified instance.
	 * @param instance the instance of the object
	 */
	VariableTree(Object instance) {
		super("com.sun.source.tree.VariableTree", instance);
	}

	/**
	 * Retrieves the name of the instance.
	 * @return the name of the instance
	 * @throws Exception if an error occurs while retrieving the name
	 */
	String getName() throws Exception {
		return findMethod("getName").invoke(getInstance()).toString();
	}

	/**
	 * Retrieves the type of the VariableTree instance.
	 * @return the type of the VariableTree instance
	 * @throws Exception if an error occurs while retrieving the type
	 */
	String getType() throws Exception {
		return findMethod("getType").invoke(getInstance()).toString();
	}

	/**
	 * Retrieves the initializer expression of the VariableTree instance.
	 * @return an ExpressionTree representing the initializer expression, or null if no
	 * initializer is found
	 * @throws Exception if an error occurs while retrieving the initializer
	 */
	ExpressionTree getInitializer() throws Exception {
		Object instance = findMethod("getInitializer").invoke(getInstance());
		return (instance != null) ? new ExpressionTree(instance) : null;
	}

	/**
	 * Retrieves the set of modifier flags associated with this VariableTree instance.
	 * @return the set of modifier flags
	 * @throws Exception if an error occurs while retrieving the modifier flags
	 */
	@SuppressWarnings("unchecked")
	Set<Modifier> getModifierFlags() throws Exception {
		Object modifiers = findMethod("getModifiers").invoke(getInstance());
		if (modifiers == null) {
			return Collections.emptySet();
		}
		return (Set<Modifier>) findMethod(findClass("com.sun.source.tree.ModifiersTree"), "getFlags").invoke(modifiers);
	}

}
