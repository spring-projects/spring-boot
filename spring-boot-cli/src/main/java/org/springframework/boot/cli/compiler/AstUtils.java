/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.cli.compiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.springframework.util.PatternMatchUtils;

/**
 * General purpose AST utilities.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Greg Turnquist
 */
public abstract class AstUtils {

	/**
	 * Determine if a {@link ClassNode} has one or more of the specified annotations on
	 * the class or any of its methods. N.B. the type names are not normally fully
	 * qualified.
	 */
	public static boolean hasAtLeastOneAnnotation(ClassNode node, String... annotations) {
		if (hasAtLeastOneAnnotation((AnnotatedNode) node, annotations)) {
			return true;
		}
		for (MethodNode method : node.getMethods()) {
			if (hasAtLeastOneAnnotation(method, annotations)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine if an {@link AnnotatedNode} has one or more of the specified annotations.
	 * N.B. the annotation type names are not normally fully qualified.
	 */
	public static boolean hasAtLeastOneAnnotation(AnnotatedNode node,
			String... annotations) {
		for (AnnotationNode annotationNode : node.getAnnotations()) {
			for (String annotation : annotations) {
				if (PatternMatchUtils.simpleMatch(annotation, annotationNode
						.getClassNode().getName())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Determine if a {@link ClassNode} has one or more fields of the specified types or
	 * method returning one or more of the specified types. N.B. the type names are not
	 * normally fully qualified.
	 */
	public static boolean hasAtLeastOneFieldOrMethod(ClassNode node, String... types) {
		Set<String> typesSet = new HashSet<String>(Arrays.asList(types));
		for (FieldNode field : node.getFields()) {
			if (typesSet.contains(field.getType().getName())) {
				return true;
			}
		}
		for (MethodNode method : node.getMethods()) {
			if (typesSet.contains(method.getReturnType().getName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine if a {@link ClassNode} subclasses any of the specified types N.B. the
	 * type names are not normally fully qualified.
	 */
	public static boolean subclasses(ClassNode node, String... types) {
		for (String type : types) {
			if (node.getSuperClass().getName().equals(type)) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasAtLeastOneInterface(ClassNode classNode, String... types) {
		Set<String> typesSet = new HashSet<String>(Arrays.asList(types));
		for (ClassNode inter : classNode.getInterfaces()) {
			if (typesSet.contains(inter.getName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Extract a top-level <code>name</code> closure from inside this block if there is
	 * one. Removes it from the block at the same time.
	 * @param block a block statement (class definition)
	 * @return a beans Closure if one can be found, null otherwise
	 */
	public static ClosureExpression getClosure(BlockStatement block, String name,
			boolean remove) {

		for (Statement statement : new ArrayList<Statement>(block.getStatements())) {
			if (statement instanceof ExpressionStatement) {
				Expression expression = ((ExpressionStatement) statement).getExpression();
				if (expression instanceof MethodCallExpression) {
					MethodCallExpression call = (MethodCallExpression) expression;
					Expression methodCall = call.getMethod();
					if (methodCall instanceof ConstantExpression) {
						ConstantExpression method = (ConstantExpression) methodCall;
						if (name.equals(method.getValue())) {
							ArgumentListExpression arguments = (ArgumentListExpression) call
									.getArguments();
							if (remove) {
								block.getStatements().remove(statement);
							}
							ClosureExpression closure = (ClosureExpression) arguments
									.getExpression(0);
							return closure;
						}
					}
				}
			}
		}
		return null;
	}

	public static ClosureExpression getClosure(BlockStatement block, String name) {
		return getClosure(block, name, false);
	}

}
