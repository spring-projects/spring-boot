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
 * @author Phillip Webb
 * @see ConditionalOnJava
 * @since 1.1.0
 */
class OnJavaCondition extends SpringBootCondition {

	private static final JavaVersion JVM_VERSION = JavaVersion.getJavaVersion();

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		Map<String, Object> attributes = metadata
				.getAnnotationAttributes(ConditionalOnJava.class.getName());
		Range range = (Range) attributes.get("range");
		JavaVersion version = (JavaVersion) attributes.get("value");
		return getMatchOutcome(range, JVM_VERSION, version);
	}

	protected ConditionOutcome getMatchOutcome(Range range, JavaVersion runningVersion,
			JavaVersion version) {
		boolean match = runningVersion.isWithin(range, version);
		return new ConditionOutcome(match, getMessage(range, runningVersion, version));
	}

	private String getMessage(Range range, JavaVersion runningVersion, JavaVersion version) {
		String expected = String.format(range == Range.EQUAL_OR_NEWER ? "%s or newer"
				: "older than %s", version);
		return "Required JVM version " + expected + " found " + runningVersion;
	}
}
