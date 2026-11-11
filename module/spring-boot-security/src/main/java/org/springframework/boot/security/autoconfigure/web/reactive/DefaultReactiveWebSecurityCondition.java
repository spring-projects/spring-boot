/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.autoconfigure.web.reactive;

import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Condition;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * {@link Condition} for
 * {@link ConditionalOnDefaultReactiveWebSecurity @ConditionalOnDefaultReactiveWebSecurity}.
 *
 * @author Andy Wilkinson
 */
class DefaultReactiveWebSecurityCondition extends AllNestedConditions {

	DefaultReactiveWebSecurityCondition() {
		super(ConfigurationPhase.REGISTER_BEAN);
	}

	@ConditionalOnClass({ Flux.class, SecurityWebFilterChain.class, ServerHttpSecurity.class })
	static class Classes {

	}

	@ConditionalOnMissingBean(SecurityWebFilterChain.class)
	static class Beans {

	}

}
