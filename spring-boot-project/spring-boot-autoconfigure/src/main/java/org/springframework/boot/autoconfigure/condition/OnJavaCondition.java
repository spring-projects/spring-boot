/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.condition;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnJava.Range;
import org.springframework.boot.system.JavaVersion;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link Condition} that checks for a required version of Java.
 *
 * @author Oliver Gierke
 * @author Phillip Webb
 * @see ConditionalOnJava
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
class OnJavaCondition extends SpringBootCondition {

	private static final JavaVersion JVM_VERSION = JavaVersion.getJavaVersion();

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnJava.class.getName());
		Range range = (Range) attributes.get("range");
		JavaVersion version = (JavaVersion) attributes.get("value");
		return getMatchOutcome(range, JVM_VERSION, version);
	}

	protected ConditionOutcome getMatchOutcome(Range range, JavaVersion runningVersion, JavaVersion version) {
		boolean match = isWithin(runningVersion, range, version);
		String expected = String.format((range != Range.EQUAL_OR_NEWER) ? "(older than %s)" : "(%s or newer)", version);
		ConditionMessage message = ConditionMessage.forCondition(ConditionalOnJava.class, expected)
				.foundExactly(runningVersion);
		return new ConditionOutcome(match, message);
	}

	/**
	 * Determines if the {@code runningVersion} is within the specified range of versions.
	 * @param runningVersion the current version.
	 * @param range the range
	 * @param version the bounds of the range
	 * @return if this version is within the specified range
	 */
	private boolean isWithin(JavaVersion runningVersion, Range range, JavaVersion version) {
		if (range == Range.EQUAL_OR_NEWER) {
			return runningVersion.isEqualOrNewerThan(version);
		}
		if (range == Range.OLDER_THAN) {
			return runningVersion.isOlderThan(version);
		}
		throw new IllegalStateException("Unknown range " + range);
	}

}
