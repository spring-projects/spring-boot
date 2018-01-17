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

package org.springframework.boot.autoconfigure.condition;

import java.util.Map;

import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link Condition} that checks for a required {@link CloudPlatform}.
 *
 * @author Madhura Bhave
 * @see ConditionalOnCloudPlatform
 */
class OnCloudPlatformCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		Map<String, Object> attributes = metadata
				.getAnnotationAttributes(ConditionalOnCloudPlatform.class.getName());
		CloudPlatform cloudPlatform = (CloudPlatform) attributes.get("value");
		return getMatchOutcome(context.getEnvironment(), cloudPlatform);
	}

	private ConditionOutcome getMatchOutcome(Environment environment,
			CloudPlatform cloudPlatform) {
		String name = cloudPlatform.name();
		ConditionMessage.Builder message = ConditionMessage
				.forCondition(ConditionalOnCloudPlatform.class);
		if (cloudPlatform.isActive(environment)) {
			return ConditionOutcome.match(message.foundExactly(name));
		}
		return ConditionOutcome.noMatch(message.didNotFind(name).atAll());
	}

}
