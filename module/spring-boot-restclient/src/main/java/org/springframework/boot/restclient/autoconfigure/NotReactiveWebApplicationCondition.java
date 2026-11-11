/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;

/**
 * {@link SpringBootCondition} that applies only when running in a non-reactive web
 * application.
 *
 * @author Phillip Webb
 */
class NotReactiveWebApplicationCondition extends NoneNestedConditions {

	NotReactiveWebApplicationCondition() {
		super(ConfigurationPhase.PARSE_CONFIGURATION);
	}

	@ConditionalOnWebApplication(type = Type.REACTIVE)
	private static final class ReactiveWebApplication {

	}

}
