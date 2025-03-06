/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.autoconfigure.http;

import java.util.Locale;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.http.ConditionalOnPreferredJsonMapper.JsonMapper;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link SpringBootCondition} for
 * {@link ConditionalOnPreferredJsonMapper @ConditionalOnPreferredJsonMapper}.
 *
 * @author Andy Wilkinson
 */
class OnPreferredJsonMapperCondition extends SpringBootCondition {

	private static final String PREFERRED_MAPPER_PROPERTY = "spring.http.converters.preferred-json-mapper";

	@Deprecated(since = "3.5.0", forRemoval = true)
	private static final String DEPRECATED_PREFERRED_MAPPER_PROPERTY = "spring.mvc.converters.preferred-json-mapper";

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		JsonMapper conditionMapper = metadata.getAnnotations()
			.get(ConditionalOnPreferredJsonMapper.class)
			.getEnum("value", JsonMapper.class);
		ConditionOutcome outcome = getMatchOutcome(context.getEnvironment(), PREFERRED_MAPPER_PROPERTY,
				conditionMapper);
		if (outcome != null) {
			return outcome;
		}
		outcome = getMatchOutcome(context.getEnvironment(), DEPRECATED_PREFERRED_MAPPER_PROPERTY, conditionMapper);
		if (outcome != null) {
			return outcome;
		}
		ConditionMessage message = ConditionMessage
			.forCondition(ConditionalOnPreferredJsonMapper.class, conditionMapper.name())
			.because("no property was configured and Jackson is the default");
		return (conditionMapper == JsonMapper.JACKSON) ? ConditionOutcome.match(message)
				: ConditionOutcome.noMatch(message);
	}

	private ConditionOutcome getMatchOutcome(Environment environment, String key, JsonMapper conditionMapper) {
		String property = environment.getProperty(key);
		if (property == null) {
			return null;
		}
		JsonMapper configuredMapper = JsonMapper.valueOf(property.toUpperCase(Locale.ROOT));
		ConditionMessage message = ConditionMessage
			.forCondition(ConditionalOnPreferredJsonMapper.class, configuredMapper.name())
			.because("property '%s' had the value '%s'".formatted(key, property));
		return (configuredMapper == conditionMapper) ? ConditionOutcome.match(message)
				: ConditionOutcome.noMatch(message);
	}

}
