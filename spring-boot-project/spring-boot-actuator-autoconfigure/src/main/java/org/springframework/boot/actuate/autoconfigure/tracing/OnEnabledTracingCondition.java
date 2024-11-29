/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.tracing;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * {@link SpringBootCondition} to check whether tracing is enabled.
 *
 * @author Moritz Halbritter
 * @see ConditionalOnEnabledTracing
 */
class OnEnabledTracingCondition extends SpringBootCondition {

	private static final String GLOBAL_PROPERTY = "management.tracing.enabled";

	private static final String EXPORTER_PROPERTY = "management.%s.tracing.export.enabled";

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		String tracingExporter = getExporterName(metadata);
		if (StringUtils.hasLength(tracingExporter)) {
			Boolean exporterTracingEnabled = context.getEnvironment()
				.getProperty(EXPORTER_PROPERTY.formatted(tracingExporter), Boolean.class);
			if (exporterTracingEnabled != null) {
				return new ConditionOutcome(exporterTracingEnabled,
						ConditionMessage.forCondition(ConditionalOnEnabledTracing.class)
							.because(EXPORTER_PROPERTY.formatted(tracingExporter) + " is " + exporterTracingEnabled));
			}
		}
		Boolean globalTracingEnabled = context.getEnvironment().getProperty(GLOBAL_PROPERTY, Boolean.class);
		if (globalTracingEnabled != null) {
			return new ConditionOutcome(globalTracingEnabled,
					ConditionMessage.forCondition(ConditionalOnEnabledTracing.class)
						.because(GLOBAL_PROPERTY + " is " + globalTracingEnabled));
		}
		return ConditionOutcome.match(ConditionMessage.forCondition(ConditionalOnEnabledTracing.class)
			.because("tracing is enabled by default"));
	}

	private static String getExporterName(AnnotatedTypeMetadata metadata) {
		Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnEnabledTracing.class.getName());
		if (attributes == null) {
			return null;
		}
		return (String) attributes.get("value");
	}

}
