/*
 * Copyright 2012-2013 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.report.AutoConfigurationReport;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Base of all {@link Condition} implementations used with Spring Boot. Provides sensible
 * logging to help the user diagnose what classes are loaded.
 * 
 * @author Phillip Webb
 * @author Greg Turnquist
 */
public abstract class SpringBootCondition implements Condition {

	private final Log logger = LogFactory.getLog(getClass());

	@Override
	public final boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		Outcome result = getMatchOutcome(context, metadata);
		StringBuilder message = getMessage(metadata, result);

		if (!result.isMatch()) {
			// Log non-matching conditions at debug
			if (this.logger.isDebugEnabled()) {
				this.logger.debug(message);
			}
			AutoConfigurationReport.registerDecision(context, message.toString(), getClassOrMethodName(metadata), result);
			return false;
		}

		// Log matching conditions at trace
		if (this.logger.isTraceEnabled()) {
			this.logger.trace(message);
		}
		AutoConfigurationReport.registerDecision(context, message.toString(), getClassOrMethodName(metadata), result);
		return true;
	}

	private StringBuilder getMessage(AnnotatedTypeMetadata metadata, Outcome result) {
		StringBuilder message = new StringBuilder();
		message.append("Condition ");
		message.append(ClassUtils.getShortName(getClass()));
		message.append(" on ");
		message.append(getClassOrMethodName(metadata));
		message.append(result.isMatch() ? " matched" : " did not match");
		if (StringUtils.hasLength(result.getMessage())) {
			message.append(" due to ");
			message.append(result.getMessage());
		}
		return message;
	}

	private String getClassOrMethodName(AnnotatedTypeMetadata metadata) {
		if (metadata instanceof ClassMetadata) {
			ClassMetadata classMetadata = (ClassMetadata) metadata;
			return classMetadata.getClassName();
		}
		else if (metadata instanceof MethodMetadata) {
			MethodMetadata methodMetadata = (MethodMetadata) metadata;
			return methodMetadata.getDeclaringClassName() + "#" + methodMetadata.getMethodName();
		}
		else {
			return "";
		}
	}

	/**
	 * Determine the outcome of the match along with suitable log output.
	 */
	public abstract Outcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata);

	protected final boolean anyMatches(ConditionContext context,
			AnnotatedTypeMetadata metadata, Condition... conditions) {
		for (Condition condition : conditions) {
			if (matches(context, metadata, condition)) {
				return true;
			}
		}
		return false;
	}

	protected final boolean matches(ConditionContext context,
			AnnotatedTypeMetadata metadata, Condition condition) {
		if (condition instanceof SpringBootCondition) {
			return ((SpringBootCondition) condition).getMatchOutcome(context, metadata)
					.isMatch();
		}
		return condition.matches(context, metadata);
	}

}
