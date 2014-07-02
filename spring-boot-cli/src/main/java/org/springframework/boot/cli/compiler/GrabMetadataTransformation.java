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

import groovy.grape.Grape;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.control.messages.Message;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.transform.ASTTransformation;
import org.springframework.boot.cli.compiler.grape.DependencyResolutionContext;
import org.springframework.boot.dependency.tools.Dependencies;
import org.springframework.boot.dependency.tools.ManagedDependencies;
import org.springframework.boot.dependency.tools.PropertiesFileDependencies;
import org.springframework.boot.groovy.GrabMetadata;

/**
 * {@link ASTTransformation} for processing {@link GrabMetadata @GrabMetadata}
 *
 * @author Andy Wilkinson
 * @since 1.1.0
 */
public class GrabMetadataTransformation extends AnnotatedNodeASTTransformation {

	private static final Set<String> GRAB_METADATA_ANNOTATION_NAMES = Collections
			.unmodifiableSet(new HashSet<String>(Arrays.asList(
					GrabMetadata.class.getName(), GrabMetadata.class.getSimpleName())));

	private final DependencyResolutionContext resolutionContext;

	public GrabMetadataTransformation(DependencyResolutionContext resolutionContext) {
		super(GRAB_METADATA_ANNOTATION_NAMES);
		this.resolutionContext = resolutionContext;
	}

	@Override
	protected void processAnnotationNodes(List<AnnotationNode> annotationNodes) {
		if (!annotationNodes.isEmpty()) {
			if (annotationNodes.size() > 1) {
				for (AnnotationNode annotationNode : annotationNodes) {
					handleDuplicateGrabMetadataAnnotation(annotationNode);
				}
			}
			else {
				processGrabMetadataAnnotation(annotationNodes.get(0));
			}
		}
	}

	private void processGrabMetadataAnnotation(AnnotationNode annotationNode) {
		Expression valueExpression = annotationNode.getMember("value");
		List<Map<String, String>> metadataDependencies = createDependencyMaps(valueExpression);
		updateArtifactCoordinatesResolver(metadataDependencies);
	}

	private List<Map<String, String>> createDependencyMaps(Expression valueExpression) {
		Map<String, String> dependency = null;

		List<ConstantExpression> constantExpressions = getConstantExpressions(valueExpression);
		List<Map<String, String>> dependencies = new ArrayList<Map<String, String>>(
				constantExpressions.size());

		for (ConstantExpression expression : constantExpressions) {
			Object value = expression.getValue();
			if (value instanceof String) {
				String[] components = ((String) expression.getValue()).split(":");
				if (components.length == 3) {
					dependency = new HashMap<String, String>();
					dependency.put("group", components[0]);
					dependency.put("module", components[1]);
					dependency.put("version", components[2]);
					dependency.put("type", "properties");
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

		reportError("@GrabMetadata requires an inline constant that is a "
				+ "string or a string array", valueExpression);
		return Collections.emptyList();
	}

	private List<ConstantExpression> getConstantExpressions(ListExpression valueExpression) {
		List<ConstantExpression> expressions = new ArrayList<ConstantExpression>();
		for (Expression expression : valueExpression.getExpressions()) {
			if (expression instanceof ConstantExpression
					&& ((ConstantExpression) expression).getValue() instanceof String) {
				expressions.add((ConstantExpression) expression);
			}
			else {
				reportError("Each entry in the array must be an "
						+ "inline string constant", expression);
			}
		}
		return expressions;
	}

	private void handleMalformedDependency(Expression expression) {
		Message message = createSyntaxErrorMessage(
				"The string must be of the form \"group:module:version\"\n", expression);
		getSourceUnit().getErrorCollector().addErrorAndContinue(message);
	}

	private void updateArtifactCoordinatesResolver(
			List<Map<String, String>> metadataDependencies) {
		URI[] uris = Grape.getInstance().resolve(null,
				metadataDependencies.toArray(new Map[metadataDependencies.size()]));
		List<Dependencies> managedDependencies = new ArrayList<Dependencies>(uris.length);
		for (URI uri : uris) {
			try {
				managedDependencies.add(new PropertiesFileDependencies(uri.toURL()
						.openStream()));
			}
			catch (IOException ex) {
				throw new IllegalStateException("Failed to parse '" + uris[0]
						+ "'. Is it a valid properties file?");
			}
		}

		this.resolutionContext.setManagedDependencies(ManagedDependencies
				.get(managedDependencies));
	}

	private void handleDuplicateGrabMetadataAnnotation(AnnotationNode annotationNode) {
		Message message = createSyntaxErrorMessage(
				"Duplicate @GrabMetadata annotation. It must be declared at most once.",
				annotationNode);
		getSourceUnit().getErrorCollector().addErrorAndContinue(message);
	}

	private void reportError(String message, ASTNode node) {
		getSourceUnit().getErrorCollector().addErrorAndContinue(
				createSyntaxErrorMessage(message, node));
	}

	private Message createSyntaxErrorMessage(String message, ASTNode node) {
		return new SyntaxErrorMessage(new SyntaxException(message, node.getLineNumber(),
				node.getColumnNumber(), node.getLastLineNumber(),
				node.getLastColumnNumber()), getSourceUnit());
	}
}
