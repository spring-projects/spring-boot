/*
 * Copyright 2012-2014 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * {@link Condition} that checks if properties are defined in environment.
 *
 * @author Maciej Walkowiak
 * @author Phillip Webb
 * @see ConditionalOnProperty
 * @since 1.1.0
 */
class OnPropertyCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {

		String prefix = ((String) metadata.getAnnotationAttributes(
				ConditionalOnProperty.class.getName()).get("prefix")).trim();
		if (!"".equals(prefix) && !prefix.endsWith(".")) {
			prefix = prefix + ".";
		}
		String[] names = (String[]) metadata.getAnnotationAttributes(
				ConditionalOnProperty.class.getName()).get("value");
		Boolean relaxedNames = (Boolean) metadata.getAnnotationAttributes(
				ConditionalOnProperty.class.getName()).get("relaxedNames");

		List<String> missingProperties = new ArrayList<String>();

		PropertyResolver resolver = context.getEnvironment();
		if (relaxedNames) {
			resolver = new RelaxedPropertyResolver(resolver, prefix);
			prefix = "";
		}

		for (String name : names) {
			name = prefix + name;
			if (!resolver.containsProperty(name)
					|| "false".equalsIgnoreCase(resolver.getProperty(name))) {
				missingProperties.add(name);

			}
		}

		if (missingProperties.isEmpty()) {
			return ConditionOutcome.match();
		}

		return ConditionOutcome.noMatch("@ConditionalOnProperty "
				+ "missing required properties: "
				+ StringUtils.arrayToCommaDelimitedString(missingProperties.toArray())
				+ " not found");
	}
}
