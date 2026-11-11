/*
 * Copyright 2012-present the original author or authors.
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

	VariableTree(Object instance) {
		super("com.sun.source.tree.VariableTree", instance);
	}

	String getName() throws Exception {
		return findMethod("getName").invoke(getInstance()).toString();
	}

	String getType() throws Exception {
		return findMethod("getType").invoke(getInstance()).toString();
	}

	ExpressionTree getInitializer() throws Exception {
		Object instance = findMethod("getInitializer").invoke(getInstance());
		return (instance != null) ? new ExpressionTree(instance) : null;
	}

	@SuppressWarnings("unchecked")
	Set<Modifier> getModifierFlags() throws Exception {
		Object modifiers = findMethod("getModifiers").invoke(getInstance());
		if (modifiers == null) {
			return Collections.emptySet();
		}
		return (Set<Modifier>) findMethod(findClass("com.sun.source.tree.ModifiersTree"), "getFlags").invoke(modifiers);
	}

}
