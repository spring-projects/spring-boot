/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.diagnostics.analyzer;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.springframework.beans.factory.support.BeanDefinitionOverrideException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of failures caused by a
 * {@link BeanDefinitionOverrideException}.
 *
 * @author Andy Wilkinson
 */
class BeanDefinitionOverrideFailureAnalyzer extends AbstractFailureAnalyzer<BeanDefinitionOverrideException> {

	private static final String ACTION = "Consider renaming one of the beans or enabling "
			+ "overriding by setting spring.main.allow-bean-definition-overriding=true";

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, BeanDefinitionOverrideException cause) {
		return new FailureAnalysis(getDescription(cause), ACTION, cause);
	}

	private String getDescription(BeanDefinitionOverrideException ex) {
		StringWriter description = new StringWriter();
		PrintWriter printer = new PrintWriter(description);
		printer.printf("The bean '%s'", ex.getBeanName());
		if (ex.getBeanDefinition().getResourceDescription() != null) {
			printer.printf(", defined in %s,", ex.getBeanDefinition().getResourceDescription());
		}
		printer.print(" could not be registered. A bean with that name has already been defined ");
		if (ex.getExistingDefinition().getResourceDescription() != null) {
			printer.printf("in %s ", ex.getExistingDefinition().getResourceDescription());
		}
		printer.print("and overriding is disabled.");
		return description.toString();
	}

}
