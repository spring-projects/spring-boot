/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.docs.elasticsearch.jest;

import io.searchbox.client.config.HttpClientConfig;

import org.springframework.boot.autoconfigure.elasticsearch.jest.HttpClientConfigBuilderCustomizer;

/**
 * Example configuration for using a {@link HttpClientConfigBuilderCustomizer} to
 * configure additional HTTP settings.
 *
 * @author Stephane Nicoll
 */
public class JestClientCustomizationExample {

	/**
	 * A {@link HttpClientConfigBuilderCustomizer} that applies additional HTTP settings
	 * to the auto-configured jest client.
	 */
	// tag::customizer[]
	static class HttpSettingsCustomizer implements HttpClientConfigBuilderCustomizer {

		@Override
		public void customize(HttpClientConfig.Builder builder) {
			builder.maxTotalConnection(100).defaultMaxTotalConnectionPerRoute(5);
		}

	}
	// end::customizer[]

}
