/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.autoconfigure.data;

import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;

/**
 * {@link SpringBootCondition} for controlling what type of Spring Data repositories are
 * auto-configured.
 *
 * @author Andy Wilkinson
 */
class OnRepositoryTypeCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		Map<String, @Nullable Object> attributes = metadata
			.getAnnotationAttributes(ConditionalOnRepositoryType.class.getName(), true);
		Assert.state(attributes != null, "'attributes' must not be null");
		String store = (String) attributes.get("store");
		Assert.state(store != null, "'store' must not be null");
		RepositoryType configuredType = getTypeProperty(context.getEnvironment(), store);
		RepositoryType requiredType = (RepositoryType) attributes.get("type");
		Assert.state(requiredType != null, "'requiredType' must not be null");
		ConditionMessage.Builder message = ConditionMessage.forCondition(ConditionalOnRepositoryType.class);
		if (configuredType == requiredType || configuredType == RepositoryType.AUTO) {
			return ConditionOutcome
				.match(message.because("configured type of '" + configuredType.name() + "' matched required type"));
		}
		return ConditionOutcome.noMatch(message.because("configured type (" + configuredType.name()
				+ ") did not match required type (" + requiredType.name() + ")"));
	}

	private RepositoryType getTypeProperty(Environment environment, String store) {
		return RepositoryType
			.valueOf(environment.getProperty(String.format("spring.data.%s.repositories.type", store), "auto")
				.toUpperCase(Locale.ENGLISH));
	}

}
