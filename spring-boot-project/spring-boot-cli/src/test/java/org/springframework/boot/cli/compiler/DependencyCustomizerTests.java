/*
 * Copyright 2012-2017 the original author or authors.
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

import java.util.List;

import groovy.lang.Grab;
import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.control.SourceUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.cli.compiler.dependencies.ArtifactCoordinatesResolver;
import org.springframework.boot.cli.compiler.grape.DependencyResolutionContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link DependencyCustomizer}
 *
 * @author Andy Wilkinson
 */
public class DependencyCustomizerTests {

	private final ModuleNode moduleNode = new ModuleNode((SourceUnit) null);

	private final ClassNode classNode = new ClassNode(DependencyCustomizerTests.class);

	@Mock
	private ArtifactCoordinatesResolver resolver;

	private DependencyCustomizer dependencyCustomizer;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		given(this.resolver.getGroupId("spring-boot-starter-logging"))
				.willReturn("org.springframework.boot");
		given(this.resolver.getArtifactId("spring-boot-starter-logging"))
				.willReturn("spring-boot-starter-logging");
		this.moduleNode.addClass(this.classNode);
		this.dependencyCustomizer = new DependencyCustomizer(
				new GroovyClassLoader(getClass().getClassLoader()), this.moduleNode,
				new DependencyResolutionContext() {

					@Override
					public ArtifactCoordinatesResolver getArtifactCoordinatesResolver() {
						return DependencyCustomizerTests.this.resolver;
					}

				});
	}

	@Test
	public void basicAdd() {
		this.dependencyCustomizer.add("spring-boot-starter-logging");
		List<AnnotationNode> grabAnnotations = this.classNode
				.getAnnotations(new ClassNode(Grab.class));
		assertThat(grabAnnotations).hasSize(1);
		AnnotationNode annotationNode = grabAnnotations.get(0);
		assertGrabAnnotation(annotationNode, "org.springframework.boot",
				"spring-boot-starter-logging", "1.2.3", null, null, true);
	}

	@Test
	public void nonTransitiveAdd() {
		this.dependencyCustomizer.add("spring-boot-starter-logging", false);
		List<AnnotationNode> grabAnnotations = this.classNode
				.getAnnotations(new ClassNode(Grab.class));
		assertThat(grabAnnotations).hasSize(1);
		AnnotationNode annotationNode = grabAnnotations.get(0);
		assertGrabAnnotation(annotationNode, "org.springframework.boot",
				"spring-boot-starter-logging", "1.2.3", null, null, false);
	}

	@Test
	public void fullyCustomized() {
		this.dependencyCustomizer.add("spring-boot-starter-logging", "my-classifier",
				"my-type", false);
		List<AnnotationNode> grabAnnotations = this.classNode
				.getAnnotations(new ClassNode(Grab.class));
		assertThat(grabAnnotations).hasSize(1);
		AnnotationNode annotationNode = grabAnnotations.get(0);
		assertGrabAnnotation(annotationNode, "org.springframework.boot",
				"spring-boot-starter-logging", "1.2.3", "my-classifier", "my-type",
				false);
	}

	@Test
	public void anyMissingClassesWithMissingClassesPerformsAdd() {
		this.dependencyCustomizer.ifAnyMissingClasses("does.not.Exist")
				.add("spring-boot-starter-logging");
		assertThat(this.classNode.getAnnotations(new ClassNode(Grab.class))).hasSize(1);
	}

	@Test
	public void anyMissingClassesWithMixtureOfClassesPerformsAdd() {
		this.dependencyCustomizer
				.ifAnyMissingClasses(getClass().getName(), "does.not.Exist")
				.add("spring-boot-starter-logging");
		assertThat(this.classNode.getAnnotations(new ClassNode(Grab.class))).hasSize(1);
	}

	@Test
	public void anyMissingClassesWithNoMissingClassesDoesNotPerformAdd() {
		this.dependencyCustomizer.ifAnyMissingClasses(getClass().getName())
				.add("spring-boot-starter-logging");
		assertThat(this.classNode.getAnnotations(new ClassNode(Grab.class))).isEmpty();
	}

	@Test
	public void allMissingClassesWithNoMissingClassesDoesNotPerformAdd() {
		this.dependencyCustomizer.ifAllMissingClasses(getClass().getName())
				.add("spring-boot-starter-logging");
		assertThat(this.classNode.getAnnotations(new ClassNode(Grab.class))).isEmpty();
	}

	@Test
	public void allMissingClassesWithMixtureOfClassesDoesNotPerformAdd() {
		this.dependencyCustomizer
				.ifAllMissingClasses(getClass().getName(), "does.not.Exist")
				.add("spring-boot-starter-logging");
		assertThat(this.classNode.getAnnotations(new ClassNode(Grab.class))).isEmpty();
	}

	@Test
	public void allMissingClassesWithAllClassesMissingPerformsAdd() {
		this.dependencyCustomizer
				.ifAllMissingClasses("does.not.Exist", "does.not.exist.Either")
				.add("spring-boot-starter-logging");
		assertThat(this.classNode.getAnnotations(new ClassNode(Grab.class))).hasSize(1);
	}

	private void assertGrabAnnotation(AnnotationNode annotationNode, String group,
			String module, String version, String classifier, String type,
			boolean transitive) {
		assertThat(getMemberValue(annotationNode, "group")).isEqualTo(group);
		assertThat(getMemberValue(annotationNode, "module")).isEqualTo(module);
		if (type == null) {
			assertThat(annotationNode.getMember("type")).isNull();
		}
		else {
			assertThat(getMemberValue(annotationNode, "type")).isEqualTo(type);
		}
		if (classifier == null) {
			assertThat(annotationNode.getMember("classifier")).isNull();
		}
		else {
			assertThat(getMemberValue(annotationNode, "classifier"))
					.isEqualTo(classifier);
		}
		assertThat(getMemberValue(annotationNode, "transitive")).isEqualTo(transitive);
	}

	private Object getMemberValue(AnnotationNode annotationNode, String member) {
		return ((ConstantExpression) annotationNode.getMember(member)).getValue();
	}

}
