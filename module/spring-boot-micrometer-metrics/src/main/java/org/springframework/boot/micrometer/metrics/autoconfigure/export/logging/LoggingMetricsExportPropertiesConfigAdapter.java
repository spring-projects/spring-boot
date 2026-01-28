/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.micrometer.metrics.autoconfigure.export.logging;

import io.micrometer.core.instrument.logging.LoggingRegistryConfig;

import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.StepRegistryPropertiesConfigAdapter;

/**
 * Adapter to convert {@link LoggingMetricsExportProperties} to a
 * {@link LoggingRegistryConfig}.
 *
 * @author Vasily Pelikh
 * @since 4.0.0
 */
public class LoggingMetricsExportPropertiesConfigAdapter
		extends StepRegistryPropertiesConfigAdapter<LoggingMetricsExportProperties> implements LoggingRegistryConfig {

	public LoggingMetricsExportPropertiesConfigAdapter(LoggingMetricsExportProperties properties) {
		super(properties);
	}

	@Override
	public String prefix() {
		return "management.logging.metrics.export";
	}

	@Override
	public boolean logInactive() {
		return obtain(LoggingMetricsExportProperties::isLogInactive, LoggingRegistryConfig.super::logInactive);
	}

}
