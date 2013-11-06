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

package org.springframework.boot.cli.compiler.transformation;

import groovy.lang.GrabResolver;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;

/**
 * {@link ASTTransformation} to apply Grab resolver auto-configuration, thereby
 * configuring the repositories available for dependency resolution.
 * 
 * @author Andy Wilkinson
 */
public final class GrabResolversAutoConfigurationTransformation implements
		ASTTransformation {

	@Override
	public void visit(ASTNode[] nodes, SourceUnit source) {
		if (!Boolean.getBoolean("disableSpringSnapshotRepos")) {
			for (ASTNode node : nodes) {
				if (node instanceof ModuleNode) {
					ModuleNode module = (ModuleNode) node;
					ClassNode classNode = module.getClasses().get(0);
					classNode.addAnnotation(createGrabResolverAnnotation(
							"spring-snapshot", "http://repo.spring.io/snapshot"));
					classNode.addAnnotation(createGrabResolverAnnotation(
							"spring-milestone", "http://repo.spring.io/milestone"));
				}
			}
		}
	}

	private AnnotationNode createGrabResolverAnnotation(String name, String url) {
		AnnotationNode resolverAnnotation = new AnnotationNode(new ClassNode(
				GrabResolver.class));
		resolverAnnotation.addMember("name", new ConstantExpression(name));
		resolverAnnotation.addMember("root", new ConstantExpression(url));
		return resolverAnnotation;
	}
}
