/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that checks whether or not a metrics exporter is
 * enabled. If the {@code management.metrics.export.<name>.enabled} property is configured
 * then its value is used to determine if it matches. Otherwise, matches if the value of
 * the {@code management.metrics.export.defaults.enabled} property is {@code true} or if
 * it is not configured.
 *
 * @author Chris Bono
 * @since 2.4.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@Conditional(OnMetricsExportEnabledCondition.class)
public @interface ConditionalOnEnabledMetricsExport {

	/**
	 * The name of the metrics exporter.
	 * @return the name of the metrics exporter
	 */
	String value();

}
