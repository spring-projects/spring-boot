/*
 * Copyright 2012-2015 the original author or authors.
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
import java.util.Collections;
import java.util.List;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.PackageNode;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.GroovyASTTransformation;

import org.springframework.boot.groovy.DependencyManagementBom;
import org.springframework.core.Ordered;

/**
 * A base class that lets plugin authors easily add additional BOMs to all apps. All the
 * dependencies in the BOM (and it's transitives) will be added to the dependency
 * management lookup, so an app can use just the artifact id (e.g. "spring-jdbc") in a
 * {@code @Grab}. To install, implement the missing methods and list the class in
 * {@code META-INF/services/org.springframework.boot.cli.compiler.SpringBootAstTransformation}
 * . The {@link #getOrder()} value needs to be before
 * {@link DependencyManagementBomTransformation#ORDER}.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
@GroovyASTTransformation(phase = CompilePhase.CONVERSION)
public abstract class GenericBomAstTransformation
		implements SpringBootAstTransformation, Ordered {

	private static ClassNode BOM = ClassHelper.make(DependencyManagementBom.class);

	@Override
	public void visit(ASTNode[] nodes, SourceUnit source) {
		for (ASTNode astNode : nodes) {
			if (astNode instanceof ModuleNode) {
				visitModule((ModuleNode) astNode, getBomModule());
			}
		}
	}

	/**
	 * The bom to be added to dependency management in compact form:
	 * <code>"&lt;groupId&gt;:&lt;artifactId&gt;:&lt;version&gt;"</code> (like in a
	 * {@code @Grab}).
	 * @return the maven co-ordinates of the BOM to add
	 */
	protected abstract String getBomModule();

	private void visitModule(ModuleNode node, String module) {
		addDependencyManagementBom(node, module);
	}

	private void addDependencyManagementBom(ModuleNode node, String module) {
		AnnotatedNode annotated = getAnnotatedNode(node);
		if (annotated != null) {
			AnnotationNode bom = getAnnotation(annotated);
			List<Expression> expressions = new ArrayList<Expression>(
					getConstantExpressions(bom.getMember("value")));
			expressions.add(new ConstantExpression(module));
			bom.setMember("value", new ListExpression(expressions));
		}
	}

	private AnnotationNode getAnnotation(AnnotatedNode annotated) {
		List<AnnotationNode> annotations = annotated.getAnnotations(BOM);
		if (!annotations.isEmpty()) {
			return annotations.get(0);
		}
		AnnotationNode annotation = new AnnotationNode(BOM);
		annotated.addAnnotation(annotation);
		return annotation;
	}

	private AnnotatedNode getAnnotatedNode(ModuleNode node) {
		PackageNode packageNode = node.getPackage();
		if (packageNode != null && !packageNode.getAnnotations(BOM).isEmpty()) {
			return packageNode;
		}
		if (!node.getClasses().isEmpty()) {
			return node.getClasses().get(0);
		}
		return packageNode;
	}

	private List<ConstantExpression> getConstantExpressions(Expression valueExpression) {
		if (valueExpression instanceof ListExpression) {
			return getConstantExpressions((ListExpression) valueExpression);
		}
		if (valueExpression instanceof ConstantExpression
				&& ((ConstantExpression) valueExpression).getValue() instanceof String) {
			return Arrays.asList((ConstantExpression) valueExpression);
		}
		return Collections.emptyList();
	}

	private List<ConstantExpression> getConstantExpressions(
			ListExpression valueExpression) {
		List<ConstantExpression> expressions = new ArrayList<ConstantExpression>();
		for (Expression expression : valueExpression.getExpressions()) {
			if (expression instanceof ConstantExpression
					&& ((ConstantExpression) expression).getValue() instanceof String) {
				expressions.add((ConstantExpression) expression);
			}
		}
		return expressions;
	}

}
