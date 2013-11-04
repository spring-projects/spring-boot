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

package org.springframework.boot.actuate.endpoint;

import org.springframework.boot.autoconfigure.report.EnableAutoConfigurationReport;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tests for {@link AutoConfigurationReportEndpoint}.
 *
 * @author Greg Turnquist
 */
public class AutoConfigurationReportEndpointTests extends AbstractEndpointTests<AutoConfigurationReportEndpoint> {

	public AutoConfigurationReportEndpointTests() {
		super(Config.class, AutoConfigurationReportEndpoint.class,
				"/autoconfigurationreport", true, "endpoints.autoconfigurationreport");
	}

	@Configuration
	@EnableConfigurationProperties
	@EnableAutoConfigurationReport
	public static class Config {

		@Bean
		public AutoConfigurationReportEndpoint endpoint() {
			return new AutoConfigurationReportEndpoint();
		}

	}

}
