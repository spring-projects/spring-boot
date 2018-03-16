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

package org.springframework.boot.cli.compiler;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import groovy.grape.Grape;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.building.UrlModelSource;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.control.messages.Message;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.transform.ASTTransformation;

import org.springframework.boot.cli.compiler.dependencies.MavenModelDependencyManagement;
import org.springframework.boot.cli.compiler.grape.DependencyResolutionContext;
import org.springframework.boot.groovy.DependencyManagementBom;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * {@link ASTTransformation} for processing {@link DependencyManagementBom} annotations.
 *
 * @author Andy Wilkinson
 * @since 1.3.0
 */
@Order(DependencyManagementBomTransformation.ORDER)
public class DependencyManagementBomTransformation
		extends AnnotatedNodeASTTransformation {

	/**
	 * The order of the transformation.
	 */
	public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 100;

	private static final Set<String> DEPENDENCY_MANAGEMENT_BOM_ANNOTATION_NAMES = Collections
			.unmodifiableSet(
					new HashSet<>(Arrays.asList(DependencyManagementBom.class.getName(),
							DependencyManagementBom.class.getSimpleName())));

	private final DependencyResolutionContext resolutionContext;

	public DependencyManagementBomTransformation(
			DependencyResolutionContext resolutionContext) {
		super(DEPENDENCY_MANAGEMENT_BOM_ANNOTATION_NAMES, true);
		this.resolutionContext = resolutionContext;
	}

	@Override
	protected void processAnnotationNodes(List<AnnotationNode> annotationNodes) {
		if (!annotationNodes.isEmpty()) {
			if (annotationNodes.size() > 1) {
				for (AnnotationNode annotationNode : annotationNodes) {
					handleDuplicateDependencyManagementBomAnnotation(annotationNode);
				}
			}
			else {
				processDependencyManagementBomAnnotation(annotationNodes.get(0));
			}
		}
	}

	private void processDependencyManagementBomAnnotation(AnnotationNode annotationNode) {
		Expression valueExpression = annotationNode.getMember("value");
		List<Map<String, String>> bomDependencies = createDependencyMaps(valueExpression);
		updateDependencyResolutionContext(bomDependencies);
	}

	private List<Map<String, String>> createDependencyMaps(Expression valueExpression) {
		Map<String, String> dependency = null;
		List<ConstantExpression> constantExpressions = getConstantExpressions(
				valueExpression);
		List<Map<String, String>> dependencies = new ArrayList<>(
				constantExpressions.size());
		for (ConstantExpression expression : constantExpressions) {
			Object value = expression.getValue();
			if (value instanceof String) {
				String[] components = ((String) expression.getValue()).split(":");
				if (components.length == 3) {
					dependency = new HashMap<>();
					dependency.put("group", components[0]);
					dependency.put("module", components[1]);
					dependency.put("version", components[2]);
					dependency.put("type", "pom");
					dependencies.add(dependency);
				}
				else {
					handleMalformedDependency(expression);
				}
			}
		}
		return dependencies;
	}

	private List<ConstantExpression> getConstantExpressions(Expression valueExpression) {
		if (valueExpression instanceof ListExpression) {
			return getConstantExpressions((ListExpression) valueExpression);
		}
		if (valueExpression instanceof ConstantExpression
				&& ((ConstantExpression) valueExpression).getValue() instanceof String) {
			return Arrays.asList((ConstantExpression) valueExpression);
		}
		reportError("@DependencyManagementBom requires an inline constant that is a "
				+ "string or a string array", valueExpression);
		return Collections.emptyList();
	}

	private List<ConstantExpression> getConstantExpressions(
			ListExpression valueExpression) {
		List<ConstantExpression> expressions = new ArrayList<>();
		for (Expression expression : valueExpression.getExpressions()) {
			if (expression instanceof ConstantExpression
					&& ((ConstantExpression) expression).getValue() instanceof String) {
				expressions.add((ConstantExpression) expression);
			}
			else {
				reportError(
						"Each entry in the array must be an " + "inline string constant",
						expression);
			}
		}
		return expressions;
	}

	private void handleMalformedDependency(Expression expression) {
		Message message = createSyntaxErrorMessage(
				String.format(
						"The string must be of the form \"group:module:version\"%n"),
				expression);
		getSourceUnit().getErrorCollector().addErrorAndContinue(message);
	}

	private void updateDependencyResolutionContext(
			List<Map<String, String>> bomDependencies) {
		URI[] uris = Grape.getInstance().resolve(null,
				bomDependencies.toArray(new Map[0]));
		DefaultModelBuilder modelBuilder = new DefaultModelBuilderFactory().newInstance();
		for (URI uri : uris) {
			try {
				DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
				request.setModelResolver(new GrapeModelResolver());
				request.setModelSource(new UrlModelSource(uri.toURL()));
				request.setSystemProperties(System.getProperties());
				Model model = modelBuilder.build(request).getEffectiveModel();
				this.resolutionContext.addDependencyManagement(
						new MavenModelDependencyManagement(model));
			}
			catch (Exception ex) {
				throw new IllegalStateException("Failed to build model for '" + uri
						+ "'. Is it a valid Maven bom?", ex);
			}
		}
	}

	private void handleDuplicateDependencyManagementBomAnnotation(
			AnnotationNode annotationNode) {
		Message message = createSyntaxErrorMessage(
				"Duplicate @DependencyManagementBom annotation. It must be declared at most once.",
				annotationNode);
		getSourceUnit().getErrorCollector().addErrorAndContinue(message);
	}

	private void reportError(String message, ASTNode node) {
		getSourceUnit().getErrorCollector()
				.addErrorAndContinue(createSyntaxErrorMessage(message, node));
	}

	private Message createSyntaxErrorMessage(String message, ASTNode node) {
		return new SyntaxErrorMessage(
				new SyntaxException(message, node.getLineNumber(), node.getColumnNumber(),
						node.getLastLineNumber(), node.getLastColumnNumber()),
				getSourceUnit());
	}

	private static class GrapeModelResolver implements ModelResolver {

		@Override
		public ModelSource resolveModel(String groupId, String artifactId, String version)
				throws UnresolvableModelException {
			Map<String, String> dependency = new HashMap<>();
			dependency.put("group", groupId);
			dependency.put("module", artifactId);
			dependency.put("version", version);
			dependency.put("type", "pom");
			try {
				return new UrlModelSource(
						Grape.getInstance().resolve(null, dependency)[0].toURL());
			}
			catch (MalformedURLException e) {
				throw new UnresolvableModelException(e.getMessage(), groupId, artifactId,
						version);
			}
		}

		@Override
		public void addRepository(Repository repository)
				throws InvalidRepositoryException {
		}

		@Override
		public ModelResolver newCopy() {
			return this;
		}

	}

}
