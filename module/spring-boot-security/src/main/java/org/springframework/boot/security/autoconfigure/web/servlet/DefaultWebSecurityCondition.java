/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.autoconfigure.web.servlet;

import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Condition;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * {@link Condition} for
 * {@link ConditionalOnDefaultWebSecurity @ConditionalOnDefaultWebSecurity}.
 *
 * @author Phillip Webb
 */
class DefaultWebSecurityCondition extends AllNestedConditions {

	DefaultWebSecurityCondition() {
		super(ConfigurationPhase.REGISTER_BEAN);
	}

	@ConditionalOnClass({ SecurityFilterChain.class, HttpSecurity.class })
	static class Classes {

	}

	@ConditionalOnMissingBean({ SecurityFilterChain.class })
	static class Beans {

	}

}
