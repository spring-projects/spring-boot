/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigureprocessor;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.SupportedAnnotationTypes;

/**
 * Version of {@link AutoConfigureAnnotationProcessor} used for testing.
 *
 * @author Madhura Bhave
 * @author Scott Frederick
 */
@SupportedAnnotationTypes({ "org.springframework.boot.autoconfigureprocessor.TestConditionalOnClass",
		"org.springframework.boot.autoconfigureprocessor.TestConditionalOnBean",
		"org.springframework.boot.autoconfigureprocessor.TestConditionalOnSingleCandidate",
		"org.springframework.boot.autoconfigureprocessor.TestConditionalOnWebApplication",
		"org.springframework.boot.autoconfigureprocessor.TestAutoConfigureBefore",
		"org.springframework.boot.autoconfigureprocessor.TestAutoConfigureAfter",
		"org.springframework.boot.autoconfigureprocessor.TestAutoConfigureOrder",
		"org.springframework.boot.autoconfigureprocessor.TestAutoConfiguration" })
public class TestAutoConfigureAnnotationProcessor extends AutoConfigureAnnotationProcessor {

	public TestAutoConfigureAnnotationProcessor() {
	}

	@Override
	protected List<PropertyGenerator> getPropertyGenerators() {
		List<PropertyGenerator> generators = new ArrayList<>();
		String annotationPackage = "org.springframework.boot.autoconfigureprocessor";
		generators.add(PropertyGenerator.of(annotationPackage, "ConditionalOnClass")
			.withAnnotation("TestConditionalOnClass", new OnClassConditionValueExtractor()));
		generators.add(PropertyGenerator.of(annotationPackage, "ConditionalOnBean")
			.withAnnotation("TestConditionalOnBean", new OnBeanConditionValueExtractor()));
		generators.add(PropertyGenerator.of(annotationPackage, "ConditionalOnSingleCandidate")
			.withAnnotation("TestConditionalOnSingleCandidate", new OnBeanConditionValueExtractor()));
		generators.add(PropertyGenerator.of(annotationPackage, "ConditionalOnWebApplication")
			.withAnnotation("TestConditionalOnWebApplication", ValueExtractor.allFrom("type")));
		generators.add(PropertyGenerator.of(annotationPackage, "AutoConfigureBefore", true)
			.withAnnotation("TestAutoConfigureBefore", ValueExtractor.allFrom("value", "name"))
			.withAnnotation("TestAutoConfiguration", ValueExtractor.allFrom("before", "beforeName")));
		generators.add(PropertyGenerator.of(annotationPackage, "AutoConfigureAfter", true)
			.withAnnotation("TestAutoConfigureAfter", ValueExtractor.allFrom("value", "name"))
			.withAnnotation("TestAutoConfiguration", ValueExtractor.allFrom("after", "afterName")));
		generators.add(PropertyGenerator.of(annotationPackage, "AutoConfigureOrder")
			.withAnnotation("TestAutoConfigureOrder", ValueExtractor.allFrom("value")));
		return generators;
	}

}
