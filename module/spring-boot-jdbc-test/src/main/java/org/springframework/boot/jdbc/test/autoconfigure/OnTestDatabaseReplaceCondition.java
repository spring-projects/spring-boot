/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.test.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link Condition} that checks the bound value of {@code spring.test.database.replace}.
 *
 * @author Hyunwoo Jung
 * @see ConditionalOnTestDatabaseReplace
 */
class OnTestDatabaseReplaceCondition extends SpringBootCondition {

	private static final String PROPERTY_NAME = "spring.test.database.replace";

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		Replace required = metadata.getAnnotations()
			.get(ConditionalOnTestDatabaseReplace.class)
			.getEnum(MergedAnnotation.VALUE, Replace.class);
		Replace replace = Binder.get(context.getEnvironment())
			.bind(PROPERTY_NAME, Replace.class)
			.orElse(Replace.NON_TEST);
		ConditionMessage.Builder message = ConditionMessage.forCondition(ConditionalOnTestDatabaseReplace.class,
				required);
		return new ConditionOutcome(replace == required, message.because(PROPERTY_NAME + " is " + replace));
	}

}
