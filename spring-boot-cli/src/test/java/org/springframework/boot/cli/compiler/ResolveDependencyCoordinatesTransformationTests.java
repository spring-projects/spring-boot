/*
 * Copyright 2012-2014 the original author or authors.
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

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ConstructorNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.PackageNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.io.ReaderSource;
import org.codehaus.groovy.transform.ASTTransformation;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.cli.compiler.dependencies.ArtifactCoordinatesResolver;
import org.springframework.boot.cli.compiler.grape.DependencyResolutionContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ResolveDependencyCoordinatesTransformation}
 * 
 * @author Andy Wilkinson
 */
public final class ResolveDependencyCoordinatesTransformationTests {

	private final SourceUnit sourceUnit = new SourceUnit((String) null,
			(ReaderSource) null, null, null, null);

	private final ModuleNode moduleNode = new ModuleNode(this.sourceUnit);

	private final AnnotationNode grabAnnotation = createGrabAnnotation();

	private final ArtifactCoordinatesResolver coordinatesResolver = mock(ArtifactCoordinatesResolver.class);

	private final DependencyResolutionContext resolutionContext = new DependencyResolutionContext(
			this.coordinatesResolver);

	private final ASTTransformation transformation = new ResolveDependencyCoordinatesTransformation(
			this.resolutionContext);

	@Before
	public void setupExpectations() {
		when(this.coordinatesResolver.getGroupId("spring-core")).thenReturn(
				"org.springframework");
		when(this.coordinatesResolver.getVersion("spring-core")).thenReturn("4.0.0.RC1");
	}

	@Test
	public void transformationOfAnnotationOnImport() {
		this.moduleNode.addImport(null, null, Arrays.asList(this.grabAnnotation));
		assertGrabAnnotationHasBeenTransformation();
	}

	@Test
	public void transformationOfAnnotationOnStarImport() {
		this.moduleNode.addStarImport("org.springframework.util",
				Arrays.asList(this.grabAnnotation));

		assertGrabAnnotationHasBeenTransformation();
	}

	@Test
	public void transformationOfAnnotationOnStaticImport() {
		this.moduleNode.addStaticImport(null, null, null,
				Arrays.asList(this.grabAnnotation));

		assertGrabAnnotationHasBeenTransformation();
	}

	@Test
	public void transformationOfAnnotationOnStaticStarImport() {
		this.moduleNode.addStaticStarImport(null, null,
				Arrays.asList(this.grabAnnotation));

		assertGrabAnnotationHasBeenTransformation();
	}

	@Test
	public void transformationOfAnnotationOnPackage() {
		PackageNode packageNode = new PackageNode("test");
		packageNode.addAnnotation(this.grabAnnotation);
		this.moduleNode.setPackage(packageNode);

		assertGrabAnnotationHasBeenTransformation();
	}

	@Test
	public void transformationOfAnnotationOnClass() {
		ClassNode classNode = new ClassNode("Test", 0, new ClassNode(Object.class));
		classNode.addAnnotation(this.grabAnnotation);
		this.moduleNode.addClass(classNode);

		assertGrabAnnotationHasBeenTransformation();
	}

	@Test
	public void transformationOfAnnotationOnAnnotation() {
	}

	@Test
	public void transformationOfAnnotationOnField() {
		ClassNode classNode = new ClassNode("Test", 0, new ClassNode(Object.class));
		this.moduleNode.addClass(classNode);

		FieldNode fieldNode = new FieldNode("test", 0, new ClassNode(Object.class),
				classNode, null);
		classNode.addField(fieldNode);

		fieldNode.addAnnotation(this.grabAnnotation);

		assertGrabAnnotationHasBeenTransformation();
	}

	@Test
	public void transformationOfAnnotationOnConstructor() {
		ClassNode classNode = new ClassNode("Test", 0, new ClassNode(Object.class));
		this.moduleNode.addClass(classNode);

		ConstructorNode constructorNode = new ConstructorNode(0, null);
		constructorNode.addAnnotation(this.grabAnnotation);
		classNode.addMethod(constructorNode);

		assertGrabAnnotationHasBeenTransformation();
	}

	@Test
	public void transformationOfAnnotationOnMethod() {
		ClassNode classNode = new ClassNode("Test", 0, new ClassNode(Object.class));
		this.moduleNode.addClass(classNode);

		MethodNode methodNode = new MethodNode("test", 0, new ClassNode(Void.class),
				new Parameter[0], new ClassNode[0], null);
		methodNode.addAnnotation(this.grabAnnotation);
		classNode.addMethod(methodNode);

		assertGrabAnnotationHasBeenTransformation();
	}

	@Test
	public void transformationOfAnnotationOnMethodParameter() {
		ClassNode classNode = new ClassNode("Test", 0, new ClassNode(Object.class));
		this.moduleNode.addClass(classNode);

		Parameter parameter = new Parameter(new ClassNode(Object.class), "test");
		parameter.addAnnotation(this.grabAnnotation);

		MethodNode methodNode = new MethodNode("test", 0, new ClassNode(Void.class),
				new Parameter[] { parameter }, new ClassNode[0], null);
		classNode.addMethod(methodNode);

		assertGrabAnnotationHasBeenTransformation();
	}

	@Test
	public void transformationOfAnnotationOnLocalVariable() {
		ClassNode classNode = new ClassNode("Test", 0, new ClassNode(Object.class));
		this.moduleNode.addClass(classNode);

		DeclarationExpression declarationExpression = new DeclarationExpression(
				new VariableExpression("test"), null, new ConstantExpression("test"));
		declarationExpression.addAnnotation(this.grabAnnotation);

		BlockStatement code = new BlockStatement(
				Arrays.asList((Statement) new ExpressionStatement(declarationExpression)),
				new VariableScope());

		MethodNode methodNode = new MethodNode("test", 0, new ClassNode(Void.class),
				new Parameter[0], new ClassNode[0], code);

		classNode.addMethod(methodNode);

		assertGrabAnnotationHasBeenTransformation();
	}

	private AnnotationNode createGrabAnnotation() {
		ClassNode classNode = new ClassNode(Grab.class);
		AnnotationNode annotationNode = new AnnotationNode(classNode);
		annotationNode.addMember("value", new ConstantExpression("spring-core"));
		return annotationNode;
	}

	private void assertGrabAnnotationHasBeenTransformation() {
		this.transformation.visit(new ASTNode[] { this.moduleNode }, this.sourceUnit);

		assertEquals("org.springframework", getGrabAnnotationMemberAsString("group"));
		assertEquals("spring-core", getGrabAnnotationMemberAsString("module"));
		assertEquals("4.0.0.RC1", getGrabAnnotationMemberAsString("version"));
	}

	private Object getGrabAnnotationMemberAsString(String memberName) {
		Expression expression = this.grabAnnotation.getMember(memberName);
		if (expression instanceof ConstantExpression) {
			return ((ConstantExpression) expression).getValue();
		}
		else if (expression == null) {
			return null;
		}
		else {
			throw new IllegalStateException("Member '" + memberName
					+ "' is not a ConstantExpression");
		}
	}

}
