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

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnJava.JavaVersion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnJava.Range;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link Condition} that checks for a required version of Java
 *
 * @author Oliver Gierke
 * @see ConditionalOnJava
 * @since 1.1.0
 */
class OnJavaCondition extends SpringBootCondition {

	private static final JavaVersion JVM_VERSION = JavaVersion.fromRuntime();
	private static final String MATCH_MESSAGE = "Required JVM version %s and found %s.";
	private static final String NO_MATCH_MESSAGE = "Required JVM version %s but found %s.";

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {

		Map<String, Object> attributes = metadata
				.getAnnotationAttributes(ConditionalOnJava.class.getName());

		JavaVersion version = (JavaVersion) attributes.get("value");
		Range range = (Range) attributes.get("range");

		ConditionOutcome match = ConditionOutcome.match(//
				String.format(MATCH_MESSAGE, range.getMessage(version), JVM_VERSION));
		ConditionOutcome noMatch = ConditionOutcome.noMatch(//
				String.format(NO_MATCH_MESSAGE, range.getMessage(version), JVM_VERSION));

		boolean equalOrBetter = JVM_VERSION.isEqualOrBetter(version);

		switch (range) {
		case OLDER_THAN:
			return equalOrBetter ? noMatch : match;
		case EQUAL_OR_NEWER:
		default:
			return equalOrBetter ? match : noMatch;
		}
	}
}
