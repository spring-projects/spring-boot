/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.tracing.autoconfigure;

import java.util.Map;

import org.jspecify.annotations.Nullable;

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
 * @see ConditionalOnEnabledTracingExport
 */
class OnEnabledTracingExportCondition extends SpringBootCondition {

	private static final String GLOBAL_PROPERTY = "management.tracing.export.enabled";

	private static final String EXPORTER_PROPERTY = "management.tracing.export.%s.enabled";

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		String tracingExporter = getExporterName(metadata);
		if (StringUtils.hasLength(tracingExporter)) {
			Boolean exporterTracingEnabled = context.getEnvironment()
				.getProperty(EXPORTER_PROPERTY.formatted(tracingExporter), Boolean.class);
			if (exporterTracingEnabled != null) {
				return new ConditionOutcome(exporterTracingEnabled,
						ConditionMessage.forCondition(ConditionalOnEnabledTracingExport.class)
							.because(EXPORTER_PROPERTY.formatted(tracingExporter) + " is " + exporterTracingEnabled));
			}
		}
		Boolean globalTracingEnabled = context.getEnvironment().getProperty(GLOBAL_PROPERTY, Boolean.class);
		if (globalTracingEnabled != null) {
			return new ConditionOutcome(globalTracingEnabled,
					ConditionMessage.forCondition(ConditionalOnEnabledTracingExport.class)
						.because(GLOBAL_PROPERTY + " is " + globalTracingEnabled));
		}
		return ConditionOutcome.match(ConditionMessage.forCondition(ConditionalOnEnabledTracingExport.class)
			.because("tracing is enabled by default"));
	}

	private static @Nullable String getExporterName(AnnotatedTypeMetadata metadata) {
		Map<String, @Nullable Object> attributes = metadata
			.getAnnotationAttributes(ConditionalOnEnabledTracingExport.class.getName());
		if (attributes == null) {
			return null;
		}
		return (String) attributes.get("value");
	}

}
