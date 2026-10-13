/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.hazelcast.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ResourceCondition;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;

/**
 * {@link SpringBootCondition} used to check if the Hazelcast configuration is available.
 * This either kicks in if a default configuration has been found or if configurable
 * property referring to the resource to use has been set.
 *
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @author Vedran Pavic
 * @since 4.0.0
 */
public abstract class HazelcastConfigResourceCondition extends ResourceCondition {

	protected static final String HAZELCAST_CONFIG_PROPERTY = "spring.hazelcast.config";

	private final String configSystemProperty;

	protected HazelcastConfigResourceCondition(String configSystemProperty, String... resourceLocations) {
		super("Hazelcast", HAZELCAST_CONFIG_PROPERTY, resourceLocations);
		Assert.notNull(configSystemProperty, "'configSystemProperty' must not be null");
		this.configSystemProperty = configSystemProperty;
	}

	@Override
	protected ConditionOutcome getResourceOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		if (System.getProperty(this.configSystemProperty) != null) {
			return ConditionOutcome
				.match(startConditionMessage().because("System property '" + this.configSystemProperty + "' is set."));
		}
		return super.getResourceOutcome(context, metadata);
	}

}
