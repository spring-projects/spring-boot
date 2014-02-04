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

import groovy.lang.Grab;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ImportNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;
import org.springframework.boot.cli.compiler.dependencies.ArtifactCoordinatesResolver;

/**
 * {@link ASTTransformation} to resolve {@link Grab} artifact coordinates.
 * 
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
public class ResolveDependencyCoordinatesTransformation implements ASTTransformation {

	private static final Set<String> GRAB_ANNOTATION_NAMES = Collections
			.unmodifiableSet(new HashSet<String>(Arrays.asList(Grab.class.getName(),
					Grab.class.getSimpleName())));

	private final ArtifactCoordinatesResolver coordinatesResolver;

	public ResolveDependencyCoordinatesTransformation(
			ArtifactCoordinatesResolver coordinatesResolver) {
		this.coordinatesResolver = coordinatesResolver;
	}

	@Override
	public void visit(ASTNode[] nodes, SourceUnit source) {
		ClassVisitor classVisitor = new ClassVisitor(source);
		for (ASTNode node : nodes) {
			if (node instanceof ModuleNode) {
				ModuleNode module = (ModuleNode) node;

				visitAnnotatedNode(module.getPackage());

				for (ImportNode importNode : module.getImports()) {
					visitAnnotatedNode(importNode);
				}
				for (ImportNode importNode : module.getStarImports()) {
					visitAnnotatedNode(importNode);
				}
				for (Map.Entry<String, ImportNode> entry : module.getStaticImports()
						.entrySet()) {
					visitAnnotatedNode(entry.getValue());
				}
				for (Map.Entry<String, ImportNode> entry : module.getStaticStarImports()
						.entrySet()) {
					visitAnnotatedNode(entry.getValue());
				}

				for (ClassNode classNode : module.getClasses()) {
					visitAnnotatedNode(classNode);
					classNode.visitContents(classVisitor);
				}
			}
		}
	}

	private void visitAnnotatedNode(AnnotatedNode annotatedNode) {
		if (annotatedNode != null) {
			for (AnnotationNode annotationNode : annotatedNode.getAnnotations()) {
				if (GRAB_ANNOTATION_NAMES.contains(annotationNode.getClassNode()
						.getName())) {
					transformGrabAnnotation(annotationNode);
				}
			}
		}
	}

	private void transformGrabAnnotation(AnnotationNode grabAnnotation) {
		grabAnnotation.setMember("initClass", new ConstantExpression(false));
		String value = getValue(grabAnnotation);
		if (value != null && !isConvenienceForm(value)) {
			applyGroupAndVersion(grabAnnotation, value);
		}
	}

	private String getValue(AnnotationNode annotation) {
		Expression expression = annotation.getMember("value");
		if (expression instanceof ConstantExpression) {
			Object value = ((ConstantExpression) expression).getValue();
			return (value instanceof String ? (String) value : null);
		}
		return null;
	}

	private boolean isConvenienceForm(String value) {
		return value.contains(":") || value.contains("#");
	}

	private void applyGroupAndVersion(AnnotationNode annotation, String module) {
		if (module != null) {
			setMember(annotation, "module", module);
		}
		else {
			Expression expression = annotation.getMembers().get("module");
			module = (String) ((ConstantExpression) expression).getValue();
		}
		if (annotation.getMember("group") == null) {
			setMember(annotation, "group", this.coordinatesResolver.getGroupId(module));
		}
		if (annotation.getMember("version") == null) {
			setMember(annotation, "version", this.coordinatesResolver.getVersion(module));
		}
	}

	private void setMember(AnnotationNode annotation, String name, String value) {
		ConstantExpression expression = new ConstantExpression(value);
		annotation.setMember(name, expression);
	}

	private class ClassVisitor extends ClassCodeVisitorSupport {

		private final SourceUnit source;

		public ClassVisitor(SourceUnit source) {
			this.source = source;
		}

		@Override
		protected SourceUnit getSourceUnit() {
			return this.source;
		}

		@Override
		public void visitAnnotations(AnnotatedNode node) {
			visitAnnotatedNode(node);
		}

	}
}
