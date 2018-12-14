/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.configurationprocessor.fieldvalues.javac;

import java.util.Collections;
import java.util.Set;

import javax.lang.model.element.Modifier;

/**
 * Reflection based access to {@code com.sun.source.tree.VariableTree}.
 *
 * @author Phillip Webb
 * @since 1.2.0
 */
class VariableTree extends ReflectionWrapper {

	VariableTree(Object instance) {
		super("com.sun.source.tree.VariableTree", instance);
	}

	public String getName() throws Exception {
		return findMethod("getName").invoke(getInstance()).toString();
	}

	public String getType() throws Exception {
		return findMethod("getType").invoke(getInstance()).toString();
	}

	public ExpressionTree getInitializer() throws Exception {
		Object instance = findMethod("getInitializer").invoke(getInstance());
		return (instance != null) ? new ExpressionTree(instance) : null;
	}

	@SuppressWarnings("unchecked")
	public Set<Modifier> getModifierFlags() throws Exception {
		Object modifiers = findMethod("getModifiers").invoke(getInstance());
		if (modifiers == null) {
			return Collections.emptySet();
		}
		return (Set<Modifier>) findMethod(findClass("com.sun.source.tree.ModifiersTree"),
				"getFlags").invoke(modifiers);
	}

}
