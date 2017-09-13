/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.session;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.AnnotationMetadata;

/**
 * General condition used with all session configuration classes.
 *
 * @author Tommy Ludwig
 * @author Stephane Nicoll
 * @author Madhura Bhave
 */
class SessionCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		ConditionMessage.Builder message = ConditionMessage
				.forCondition("Session Condition");
		Environment environment = context.getEnvironment();
		StoreType required = SessionStoreMappings
				.getType(((AnnotationMetadata) metadata).getClassName());
		if (!environment.containsProperty("spring.session.store-type")) {
			return ConditionOutcome.match(message.didNotFind("property", "properties")
					.items(ConditionMessage.Style.QUOTE, "spring.session.store-type"));
		}
		try {
			Binder binder = Binder.get(environment);
			return binder.bind("spring.session.store-type", StoreType.class)
					.map((t) -> new ConditionOutcome(t == required,
							message.found("spring.session.store-type property").items(t)))
					.orElse(ConditionOutcome.noMatch(message
							.didNotFind("spring.session.store-type property").atAll()));
		}
		catch (BindException ex) {
			return ConditionOutcome.noMatch(
					message.found("invalid spring.session.store-type property").atAll());
		}
	}

}
