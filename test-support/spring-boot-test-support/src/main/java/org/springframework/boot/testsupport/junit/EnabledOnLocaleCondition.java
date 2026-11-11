/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.junit;

import java.util.Locale;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * An implementation of {@link ExecutionCondition} that conditionally enables or disables
 * the execution of a test method or class based on the specified locale attributes.
 *
 * @author Dmytro Nosan
 */
class EnabledOnLocaleCondition implements ExecutionCondition {

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		return AnnotationSupport.findAnnotation(context.getElement(), EnabledOnLocale.class)
			.map(this::evaluate)
			.orElseGet(() -> ConditionEvaluationResult.enabled("No @EnabledOnLocale annotation found"));
	}

	private ConditionEvaluationResult evaluate(EnabledOnLocale annotation) {
		Locale locale = Locale.getDefault();
		String language = locale.getLanguage();
		if (!annotation.language().equals(language)) {
			return ConditionEvaluationResult.disabled("Disabled on language: " + language);
		}
		return ConditionEvaluationResult.enabled("Enabled on language: " + language);
	}

}
