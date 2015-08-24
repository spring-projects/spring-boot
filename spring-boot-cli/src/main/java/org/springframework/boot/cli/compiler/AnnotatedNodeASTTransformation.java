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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ImportNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;

/**
 * A base class for {@link ASTTransformation AST transformations} that are solely
 * interested in {@link AnnotatedNode AnnotatedNodes}.
 *
 * @author Andy Wilkinson
 * @since 1.1.0
 */
public abstract class AnnotatedNodeASTTransformation implements ASTTransformation {

	private final Set<String> interestingAnnotationNames;

	private final boolean removeAnnotations;

	private List<AnnotationNode> annotationNodes = new ArrayList<AnnotationNode>();

	private SourceUnit sourceUnit;

	protected AnnotatedNodeASTTransformation(Set<String> interestingAnnotationNames,
			boolean removeAnnotations) {
		this.interestingAnnotationNames = interestingAnnotationNames;
		this.removeAnnotations = removeAnnotations;
	}

	@Override
	public void visit(ASTNode[] nodes, SourceUnit source) {
		this.sourceUnit = source;

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

		processAnnotationNodes(this.annotationNodes);
	}

	protected SourceUnit getSourceUnit() {
		return this.sourceUnit;
	}

	protected abstract void processAnnotationNodes(List<AnnotationNode> annotationNodes);

	private void visitAnnotatedNode(AnnotatedNode annotatedNode) {
		if (annotatedNode != null) {
			Iterator<AnnotationNode> annotationNodes = annotatedNode.getAnnotations()
					.iterator();
			while (annotationNodes.hasNext()) {
				AnnotationNode annotationNode = annotationNodes.next();
				if (this.interestingAnnotationNames.contains(annotationNode
						.getClassNode().getName())) {
					this.annotationNodes.add(annotationNode);
					if (this.removeAnnotations) {
						annotationNodes.remove();
					}
				}
			}
		}
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
