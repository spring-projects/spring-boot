/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.client;

import java.util.List;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

/**
 * Configure {@link RestClient.Builder} with sensible defaults.
 *
 * @author Moritz Halbritter
 * @since 3.2.0
 */
public class RestClientBuilderConfigurer {

	private List<RestClientCustomizer> customizers;

	void setRestClientCustomizers(List<RestClientCustomizer> customizers) {
		this.customizers = customizers;
	}

	/**
	 * Configure the specified {@link RestClient.Builder}. The builder can be further
	 * tuned and default settings can be overridden.
	 * @param builder the {@link RestClient.Builder} instance to configure
	 * @return the configured builder
	 */
	public RestClient.Builder configure(RestClient.Builder builder) {
		applyCustomizers(builder);
		return builder;
	}

	private void applyCustomizers(Builder builder) {
		if (this.customizers != null) {
			for (RestClientCustomizer customizer : this.customizers) {
				customizer.customize(builder);
			}
		}
	}

}
