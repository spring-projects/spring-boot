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

package org.springframework.boot.opentelemetry.autoconfigure.logging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

/**
 * {@link Configuration @Configuration} for {@link OpenTelemetryLoggingConnectionDetails}.
 *
 * @author Toshiaki Maki
 */
@Configuration(proxyBeanMethods = false)
class OpenTelemetryLoggingConnectionDetailsConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty("management.opentelemetry.logging.export.endpoint")
	PropertiesOpenTelemetryLoggingConnectionDetails openTelemetryLoggingConnectionDetails(
			OpenTelemetryLoggingExportProperties properties) {
		return new PropertiesOpenTelemetryLoggingConnectionDetails(properties);
	}

	/**
	 * Adapts {@link OpenTelemetryLoggingExportProperties} to
	 * {@link OpenTelemetryLoggingConnectionDetails}.
	 */
	static class PropertiesOpenTelemetryLoggingConnectionDetails implements OpenTelemetryLoggingConnectionDetails {

		private final OpenTelemetryLoggingExportProperties properties;

		PropertiesOpenTelemetryLoggingConnectionDetails(OpenTelemetryLoggingExportProperties properties) {
			this.properties = properties;
		}

		@Override
		public String getUrl(Transport transport) {
			Assert.state(transport == this.properties.getTransport(),
					"Requested transport %s doesn't match configured transport %s".formatted(transport,
							this.properties.getTransport()));
			return this.properties.getEndpoint();
		}

	}

}
