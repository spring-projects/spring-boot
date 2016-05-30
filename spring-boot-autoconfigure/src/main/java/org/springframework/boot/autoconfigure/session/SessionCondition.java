/*
 * Copyright 2012-2016 the original author or authors.
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

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

/**
 * General condition used with all session configuration classes.
 *
 * @author Tommy Ludwig
 */
class SessionCondition extends SpringBootCondition {

	private static final boolean redisPresent = ClassUtils.isPresent(
			"org.springframework.data.redis.core.RedisTemplate",
			SessionCondition.class.getClassLoader());

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(
				context.getEnvironment(), "spring.session.");
		StoreType sessionStoreType = SessionStoreMappings
				.getType(((AnnotationMetadata) metadata).getClassName());
		if (!resolver.containsProperty("store-type")) {
			if (sessionStoreType == StoreType.REDIS && redisPresent) {
				return ConditionOutcome
						.match("Session store type default to redis (deprecated)");
			}
			return ConditionOutcome.noMatch("Session store type not set");
		}
		String value = resolver.getProperty("store-type").replace("-", "_").toUpperCase();
		if (value.equals(sessionStoreType.name())) {
			return ConditionOutcome.match("Session store type " + sessionStoreType);
		}
		return ConditionOutcome.noMatch("Session store type " + value);
	}

}
