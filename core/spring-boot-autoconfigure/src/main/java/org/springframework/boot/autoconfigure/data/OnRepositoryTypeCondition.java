/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.data;

import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;

/**
 * {@link SpringBootCondition} for controlling what type of Spring Data repositories are
 * auto-configured.
 *
 * @author Andy Wilkinson
 */
class OnRepositoryTypeCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		Map<String, @Nullable Object> attributes = metadata
			.getAnnotationAttributes(ConditionalOnRepositoryType.class.getName(), true);
		Assert.state(attributes != null, "'attributes' must not be null");
		String store = (String) attributes.get("store");
		Assert.state(store != null, "'store' must not be null");
		RepositoryType configuredType = getTypeProperty(context.getEnvironment(), store);
		RepositoryType requiredType = (RepositoryType) attributes.get("type");
		Assert.state(requiredType != null, "'requiredType' must not be null");
		ConditionMessage.Builder message = ConditionMessage.forCondition(ConditionalOnRepositoryType.class);
		if (configuredType == requiredType || configuredType == RepositoryType.AUTO) {
			return ConditionOutcome
				.match(message.because("configured type of '" + configuredType.name() + "' matched required type"));
		}
		return ConditionOutcome.noMatch(message.because("configured type (" + configuredType.name()
				+ ") did not match required type (" + requiredType.name() + ")"));
	}

	private RepositoryType getTypeProperty(Environment environment, String store) {
		return RepositoryType
			.valueOf(environment.getProperty(String.format("spring.data.%s.repositories.type", store), "auto")
				.toUpperCase(Locale.ENGLISH));
	}

}
