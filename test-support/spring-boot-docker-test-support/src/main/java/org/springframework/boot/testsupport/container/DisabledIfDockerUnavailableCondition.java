/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.container;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * An {@link ExecutionCondition} that disables execution if Docker is unavailable.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class DisabledIfDockerUnavailableCondition implements ExecutionCondition {

	private static final boolean TESTCONTAINERS_PRESENT = ClassUtils.isPresent("org.testcontainers.DockerClientFactory",
			DisabledIfDockerUnavailableCondition.class.getClassLoader());

	private static final String SILENCE_PROPERTY = "visibleassertions.silence";

	private static final ConditionEvaluationResult ENABLED = ConditionEvaluationResult.enabled("Docker available");

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		Assert.isTrue(TESTCONTAINERS_PRESENT, "Testcontainers is required when using @DisabledIfDockerUnavailable."
				+ " Add a dependency on org.testcontainers:testcontainers");
		String originalSilenceValue = System.getProperty(SILENCE_PROPERTY);
		try {
			DockerClientFactory.instance().client();
			return ENABLED;
		}
		catch (Throwable ex) {
			return ConditionEvaluationResult.disabled("Docker unavailable", ex.getMessage());
		}
		finally {
			if (originalSilenceValue != null) {
				System.setProperty(SILENCE_PROPERTY, originalSilenceValue);
			}
			else {
				System.clearProperty(SILENCE_PROPERTY);
			}
		}
	}

}
