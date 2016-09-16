/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.web;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for web client.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 1.4.0
 */
@Configuration
@AutoConfigureAfter(HttpMessageConvertersAutoConfiguration.class)
public class WebClientAutoConfiguration {

	@Configuration
	@ConditionalOnClass(RestTemplate.class)
	public static class RestTemplateConfiguration {

		private final ObjectProvider<HttpMessageConverters> messageConverters;

		private final ObjectProvider<List<RestTemplateCustomizer>> restTemplateCustomizers;

		public RestTemplateConfiguration(
				ObjectProvider<HttpMessageConverters> messageConverters,
				ObjectProvider<List<RestTemplateCustomizer>> restTemplateCustomizers) {
			this.messageConverters = messageConverters;
			this.restTemplateCustomizers = restTemplateCustomizers;
		}

		@Bean
		@ConditionalOnMissingBean
		public RestTemplateBuilder restTemplateBuilder() {
			RestTemplateBuilder builder = new RestTemplateBuilder();
			HttpMessageConverters converters = this.messageConverters.getIfUnique();
			if (converters != null) {
				builder = builder.messageConverters(converters.getConverters());
			}
			List<RestTemplateCustomizer> customizers = this.restTemplateCustomizers
					.getIfAvailable();
			if (!CollectionUtils.isEmpty(customizers)) {
				customizers = new ArrayList<RestTemplateCustomizer>(customizers);
				AnnotationAwareOrderComparator.sort(customizers);
				builder = builder.customizers(customizers);
			}
			return builder;
		}

	}

}
