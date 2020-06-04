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

package org.springframework.boot.actuate.autoconfigure.metrics.export.condition;

import java.lang.annotation.Annotation;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Metrics exporter enabled condition. All exporters can be disabled globally via the
 * {@code management.metrics.export.enabled} property or individually via the
 * {@code management.metrics.export.<name>.enabled} property (where {@code <name>} is the
 * name of the exporter.
 *
 * @author Chris Bono
 * @since 2.4.0
 */
public class OnMetricsExportEnabledCondition extends SpringBootCondition {

	private static final String PREFIX = "management.metrics.export";

	private static final Class<? extends Annotation> ANNOTATION_TYPE = ConditionalOnEnabledMetricsExport.class;

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		AnnotationAttributes annotationAttributes = AnnotationAttributes
				.fromMap(metadata.getAnnotationAttributes(this.ANNOTATION_TYPE.getName()));
		String exporterName = annotationAttributes.getString("value");
		ConditionOutcome outcome = getSpecificExporterOutcome(context, exporterName);
		if (outcome != null) {
			return outcome;
		}
		return getGlobalExporterOutcome(context);
	}

	protected ConditionOutcome getSpecificExporterOutcome(ConditionContext context, String exporterName) {
		Environment environment = context.getEnvironment();
		String enabledProperty = String.format("%s.%s.enabled", this.PREFIX, exporterName);
		if (!environment.containsProperty(enabledProperty)) {
			return null;
		}
		boolean match = environment.getProperty(enabledProperty, Boolean.class);
		return new ConditionOutcome(match, ConditionMessage.forCondition(this.ANNOTATION_TYPE)
				.because(String.format("%s is %b", enabledProperty, match)));
	}

	protected ConditionOutcome getGlobalExporterOutcome(ConditionContext context) {
		Environment environment = context.getEnvironment();
		String enabledProperty = String.format("%s.enabled", this.PREFIX);
		boolean match = environment.getProperty(enabledProperty, Boolean.class, true);
		return new ConditionOutcome(match, ConditionMessage.forCondition(this.ANNOTATION_TYPE)
				.because(String.format("%s is considered %b", enabledProperty, match)));
	}

}
