/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationprocessor.fieldvalues.javac;

/**
 * Reflection base alternative for {@code com.sun.source.tree.TreeVisitor}.
 *
 * @author Phillip Webb
 */
interface TreeVisitor {

	void visitVariable(VariableTree variable) throws Exception;

}
