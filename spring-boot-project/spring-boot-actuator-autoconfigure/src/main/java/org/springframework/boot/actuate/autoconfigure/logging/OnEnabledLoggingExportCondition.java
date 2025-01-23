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

package org.springframework.boot.actuate.autoconfigure.logging;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * {@link SpringBootCondition} to check whether logging exporter is enabled.
 *
 * @author Moritz Halbritter
 * @author Dmytro Nosan
 * @see ConditionalOnEnabledLoggingExport
 */
class OnEnabledLoggingExportCondition extends SpringBootCondition {

	private static final String GLOBAL_PROPERTY = "management.logging.export.enabled";

	private static final String EXPORTER_PROPERTY = "management.%s.logging.export.enabled";

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		String loggingExporter = getExporterName(metadata);
		if (StringUtils.hasLength(loggingExporter)) {
			String formattedExporterProperty = EXPORTER_PROPERTY.formatted(loggingExporter);
			Boolean exporterLoggingEnabled = context.getEnvironment()
				.getProperty(formattedExporterProperty, Boolean.class);
			if (exporterLoggingEnabled != null) {
				return new ConditionOutcome(exporterLoggingEnabled,
						ConditionMessage.forCondition(ConditionalOnEnabledLoggingExport.class)
							.because(formattedExporterProperty + " is " + exporterLoggingEnabled));
			}
		}
		Boolean globalLoggingEnabled = context.getEnvironment().getProperty(GLOBAL_PROPERTY, Boolean.class);
		if (globalLoggingEnabled != null) {
			return new ConditionOutcome(globalLoggingEnabled,
					ConditionMessage.forCondition(ConditionalOnEnabledLoggingExport.class)
						.because(GLOBAL_PROPERTY + " is " + globalLoggingEnabled));
		}
		return ConditionOutcome.match(ConditionMessage.forCondition(ConditionalOnEnabledLoggingExport.class)
			.because("is enabled by default"));
	}

	private static String getExporterName(AnnotatedTypeMetadata metadata) {
		Map<String, Object> attributes = metadata
			.getAnnotationAttributes(ConditionalOnEnabledLoggingExport.class.getName());
		if (attributes == null) {
			return null;
		}
		return (String) attributes.get("value");
	}

}
