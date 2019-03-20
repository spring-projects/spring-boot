/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.security.oauth2.client;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * Condition that checks for {@link EnableOAuth2Sso} on a
 * {@link WebSecurityConfigurerAdapter}.
 *
 * @author Dave Syer
 */
class EnableOAuth2SsoCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		String[] enablers = context.getBeanFactory()
				.getBeanNamesForAnnotation(EnableOAuth2Sso.class);
		ConditionMessage.Builder message = ConditionMessage
				.forCondition("@EnableOAuth2Sso Condition");
		for (String name : enablers) {
			if (context.getBeanFactory().isTypeMatch(name,
					WebSecurityConfigurerAdapter.class)) {
				return ConditionOutcome.match(message.found(
						"@EnableOAuth2Sso annotation on WebSecurityConfigurerAdapter")
						.items(name));
			}
		}
		return ConditionOutcome.noMatch(message.didNotFind(
				"@EnableOAuth2Sso annotation " + "on any WebSecurityConfigurerAdapter")
				.atAll());
	}

}
