/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.condition;

import jakarta.servlet.ServletContext;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.web.context.WebApplicationContext;

/**
 * {@link Condition} that checks if the application is running as a traditional war
 * deployment.
 *
 * @author Madhura Bhave
 * @see ConditionalOnWarDeployment
 * @see ConditionalOnNotWarDeployment
 */
class OnWarDeploymentCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		boolean required = metadata.isAnnotated(ConditionalOnWarDeployment.class.getName());
		ResourceLoader resourceLoader = context.getResourceLoader();
		if (resourceLoader instanceof WebApplicationContext applicationContext) {
			ServletContext servletContext = applicationContext.getServletContext();
			if (servletContext != null) {
				return new ConditionOutcome(required, "Application is deployed as a WAR file.");
			}
		}
		return new ConditionOutcome(!required, ConditionMessage.forCondition(ConditionalOnWarDeployment.class)
			.because("the application is not deployed as a WAR file."));
	}

}
