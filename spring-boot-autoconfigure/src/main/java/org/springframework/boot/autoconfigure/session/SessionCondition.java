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

package org.springframework.boot.autoconfigure.session;

import java.util.Locale;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.AnnotationMetadata;

/**
 * General condition used with all session configuration classes.
 *
 * @author Tommy Ludwig
 * @author Stephane Nicoll
 */
class SessionCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		ConditionMessage.Builder message = ConditionMessage
				.forCondition("Session Condition");
		RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(
				context.getEnvironment(), "spring.session.");
		StoreType sessionStoreType = SessionStoreMappings
				.getType(((AnnotationMetadata) metadata).getClassName());
		if (!resolver.containsProperty("store-type")) {
			return ConditionOutcome.noMatch(
					message.didNotFind("spring.session.store-type property").atAll());
		}
		String value = resolver.getProperty("store-type").replace('-', '_')
				.toUpperCase(Locale.ENGLISH);
		if (value.equals(sessionStoreType.name())) {
			return ConditionOutcome.match(message
					.found("spring.session.store-type property").items(sessionStoreType));
		}
		return ConditionOutcome.noMatch(
				message.found("spring.session.store-type property").items(value));
	}

}
